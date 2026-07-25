package ai.mayra.app.owner

import ai.mayra.app.action.MayraActionRisk
import ai.mayra.app.assistive.MayraAssistiveService
import ai.mayra.app.background.MayraNotificationListener
import ai.mayra.app.core.actions.DeviceActionRequest
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class OwnerAccessState { READY, ACTION_REQUIRED, DEVICE_UNSUPPORTED }

enum class OwnerCapability {
    MICROPHONE, CONTACTS, CAMERA, PHONE_CALLS, SMS, NOTIFICATIONS,
    NOTIFICATION_ACCESS, FLOATING_OVERLAY, ACCESSIBILITY, BATTERY_BACKGROUND, DEFAULT_APPS
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
    val directMediumRiskActions: Boolean = true,
    val trustedDirectHandoffs: Boolean = false,
    val proactivePresence: Boolean = true,
    val keepBackgroundRuntime: Boolean = true
)

class MayraOwnerModeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): MayraOwnerPreferences = MayraOwnerPreferences(
        enabled = preferences.getBoolean(KEY_ENABLED, true),
        directLowRiskActions = preferences.getBoolean(KEY_DIRECT_LOW, true),
        directMediumRiskActions = preferences.getBoolean(KEY_DIRECT_MEDIUM, true),
        trustedDirectHandoffs = preferences.getBoolean(KEY_TRUSTED_HANDOFFS, false),
        proactivePresence = preferences.getBoolean(KEY_PROACTIVE, true),
        keepBackgroundRuntime = preferences.getBoolean(KEY_BACKGROUND, true)
    )

    fun save(value: MayraOwnerPreferences) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, value.enabled)
            .putBoolean(KEY_DIRECT_LOW, value.directLowRiskActions)
            .putBoolean(KEY_DIRECT_MEDIUM, value.directMediumRiskActions)
            .putBoolean(KEY_TRUSTED_HANDOFFS, value.trustedDirectHandoffs)
            .putBoolean(KEY_PROACTIVE, value.proactivePresence)
            .putBoolean(KEY_BACKGROUND, value.keepBackgroundRuntime)
            .apply()
    }

    private companion object {
        const val PREFS = "mayra_owner_mode"
        const val KEY_ENABLED = "enabled"
        const val KEY_DIRECT_LOW = "direct_low"
        const val KEY_DIRECT_MEDIUM = "direct_medium"
        const val KEY_TRUSTED_HANDOFFS = "trusted_handoffs"
        const val KEY_PROACTIVE = "proactive"
        const val KEY_BACKGROUND = "background"
    }
}

fun interface MayraOwnerActionPolicy {
    fun mayAutoConfirm(request: DeviceActionRequest, risk: MayraActionRisk): Boolean
}

class StoredMayraOwnerActionPolicy(context: Context) : MayraOwnerActionPolicy {
    private val store = MayraOwnerModeStore(context.applicationContext)

    override fun mayAutoConfirm(request: DeviceActionRequest, risk: MayraActionRisk): Boolean {
        val settings = store.read()
        if (!settings.enabled) return false
        if (request.metadata["financial"] == "true" ||
            request.metadata["legalAcceptance"] == "true" ||
            request.metadata["destructive"] == "true" ||
            request.metadata["publicPost"] == "true" ||
            request.metadata["sensitive"] == "true") return false
        return when (risk) {
            MayraActionRisk.LOW -> settings.directLowRiskActions
            MayraActionRisk.MEDIUM -> settings.directMediumRiskActions
            MayraActionRisk.HIGH -> settings.trustedDirectHandoffs
            MayraActionRisk.CRITICAL -> false
        }
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
        floatingOverlayAccess(),
        accessibilityAccess(),
        OwnerCapabilityStatus(
            OwnerCapability.BATTERY_BACKGROUND,
            OwnerAccessState.ACTION_REQUIRED,
            "Background reliability",
            "Review battery optimization so reminders, notifications and Floating Mayra can remain dependable.",
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        ),
        OwnerCapabilityStatus(
            OwnerCapability.DEFAULT_APPS,
            OwnerAccessState.ACTION_REQUIRED,
            "Default assistant / phone roles",
            "Deeper assistant and call control require Android default-app roles and complete role-specific implementations.",
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        )
    )

    fun readinessScore(statuses: List<OwnerCapabilityStatus> = snapshot()): Int {
        if (statuses.isEmpty()) return 0
        val supported = statuses.filterNot { it.state == OwnerAccessState.DEVICE_UNSUPPORTED }
        if (supported.isEmpty()) return 100
        return (supported.count { it.state == OwnerAccessState.READY } * 100) / supported.size
    }

    private fun runtimePermission(capability: OwnerCapability, title: String, permission: String): OwnerCapabilityStatus {
        val granted = ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
        return OwnerCapabilityStatus(
            capability,
            if (granted) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            title,
            if (granted) "Ready" else "Grant this permission in the guided basic-access step.",
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${appContext.packageName}"))
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
        val ready = runtimeGranted && NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        return OwnerCapabilityStatus(
            OwnerCapability.NOTIFICATIONS,
            if (ready) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            "Mayra notifications",
            if (ready) "Ready" else "Allow notification permission and keep Mayra notifications enabled.",
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
            if (enabled) "Ready" else "Enable Notification Access for summaries and supported confirmed replies.",
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component.flattenToString())
        )
    }

    private fun floatingOverlayAccess(): OwnerCapabilityStatus {
        val enabled = Settings.canDrawOverlays(appContext)
        return OwnerCapabilityStatus(
            OwnerCapability.FLOATING_OVERLAY,
            if (enabled) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            "Floating Mayra",
            if (enabled) "Ready to appear as a draggable companion over other apps." else "Allow display over other apps to use the minimized Mayra companion.",
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${appContext.packageName}"))
        )
    }

    private fun accessibilityAccess(): OwnerCapabilityStatus {
        val expected = ComponentName(appContext, MayraAssistiveService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val enabled = enabledServices.split(':').any { it.equals(expected, ignoreCase = true) }
        return OwnerCapabilityStatus(
            OwnerCapability.ACCESSIBILITY,
            if (enabled) OwnerAccessState.READY else OwnerAccessState.ACTION_REQUIRED,
            "Assistive screen context",
            if (enabled) {
                "Ready for user-invoked, visible, non-sensitive screen assistance. No hidden clicks or password reading."
            } else {
                "Optional. Enable Mayra assistive context only when you want help with visible screen content."
            },
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        )
    }
}

internal fun ownerModeSafetySummary(preferences: MayraOwnerPreferences): String = when {
    !preferences.enabled -> "Owner Mode is off. Mayra uses standard confirmation behavior."
    preferences.trustedDirectHandoffs -> "Owner Mode is active with trusted direct call/message handoffs. Sensitive, destructive, financial, legal and critical actions remain protected."
    preferences.directMediumRiskActions -> "Owner Mode is active: low and medium risk actions may run directly; high and critical actions still require confirmation."
    preferences.directLowRiskActions -> "Owner Mode is active: low risk actions may run directly; medium, high and critical actions still require confirmation."
    else -> "Owner Mode is active with confirmations preserved for all actions."
}
