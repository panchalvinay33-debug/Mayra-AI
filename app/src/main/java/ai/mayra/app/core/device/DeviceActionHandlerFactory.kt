package ai.mayra.app.core.device

object DeviceActionHandlerFactory {
    fun create(
        intents: DeviceIntentGateway,
        clipboard: DeviceClipboardGateway,
        notifications: DeviceNotificationGateway
    ): DeviceActionHandler = PlatformDeviceActionHandler(
        intents = intents,
        clipboard = clipboard,
        notifications = notifications
    )
}
