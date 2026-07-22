package ai.mayra.app.core.device

class PlatformDeviceActionHandler(
    private val intents: DeviceIntentGateway,
    private val clipboard: DeviceClipboardGateway,
    private val notifications: DeviceNotificationGateway
) : DeviceActionHandler {

    override suspend fun execute(
        action: DeviceAction,
        context: DeviceActionContext
    ): DeviceActionResult = runCatching {
        when (action) {
            is DeviceAction.LaunchApp -> executeBoolean(
                supported = intents.canLaunchApp(action.packageName),
                success = { intents.launchApp(action.packageName) },
                unsupportedCode = "app_not_found",
                failureCode = "app_launch_failed"
            )

            is DeviceAction.OpenUrl -> executeBoolean(
                supported = intents.canOpenUrl(action.url),
                success = { intents.openUrl(action.url) },
                unsupportedCode = "url_handler_not_found",
                failureCode = "url_open_failed"
            )

            is DeviceAction.ShareText -> executeBoolean(
                supported = true,
                success = { intents.shareText(action.text, action.title) },
                failureCode = "share_failed"
            )

            is DeviceAction.CopyToClipboard -> executeBoolean(
                supported = true,
                success = { clipboard.copy(action.label, action.text) },
                failureCode = "clipboard_write_failed"
            )

            is DeviceAction.ShowNotification -> executeBoolean(
                supported = true,
                success = { notifications.show(action.channelId, action.title, action.message) },
                failureCode = "notification_failed"
            )

            is DeviceAction.OpenSettings -> executeBoolean(
                supported = true,
                success = { intents.openSettings(action.section) },
                failureCode = "settings_open_failed"
            )
        }
    }.getOrElse { error ->
        DeviceActionResult(
            status = DeviceActionStatus.FAILED,
            message = error.message,
            errorCode = "platform_action_exception",
            metadata = mapOf("exception" to error::class.java.simpleName)
        )
    }

    private inline fun executeBoolean(
        supported: Boolean,
        success: () -> Boolean,
        unsupportedCode: String = "unsupported",
        failureCode: String
    ): DeviceActionResult {
        if (!supported) {
            return DeviceActionResult(
                status = DeviceActionStatus.UNSUPPORTED,
                errorCode = unsupportedCode
            )
        }
        return if (success()) {
            DeviceActionResult(DeviceActionStatus.SUCCESS)
        } else {
            DeviceActionResult(DeviceActionStatus.FAILED, errorCode = failureCode)
        }
    }
}
