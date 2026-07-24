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
import android.content.Context

/**
 * Production bridge between Mayra's framework-neutral command layer and Android device actions.
 *
 * Targets are resolved before execution, permission checks happen before sensitive provider reads,
 * and high-risk actions are held behind the one-time confirmation gate. The Android constructor
 * routes all execution through the shared [MayraActionRuntime], so the global kill switch, audit,
 * capability policy, verification and fallback layers govern real chat and voice commands.
 * Injected tests may continue using the legacy coordinator directly.
 */
class AndroidActionExecutor(
    private val permissionSnapshot: () -> PermissionSnapshot,
    private val contactResolver: ContactResolver,
    private val installedAppResolver: InstalledAppResolver,
    private val coordinator: DeviceActionCoordinator,
    private val clock: () -> Long = System::currentTimeMillis,
    private val sharedEngine: MayraActionEngine? = null
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
        sharedEngine = MayraActionRuntime.install(context.applicationContext)
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

    override suspend fun sendMessage(
        recipient: String,
        message: String?
    ): ActionExecutionResult {
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
        return sharedEngine
            ?.confirm(token)
            ?.toActionResult()
            ?: coordinator.confirm(token).toActionResult()
    }

    override suspend fun rejectPending(): ActionExecutionResult {
        val token = pendingConfirmationToken
            ?: return ActionExecutionResult.NotSupported("There is no action waiting for confirmation.")
        pendingConfirmationToken = null
        return sharedEngine
            ?.reject(token)
            ?.toActionResult()
            ?: coordinator.reject(token).toActionResult()
    }

    fun hasPendingConfirmation(): Boolean = pendingConfirmationToken != null

    private suspend fun submit(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot = permissionSnapshot()
    ): ActionExecutionResult = sharedEngine
        ?.submit(request, permissions)
        ?.toActionResult()
        ?: coordinator.submit(request, permissions).toActionResult()

    private fun MayraActionResult.toActionResult(): ActionExecutionResult = when (this) {
        is MayraActionResult.Completed -> ActionExecutionResult.Success
        is MayraActionResult.AwaitingPermission -> permissionResult(
            missing = missing,
            permanentlyDenied = permanentlyDenied
        )
        is MayraActionResult.AwaitingConfirmation -> {
            pendingConfirmationToken = ticket.token
            val protection = when {
                risk.doubleConfirmationRequired -> " A protected second confirmation is required for this sensitive action."
                risk.strongAuthenticationRecommended -> " Device authentication may be requested for sensitive actions."
                else -> ""
            }
            ActionExecutionResult.ConfirmationRequired(
                "$prompt Say yes to continue or no to cancel.$protection"
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

    private fun DeviceActionExecutionResult.toActionResult(): ActionExecutionResult = when (this) {
        is DeviceActionExecutionResult.Completed -> ActionExecutionResult.Success
        is DeviceActionExecutionResult.AwaitingPermission -> permissionResult(missing, permanentlyDenied)
        is DeviceActionExecutionResult.AwaitingConfirmation -> {
            pendingConfirmationToken = ticket.token
            ActionExecutionResult.ConfirmationRequired(
                "$prompt Say yes to continue or no to cancel."
            )
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
