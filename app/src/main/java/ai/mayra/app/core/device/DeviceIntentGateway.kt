package ai.mayra.app.core.device

/** Platform boundary used by device actions without exposing Android framework types to core code. */
interface DeviceIntentGateway {
    fun canLaunchApp(packageName: String): Boolean
    fun launchApp(packageName: String): Boolean
    fun canOpenUrl(url: String): Boolean
    fun openUrl(url: String): Boolean
    fun shareText(text: String, title: String?): Boolean
    fun openSettings(section: String?): Boolean
}

interface DeviceClipboardGateway {
    fun copy(label: String, text: String): Boolean
}

interface DeviceNotificationGateway {
    fun show(channelId: String, title: String, message: String): Boolean
}
