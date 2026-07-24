package ai.mayra.app.platform.device

import ai.mayra.app.action.MayraActionResult
import ai.mayra.app.action.MayraActionRuntime
import ai.mayra.app.core.ActionExecutionResult
import ai.mayra.app.core.ActionExecutor
import ai.mayra.app.core.actions.DeviceActionRequest
import ai.mayra.app.core.actions.DeviceActionType
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.core.actions.PermissionSnapshot
import ai.mayra.app.owner.MayraOwnerActionPolicy
import ai.mayra.app.owner.StoredMayraOwnerActionPolicy
import android.content.Context

/**
 * Production command bridge backed by the single shared [MayraActionRuntime].
 *
 * Contact/app resolution stays outside the action engine because it can require provider reads and
 * disambiguation. Once a concrete target exists, every action goes through capability, permission,
 * confirmation, kill-switch, verification, fallback and audit layers.
 */
class MayraSafeActionExecutor(
    private val permissionSnapshot: () -> PermissionSnapshot,
    private val contactResolver: ContactResolver,
    private val installedAppResolver: InstalledAppResolver,
    private val engineProvider: () -> ai.mayra.app.action.MayraActionEngine,
    private val ownerPolicy: MayraOwnerActionPolicy = MayraOwnerActionPolicy { _, _ -> false },
    private val clock: () -> Long = System::currentTimeMillis
) : ActionExecutor {

    constructor(context: Context) : this(
        permissionSnapshot = {
            DevicePermissionSnapshotProvider(
                AndroidDevicePermissionStateReader(context.applicationContext)
            ).snapshot()
        },
        contactResolver = ContactResolver(AndroidContactPhoneDataSource(context.applicationContext)),
        installedAppResolver = InstalledAppResolver(AndroidInstalledAppDataSource(context.applicationContext)),
        engineProvider = { MayraActionRuntime.install(context.applicationContext) },
        ownerPolicy = StoredMayraOwnerActionPolicy(context.applicationContext)
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

        return when (val resolution = contactResolver.resolve(name)) {
            is ContactResolution.Resolved -> submit(
                request(DeviceActionType.CALL_CONTACT, resolution.contact.normalizedPhoneNumber),
                permissions
            )
            is ContactResolution.Ambiguous -> ActionExecutionResult.NotSupported(
                "I found multiple contacts: ${resolution.candidates.joinToString { it.displayName }}. Please say the full name."
            )
            ContactResolution.NotFound -> ActionExecutionResult.NotSupported(
                "I couldn't find a contact named ${name.trim()}."
            )
        }
    }

    override suspend fun sendMessage(recipient: String, message: String?): ActionExecutionResult {
        val permissions = permissionSnapshot()
        if (DevicePermission.READ_CONTACTS !in permissions.granted) {
            return submit(request(DeviceActionType.SEND_MESSAGE, recipient, message), permissions)
        }

        return when (val resolution = contactResolver.resolve(recipient)) {
            is ContactResolution.Resolved -> submit(
                request(
                    DeviceActionType.SEND_MESSAGE,
                    resolution.contact.normalizedPhoneNumber,
                    message
                ),
                permissions
            )
            is ContactResolution.Ambiguous -> ActionExecutionResult.NotSupported(
                "I found multiple contacts: ${resolution.candidates.joinToString { it.displayName }}. Please say the full name."
            )
            ContactResolution.NotFound -> ActionExecutionResult.NotSupported(
                "I couldn't find a contact named ${recipient.trim()}."
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
        return engineProvider().confirm(token).toActionResult()
    }

    override suspend fun rejectPending(): ActionExecutionResult {
        val token = pendingConfirmationToken
            ?: return ActionExecutionResult.NotSupported("There is no action waiting for confirmation.")
        pendingConfirmationToken = null
        return engineProvider().reject(token).toActionResult()
    }

    fun hasPendingConfirmation(): Boolean = pendingConfirmationToken != null

    private suspend fun submit(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot = permissionSnapshot()
    ): ActionExecutionResult {
        val engine = engineProvider()
        val first = engine.submit(request, permissions)
        val resolved = if (
            first is MayraActionResult.AwaitingConfirmation &&
            ownerPolicy.mayAutoConfirm(first.request, first.risk.level)
        ) {
            engine.confirm(first.ticket.token)
        } else {
            first
        }
        return resolved.toActionResult()
    }

    private fun MayraActionResult.toActionResult(): ActionExecutionResult = when (this) {
        is MayraActionResult.Completed -> ActionExecutionResult.Success
        is MayraActionResult.AwaitingPermission -> {
            val names = missing.joinToString { it.userFacingName() }
            ActionExecutionResult.PermissionRequired(
                message = if (permanentlyDenied.isNotEmpty()) {
                    "Please enable $names from Android Settings, then try again."
                } else {
                    "Mayra needs $names permission for this action. Open Device readiness and grant it, then try again."
                },
                permissions = missing
            )
        }
        is MayraActionResult.AwaitingConfirmation -> {
            pendingConfirmationToken = ticket.token
            val extra = when {
                risk.doubleConfirmationRequired -> " This action is sensitive and will need an additional protected confirmation in the final flow."
                risk.strongAuthenticationRecommended -> " Device authentication may be requested for sensitive actions."
                else -> ""
            }
            ActionExecutionResult.ConfirmationRequired(
                "$prompt Say yes to continue or no to cancel.$extra"
            )
        }
        is MayraActionResult.Blocked -> ActionExecutionResult.NotSupported(
            listOfNotNull(capability.reason, fallback.instruction).distinct().joinToString(" ")
        )
        is MayraActionResult.Rejected -> ActionExecutionResult.Failure(reason)
        is MayraActionResult.Failed -> ActionExecutionResult.Failure(
            "$message ${fallback.instruction}".trim()
        )
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String? = null
    ) = DeviceActionRequest(
        type = type,
        target = target.trim(),
        payload = payload?.trim()?.takeIf(String::isNotBlank),
        createdAt = clock()
    )

    private fun DevicePermission.userFacingName(): String = when (this) {
        DevicePermission.QUERY_APPS -> "installed apps"
        DevicePermission.CALL_PHONE -> "phone calls"
        DevicePermission.READ_CONTACTS -> "contacts"
        DevicePermission.SEND_MESSAGES -> "SMS"
        DevicePermission.POST_NOTIFICATIONS -> "notifications"
        DevicePermission.SCHEDULE_EXACT_ALARM -> "exact reminders"
    }
}
