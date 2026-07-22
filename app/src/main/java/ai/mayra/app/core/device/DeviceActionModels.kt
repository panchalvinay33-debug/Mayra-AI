package ai.mayra.app.core.device

enum class DeviceCapability {
    APP_LAUNCH,
    OPEN_URL,
    SHARE_TEXT,
    CLIPBOARD_WRITE,
    SHOW_NOTIFICATION,
    OPEN_SETTINGS
}

enum class DeviceActionStatus {
    SUCCESS,
    UNSUPPORTED,
    INVALID_REQUEST,
    PERMISSION_DENIED,
    CONFIRMATION_REQUIRED,
    FAILED
}

sealed interface DeviceAction {
    val capability: DeviceCapability

    data class LaunchApp(val packageName: String) : DeviceAction {
        override val capability: DeviceCapability = DeviceCapability.APP_LAUNCH
    }

    data class OpenUrl(val url: String) : DeviceAction {
        override val capability: DeviceCapability = DeviceCapability.OPEN_URL
    }

    data class ShareText(val text: String, val title: String? = null) : DeviceAction {
        override val capability: DeviceCapability = DeviceCapability.SHARE_TEXT
    }

    data class CopyToClipboard(val text: String, val label: String = "Mayra") : DeviceAction {
        override val capability: DeviceCapability = DeviceCapability.CLIPBOARD_WRITE
    }

    data class ShowNotification(
        val title: String,
        val message: String,
        val channelId: String = "mayra_general"
    ) : DeviceAction {
        override val capability: DeviceCapability = DeviceCapability.SHOW_NOTIFICATION
    }

    data class OpenSettings(val section: String? = null) : DeviceAction {
        override val capability: DeviceCapability = DeviceCapability.OPEN_SETTINGS
    }
}

data class DeviceActionContext(
    val sessionId: String,
    val grantedPermissions: Set<String> = emptySet(),
    val confirmed: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
    }
}

data class DeviceActionResult(
    val status: DeviceActionStatus,
    val message: String? = null,
    val errorCode: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    val isSuccess: Boolean get() = status == DeviceActionStatus.SUCCESS
}

fun interface DeviceActionHandler {
    suspend fun execute(action: DeviceAction, context: DeviceActionContext): DeviceActionResult
}
