package ai.mayra.app.core.device

class DeviceActionDispatcher(
    private val capabilityRegistry: AndroidCapabilityRegistry,
    private val policy: DeviceActionPolicy,
    private val handler: DeviceActionHandler
) {
    suspend fun dispatch(
        action: DeviceAction,
        context: DeviceActionContext
    ): DeviceActionResult {
        val validationError = validate(action)
        if (validationError != null) {
            return DeviceActionResult(
                status = DeviceActionStatus.INVALID_REQUEST,
                message = validationError,
                errorCode = "invalid_device_action"
            )
        }

        if (!capabilityRegistry.supports(action.capability)) {
            return DeviceActionResult(
                status = DeviceActionStatus.UNSUPPORTED,
                message = "Capability ${action.capability} is not available.",
                errorCode = "unsupported_capability"
            )
        }

        val decision = policy.evaluate(action, context)
        if (!decision.allowed) {
            return DeviceActionResult(
                status = decision.status,
                message = decision.reason,
                errorCode = when (decision.status) {
                    DeviceActionStatus.PERMISSION_DENIED -> "permission_denied"
                    DeviceActionStatus.CONFIRMATION_REQUIRED -> "confirmation_required"
                    else -> "policy_denied"
                }
            )
        }

        return try {
            handler.execute(action, context)
        } catch (error: Throwable) {
            DeviceActionResult(
                status = DeviceActionStatus.FAILED,
                message = error.message ?: "Device action failed.",
                errorCode = "device_action_exception",
                metadata = mapOf("exception" to error::class.java.simpleName)
            )
        }
    }

    private fun validate(action: DeviceAction): String? = when (action) {
        is DeviceAction.LaunchApp -> if (action.packageName.isBlank()) "Package name cannot be blank." else null
        is DeviceAction.OpenUrl -> if (!isSafeUrl(action.url)) "URL must use http or https." else null
        is DeviceAction.ShareText -> if (action.text.isBlank()) "Share text cannot be blank." else null
        is DeviceAction.CopyToClipboard -> if (action.text.isBlank()) "Clipboard text cannot be blank." else null
        is DeviceAction.ShowNotification -> when {
            action.title.isBlank() -> "Notification title cannot be blank."
            action.message.isBlank() -> "Notification message cannot be blank."
            action.channelId.isBlank() -> "Notification channel cannot be blank."
            else -> null
        }
        is DeviceAction.OpenSettings -> null
    }

    private fun isSafeUrl(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.startsWith("https://") || normalized.startsWith("http://")
    }
}
