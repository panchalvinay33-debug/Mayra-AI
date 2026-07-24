package ai.mayra.app.runtime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

enum class RuntimeNotificationReadiness {
    READY,
    PERMISSION_REQUIRED,
    SYSTEM_BLOCKED
}

internal fun classifyNotificationReadiness(
    requiresRuntimePermission: Boolean,
    runtimePermissionGranted: Boolean,
    notificationsEnabled: Boolean
): RuntimeNotificationReadiness = when {
    requiresRuntimePermission && !runtimePermissionGranted -> RuntimeNotificationReadiness.PERMISSION_REQUIRED
    !notificationsEnabled -> RuntimeNotificationReadiness.SYSTEM_BLOCKED
    else -> RuntimeNotificationReadiness.READY
}

internal fun notificationReadiness(context: Context): RuntimeNotificationReadiness {
    val appContext = context.applicationContext
    val requiresPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val permissionGranted = !requiresPermission ||
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    return classifyNotificationReadiness(
        requiresRuntimePermission = requiresPermission,
        runtimePermissionGranted = permissionGranted,
        notificationsEnabled = NotificationManagerCompat.from(appContext).areNotificationsEnabled()
    )
}

internal fun notificationReadinessMessage(readiness: RuntimeNotificationReadiness): String = when (readiness) {
    RuntimeNotificationReadiness.READY -> "Notification permission is ready"
    RuntimeNotificationReadiness.PERMISSION_REQUIRED -> "Notification permission is required"
    RuntimeNotificationReadiness.SYSTEM_BLOCKED -> "Notifications are blocked in system settings"
}

internal fun notificationSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
