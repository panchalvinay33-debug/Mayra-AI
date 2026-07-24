package ai.mayra.app.owner

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ai.mayra.app.background.MayraNotificationListener

enum class OwnerAccessState { READY, ACTION_REQUIRED, DEVICE_UNSUPPORTED }

enum class OwnerCapability {
    MICROPHONE,
    CONTACTS,
    CAMERA,
    PHONE_CALLS,
    SMS,
    NOTIFICATIONS,
    NOTIFICATION_ACCESS,
    ACCESSIBILITY,
    BATTERY_BACKGROUND,
    DEFAULT_APPS
}

data class OwnerCapabilityStatus(
    val capability: OwnerCapability,
    val state: OwnerAccessState,
    val title: String,
    val detail: String,
    val settingsIntent: Intent? = null
)

data class MayraOwnerPreferences(
    val enabled: Boolean = true,
    val directLowRiskActions: Boolean = true,
    val directMediumRiskActions: Boolean = false,
    val proactivePresence: Boolean = true,
    val keepBackgroundRuntime: Boolean = true
)

class MayraOwnerModeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): MayraOwnerPreferences = MayraOwnerPreferences(
        enabled = preferences.getBoolean(KEY_ENABLED, true),
        directLowRiskActions = preferences.getBoolean(KEY_DIRECT_LOW, true),
        directMediumRiskActions = preferences.getBoolean(KEY_DIRECT_MEDIUM, false),
        proactivePresence = preferences.getBoolean(KEY_PROACTIVE, true),
        keepBackgroundRuntime = preferences.getBoolean(KEY_BACKGROUND, true)
    )

    fun save(value: MayraOwnerPreferences) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, value.enabled)
            .putBoolean(KEY_DIRECT_LOW, value.directLowRiskActions)
            .putBoolean(KEY_DIRECT_MEDIUM, value.directMediumRiskActions)
            .putBoolean(KEY_PROACTIVE, value.proactivePresence)
            .putBoolean(KEY_BACKGROUND, value.keepBackgroundRuntime)
            .apply()
    }

    private companion object {
        const val PREFS = "mayra_owner_mode"
        const val KEY_ENABLED = "enabled"
        const val KEY_DIRECT_LOW = "direct_low"
        const val KEY_DIRECT_MEDIUM = "direct_medium"
        const val KEY_PROACTIVE = "proactive"
        const val KEY_BACKGROUND = "background"
    }
}

class MayraOwnerCapabilityInspector(private val context: Context) {
    private val appContext = context.applicationContext

    fun snapshot(): List<OwnerCapabilityStatus> = listOf(
        runtimePermission(OwnerCapability.MICROPHONE, "Microphone", Manifest.permission.RECORD_AUDIO),
        runtimePermission(OwnerCapability.CONTACTS, "Contacts", Manifest.permission.READ_CONTACTS),
        runtimePermission(OwnerCapability.CAMERA, "Camera", Manifest.permission.CAMERA),
        telephonyPermission(OwnerCapability.PHONE_CALLS, "Phone calls", Manifest.permission.CALL_PHONE),
        telephonyPermission(OwnerCapability.SMS, "SMS", Manifest.permission.SEND_SMS),
        notificationPermission(),
        notificationAccess(),
        OwnerCapabilityStatus(
            OwnerCapability.ACCESSIBILITY,
            OwnerAccessState.ACTION_REQUIRED,
            "Assistive screen control",
            "Optional. Android requires you to enable an Accessibility service manually; Mayra must never hide or bypass this consent.",
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        ),
        OwnerCapabilityStatus(
            OwnerCapability.BATTERY_BACKGROUND,
            OwnerAccessState.ACTION_REQUIRED,
            "Background reliability",
            "Review battery optimization and allow Mayra to run reliably in the background where your phone permits it.",
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        ),
        OwnerCapabilityStatus(
            OwnerCapability.DEFAULT_APPS,
            OwnerAccessState.ACTION_REQUIRED,
            "Default assistant / phone roles",
            "Full call control and deeper assistant integration require Android default-app roles and complete role-specific implementations.",
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
    )

    fun readinessScore(statuses: List<OwnerCapabilityStatus> = snapshot()): Int {
        if (statuses.isEmpty()) return 0
        val ready = statuses.count { it.state == OwnerAccessState.READY }
        return (ready * 100) / statuses.size
    }

    private fun runtimePermission(capability: OwnerCapability, title: String, permission: String): OwnerCapabilityStatus {
        val granted = ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
        return OwnerCapabilityStatus(
            capability,
            if (granted) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            title,
            if (granted) "Ready" else "Grant when Mayra opens the relevant feature.",
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(android.net.Uri.parse("package:${appContext.packageName}"))
        )
    }

    private fun telephonyPermission(capability: OwnerCapability, title: String, permission: String): OwnerCapabilityStatus {
        if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            return OwnerCapabilityStatus(capability, OwnerAccessState.DEVICE_UNSUPPORTED, title, "This device has no telephony feature.")
        }
        return runtimePermission(capability, title, permission)
    }

    private fun notificationPermission(): OwnerCapabilityStatus {
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val enabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        val ready = runtimeGranted && enabled
        return OwnerCapabilityStatus(
            OwnerCapability.NOTIFICATIONS,
            if (ready) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            "Mayra notifications",
            if (ready) "Ready" else "Allow notification permission and enable Mayra notifications.",
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        )
    }

    private fun notificationAccess(): OwnerCapabilityStatus {
        val component = ComponentName(appContext, MayraNotificationListener::class.java)
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(appContext).contains(appContext.packageName)
        return OwnerCapabilityStatus(
            OwnerCapability.NOTIFICATION_ACCESS,
            if (enabled) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            "Notification intelligence",
            if (enabled) "Ready" else "Enable Notification Access so Mayra can summarize and safely reply to supported notifications.",
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component.flattenToString())
        )
    }
}

internal fun ownerModeSafetySummary(preferences: MayraOwnerPreferences): String = when {
    !preferences.enabled -> "Owner Mode is off. Mayra uses standard confirmation behavior."
    preferences.directMediumRiskActions -> "Owner Mode is active: low and medium risk actions may run directly; high and critical actions still require confirmation."
    preferences.directLowRiskActions -> "Owner Mode is active: low risk actions may run directly; medium, high and critical actions still require confirmation."
    else -> "Owner Mode is active with confirmations preserved for all actions."
}
