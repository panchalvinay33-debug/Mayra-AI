package ai.mayra.app.platform.device

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import ai.mayra.app.core.device.DeviceIntentGateway

/** Android implementation of the platform intent boundary used by device actions. */
class AndroidIntentGateway(
    context: Context
) : DeviceIntentGateway {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager

    override fun canLaunchApp(packageName: String): Boolean =
        normalizedPackage(packageName)?.let(packageManager::getLaunchIntentForPackage) != null

    override fun launchApp(packageName: String): Boolean {
        val normalized = normalizedPackage(packageName) ?: return false
        val intent = packageManager.getLaunchIntentForPackage(normalized) ?: return false
        return start(intent)
    }

    override fun canOpenUrl(url: String): Boolean {
        val intent = urlIntent(url) ?: return false
        return intent.resolveActivity(packageManager) != null
    }

    override fun openUrl(url: String): Boolean = urlIntent(url)?.let(::start) ?: false

    override fun shareText(text: String, title: String?): Boolean {
        if (text.isBlank()) return false
        val sendIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        val chooser = Intent.createChooser(sendIntent, title?.takeIf(String::isNotBlank))
        return start(chooser)
    }

    override fun openSettings(section: String?): Boolean {
        val intent = when (section?.trim()?.lowercase()) {
            null, "", "app", "application" -> Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${appContext.packageName}")
            )
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "notifications", "notification" -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
            "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }
        return start(intent)
    }

    private fun urlIntent(rawUrl: String): Intent? {
        val uri = runCatching { Uri.parse(rawUrl.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme !in ALLOWED_SCHEMES) return null
        return Intent(Intent.ACTION_VIEW, uri)
    }

    private fun start(intent: Intent): Boolean = runCatching {
        appContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    private fun normalizedPackage(value: String): String? =
        value.trim().takeIf { it.matches(PACKAGE_PATTERN) }

    private companion object {
        val ALLOWED_SCHEMES = setOf("http", "https", "mailto", "tel", "geo")
        val PACKAGE_PATTERN = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
    }
}
