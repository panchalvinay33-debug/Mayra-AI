package ai.mayra.app.core.device

import ai.mayra.app.core.intelligence.ToolManifest
import ai.mayra.app.core.intelligence.ToolParameter
import ai.mayra.app.core.intelligence.ToolRiskLevel

object DeviceToolCatalog {
    const val LAUNCH_APP = "device.launch_app"
    const val OPEN_URL = "device.open_url"
    const val SHARE_TEXT = "device.share_text"
    const val COPY_CLIPBOARD = "device.copy_clipboard"
    const val SHOW_NOTIFICATION = "device.show_notification"
    const val OPEN_SETTINGS = "device.open_settings"

    data class Definition(
        val manifest: ToolManifest,
        val capability: DeviceCapability,
        val actionFactory: (Map<String, String>) -> DeviceAction
    )

    fun definitions(): List<Definition> = listOf(
        Definition(
            manifest = ToolManifest(
                id = LAUNCH_APP,
                displayName = "Launch app",
                description = "Launch an installed Android application by package name.",
                parameters = listOf(required("packageName", "Android package name to launch.")),
                riskLevel = ToolRiskLevel.HIGH,
                tags = setOf("android", "device", "app", "launch")
            ),
            capability = DeviceCapability.APP_LAUNCH,
            actionFactory = { DeviceAction.LaunchApp(it.requiredValue("packageName")) }
        ),
        Definition(
            manifest = ToolManifest(
                id = OPEN_URL,
                displayName = "Open URL",
                description = "Open a web address using an Android application.",
                parameters = listOf(required("url", "HTTP or HTTPS address to open.")),
                riskLevel = ToolRiskLevel.MEDIUM,
                tags = setOf("android", "device", "browser", "url")
            ),
            capability = DeviceCapability.OPEN_URL,
            actionFactory = { DeviceAction.OpenUrl(it.requiredValue("url")) }
        ),
        Definition(
            manifest = ToolManifest(
                id = SHARE_TEXT,
                displayName = "Share text",
                description = "Open Android's share sheet with text content.",
                parameters = listOf(
                    required("text", "Text to share."),
                    optional("title", "Optional share-sheet title.")
                ),
                riskLevel = ToolRiskLevel.MEDIUM,
                tags = setOf("android", "device", "share", "text")
            ),
            capability = DeviceCapability.SHARE_TEXT,
            actionFactory = {
                DeviceAction.ShareText(
                    text = it.requiredValue("text"),
                    title = it.optionalValue("title")
                )
            }
        ),
        Definition(
            manifest = ToolManifest(
                id = COPY_CLIPBOARD,
                displayName = "Copy to clipboard",
                description = "Copy text into the Android clipboard.",
                parameters = listOf(
                    required("text", "Text to copy."),
                    optional("label", "Optional clipboard label.")
                ),
                riskLevel = ToolRiskLevel.LOW,
                tags = setOf("android", "device", "clipboard", "copy")
            ),
            capability = DeviceCapability.CLIPBOARD_WRITE,
            actionFactory = {
                DeviceAction.CopyToClipboard(
                    text = it.requiredValue("text"),
                    label = it.optionalValue("label") ?: "Mayra"
                )
            }
        ),
        Definition(
            manifest = ToolManifest(
                id = SHOW_NOTIFICATION,
                displayName = "Show notification",
                description = "Display a local Android notification.",
                parameters = listOf(
                    required("title", "Notification title."),
                    required("message", "Notification message."),
                    optional("channelId", "Android notification channel id.")
                ),
                requiredPermissions = setOf("android.permission.POST_NOTIFICATIONS"),
                riskLevel = ToolRiskLevel.HIGH,
                tags = setOf("android", "device", "notification")
            ),
            capability = DeviceCapability.SHOW_NOTIFICATION,
            actionFactory = {
                DeviceAction.ShowNotification(
                    title = it.requiredValue("title"),
                    message = it.requiredValue("message"),
                    channelId = it.optionalValue("channelId") ?: "mayra_general"
                )
            }
        ),
        Definition(
            manifest = ToolManifest(
                id = OPEN_SETTINGS,
                displayName = "Open settings",
                description = "Open Android settings, optionally at a named section.",
                parameters = listOf(optional("section", "Optional settings section.")),
                riskLevel = ToolRiskLevel.HIGH,
                tags = setOf("android", "device", "settings")
            ),
            capability = DeviceCapability.OPEN_SETTINGS,
            actionFactory = { DeviceAction.OpenSettings(it.optionalValue("section")) }
        )
    )

    private fun required(name: String, description: String) = ToolParameter(name, description)

    private fun optional(name: String, description: String) = ToolParameter(
        name = name,
        description = description,
        required = false
    )

    private fun Map<String, String>.requiredValue(name: String): String =
        this[name]?.trim()?.takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("Missing required argument: $name")

    private fun Map<String, String>.optionalValue(name: String): String? =
        this[name]?.trim()?.takeIf(String::isNotEmpty)
}
