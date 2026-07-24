package ai.mayra.app.platform.device

import ai.mayra.app.action.MayraActionEngine
import ai.mayra.app.action.MayraActionResult
import ai.mayra.app.action.MayraActionRuntime
import ai.mayra.app.core.ActionExecutionResult
import ai.mayra.app.core.ActionExecutor
import ai.mayra.app.core.actions.AndroidDeviceActionRunner
import ai.mayra.app.core.actions.DeviceActionCoordinator
import ai.mayra.app.core.actions.DeviceActionExecutionResult
import ai.mayra.app.core.actions.DeviceActionRequest
import ai.mayra.app.core.actions.DeviceActionSafetyGate
import ai.mayra.app.core.actions.DeviceActionType
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.core.actions.PermissionSnapshot
import ai.mayra.app.identity.MayraContactIdentityEngine
import ai.mayra.app.identity.MayraContactIdentityStore
import ai.mayra.app.identity.MayraContactTrust
import ai.mayra.app.identity.MayraIdentityResolution
import android.content.Context

/**
 * Production bridge between Mayra's framework-neutral command layer and Android device actions.
 * Identity aliases resolve to an Android contact name before the existing contact resolver reads
 * the actual number. Ambiguous people are never guessed.
 */
class AndroidActionExecutor(
    private val permissionSnapshot: () -> PermissionSnapshot,
    private val contactResolver: ContactResolver,
    private val installedAppResolver: InstalledAppResolver,
    private val coordinator: DeviceActionCoordinator,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sharedEngine: MayraActionEngine? = null,
    private val identityEngine: MayraContactIdentityEngine? = null
) : ActionExecutor {

    constructor(context: Context) : this(
        permissionSnapshot = {
            DevicePermissionSnapshotProvider(
                AndroidDevicePermissionStateReader(context.applicationContext)
            ).snapshot()
        },
        contactResolver = ContactResolver(AndroidContactPhoneDataSource(context.applicationContext)),
        installedAppResolver = InstalledAppResolver(AndroidInstalledAppDataSource(context.applicationContext)),
        coordinator = DeviceActionCoordinator(
            safetyGate = DeviceActionSafetyGate(),
            runner = AndroidDeviceActionRunner(context.applicationContext)
        ),
        sharedEngine = MayraActionRuntime.install(context.applicationContext),
        identityEngine = MayraContactIdentityStore(context.applicationContext).engine()
    )

    private var pendingConfirmationToken: String? = null

    override suspend fun openApp(packageOrName: String): ActionExecutionResult {
        val clean = packageOrName.trim()
        if (clean.isBlank()) return ActionExecutionResult.Failure("App name cannot be blank")

        return when (val resolution = installedAppResolver.resolve(clean)) {
            is AppResolution.Resolved -> submit(
                DeviceActionRequest(
                    type = DeviceActionType.OPEN_APP,
                    target = resolution.app.label,
                    createdAt = clock(),
                    metadata = mapOf("packageName" to resolution.app.packageName)
                )
            )
            is AppResolution.Ambiguous -> ActionExecutionResult.NotSupported(
                "I found multiple apps: ${resolution.candidates.joinToString { it.label }}. Please use the full app name."
            )
            AppResolution.NotFound -> ActionExecutionResult.NotSupported(
                "I couldn't find an installed app named $clean."
            )
        }
    }

    override suspend fun callContact(name: String): ActionExecutionResult {
        val permissions = permissionSnapshot()
        if (DevicePermission.READ_CONTACTS !in permissions.granted) {
            return submit(request(DeviceActionType.CALL_CONTACT, name), permissions)
        }
        val identity = resolveIdentity(name) ?: return ambiguousIdentityResult(name)
        return when (val resolution = contactResolver.resolve(identity.contactName)) {
            is ContactResolution.Resolved -> submit(
                request(
                    DeviceActionType.CALL_CONTACT,
                    resolution.contact.normalizedPhoneNumber,
                    metadata = identity.metadata
                ),
                permissions
            )
            is ContactResolution.Ambiguous -> ActionExecutionResult.NotSupported(
                "I found multiple Android contacts for ${identity.contactName}: ${resolution.candidates.joinToString { it.displayName }}. Please use the exact saved name."
            )
            ContactResolution.NotFound -> ActionExecutionResult.NotSupported(
                "${identity.displayName} maps to ${identity.contactName}, but that Android contact was not found. Update People & Relationships or Contacts."
            )
        }
    }

    override suspend fun sendMessage(recipient: String, message: String?): ActionExecutionResult {
        val permissions = permissionSnapshot()
        if (DevicePermission.READ_CONTACTS !in permissions.granted) {
            return submit(request(DeviceActionType.SEND_MESSAGE, recipient, message), permissions)
        }
        val identity = resolveIdentity(recipient) ?: return ambiguousIdentityResult(recipient)
        return when (val resolution = contactResolver.resolve(identity.contactName)) {
            is ContactResolution.Resolved -> submit(
                request(
                    DeviceActionType.SEND_MESSAGE,
                    resolution.contact.normalizedPhoneNumber,
                    message,
                    identity.metadata
                ),
                permissions
            )
            is ContactResolution.Ambiguous -> ActionExecutionResult.NotSupported(
                "I found multiple Android contacts for ${identity.contactName}: ${resolution.candidates.joinToString { it.displayName }}. Please use the exact saved name."
            )
            ContactResolution.NotFound -> ActionExecutionResult.NotSupported(
                "${identity.displayName} maps to ${identity.contactName}, but that Android contact was not found. Update People & Relationships or Contacts."
            )
        }
    }

    override suspend fun createReminder(request: String): ActionExecutionResult = submit(
        request(DeviceActionType.CREATE_REMINDER, request)
    )

    override suspend fun confirmPending(): ActionExecutionResult {
        val token = pendingConfirmationToken
            ?: return ActionExecutionResult.NotSupported("There is no action waiting for confirmation.")
        pendingConfirmationToken = null
        return sharedEngine?.confirm(token)?.toActionResult() ?: coordinator.confirm(token).toActionResult()
    }

    override suspend fun rejectPending(): ActionExecutionResult {
        val token = pendingConfirmationToken
            ?: return ActionExecutionResult.NotSupported("There is no action waiting for confirmation.")
        pendingConfirmationToken = null
        return sharedEngine?.reject(token)?.toActionResult() ?: coordinator.reject(token).toActionResult()
    }

    fun hasPendingConfirmation(): Boolean = pendingConfirmationToken != null

    private fun resolveIdentity(query: String): IdentityTarget? {
        val engine = identityEngine ?: return IdentityTarget(query.trim(), query.trim(), emptyMap())
        return when (val result = engine.resolve(query)) {
            is MayraIdentityResolution.Resolved -> IdentityTarget(
                contactName = result.identity.canonicalContactName,
                displayName = result.identity.relationship ?: result.identity.canonicalContactName,
                metadata = buildMap {
                    put("identityId", result.identity.id)
                    put("relationship", result.identity.relationship.orEmpty())
                    put("identityTrust", result.identity.trust.name)
                    put("preferredChannel", result.identity.preferredChannel.name)
                    if (result.identity.trust == MayraContactTrust.TRUSTED) put("trustedContact", "true")
                    if (result.identity.trust == MayraContactTrust.SENSITIVE) put("sensitive", "true")
                }
            )
            is MayraIdentityResolution.Ambiguous -> null
            is MayraIdentityResolution.Unmapped -> IdentityTarget(query.trim(), query.trim(), emptyMap())
        }
    }

    private fun ambiguousIdentityResult(query: String): ActionExecutionResult.NotSupported {
        val candidates = (identityEngine?.resolve(query) as? MayraIdentityResolution.Ambiguous)
            ?.candidates
            .orEmpty()
            .joinToString { it.relationship ?: it.canonicalContactName }
        return ActionExecutionResult.NotSupported(
            if (candidates.isBlank()) "I could not safely resolve ${query.trim()}." else "I found multiple people for ${query.trim()}: $candidates. Please say the exact relationship or contact name."
        )
    }

    private suspend fun submit(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot = permissionSnapshot()
    ): ActionExecutionResult = sharedEngine?.submit(request, permissions)?.toActionResult()
        ?: coordinator.submit(request, permissions).toActionResult()

    private fun MayraActionResult.toActionResult(): ActionExecutionResult = when (this) {
        is MayraActionResult.Completed -> ActionExecutionResult.Success
        is MayraActionResult.AwaitingPermission -> permissionResult(missing, permanentlyDenied)
        is MayraActionResult.AwaitingConfirmation -> {
            pendingConfirmationToken = ticket.token
            val protection = when {
                risk.doubleConfirmationRequired -> " A protected second confirmation is required for this sensitive action."
                risk.strongAuthenticationRecommended -> " Device authentication may be requested for sensitive actions."
                else -> ""
            }
            ActionExecutionResult.ConfirmationRequired("$prompt Say yes to continue or no to cancel.$protection")
        }
        is MayraActionResult.Blocked -> ActionExecutionResult.NotSupported(
            listOfNotNull(capability.reason, fallback.instruction).distinct().joinToString(" ")
        )
        is MayraActionResult.Rejected -> ActionExecutionResult.Failure(reason)
        is MayraActionResult.Failed -> ActionExecutionResult.Failure("$message ${fallback.instruction}".trim())
    }

    private fun DeviceActionExecutionResult.toActionResult(): ActionExecutionResult = when (this) {
        is DeviceActionExecutionResult.Completed -> ActionExecutionResult.Success
        is DeviceActionExecutionResult.AwaitingPermission -> permissionResult(missing, permanentlyDenied)
        is DeviceActionExecutionResult.AwaitingConfirmation -> {
            pendingConfirmationToken = ticket.token
            ActionExecutionResult.ConfirmationRequired("$prompt Say yes to continue or no to cancel.")
        }
        is DeviceActionExecutionResult.Rejected -> ActionExecutionResult.Failure(reason)
        is DeviceActionExecutionResult.Failed -> ActionExecutionResult.Failure(message)
    }

    private fun permissionResult(
        missing: Set<DevicePermission>,
        permanentlyDenied: Set<DevicePermission>
    ): ActionExecutionResult.PermissionRequired {
        val names = missing.joinToString { it.userFacingName() }
        return ActionExecutionResult.PermissionRequired(
            message = if (permanentlyDenied.isNotEmpty()) {
                "Please enable $names from Android Settings, then try again."
            } else {
                "Mayra needs $names permission for this action. Open Device readiness and grant it, then try again."
            },
            permissions = missing
        )
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) = DeviceActionRequest(
        type = type,
        target = target.trim(),
        payload = payload?.trim()?.takeIf(String::isNotBlank),
        createdAt = clock(),
        metadata = metadata
    )

    private fun DevicePermission.userFacingName(): String = when (this) {
        DevicePermission.QUERY_APPS -> "installed apps"
        DevicePermission.CALL_PHONE -> "phone calls"
        DevicePermission.READ_CONTACTS -> "contacts"
        DevicePermission.SEND_MESSAGES -> "SMS"
        DevicePermission.POST_NOTIFICATIONS -> "notifications"
        DevicePermission.SCHEDULE_EXACT_ALARM -> "exact reminders"
    }

    private data class IdentityTarget(
        val contactName: String,
        val displayName: String,
        val metadata: Map<String, String>
    )
}
