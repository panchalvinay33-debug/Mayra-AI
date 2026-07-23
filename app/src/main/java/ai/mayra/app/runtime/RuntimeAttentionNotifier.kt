package ai.mayra.app.runtime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

data class RuntimeAttentionAlert(
    val title: String,
    val message: String,
    val fingerprint: String
)

internal fun buildRuntimeAttentionAlert(
    runtimeFailures: Long,
    planFailures: Long,
    pendingActions: List<Pair<String, String>>,
    blockedPlans: Int,
    waitingConfirmationSteps: Int
): RuntimeAttentionAlert? {
    val failedCount = runtimeFailures + planFailures
    return when {
        failedCount > 0 -> RuntimeAttentionAlert(
            title = "Mayra runtime needs attention",
            message = "$failedCount runtime failure${if (failedCount == 1L) "" else "s"} detected.",
            fingerprint = "failure:$failedCount:$runtimeFailures:$planFailures"
        )
        pendingActions.isNotEmpty() -> RuntimeAttentionAlert(
            title = "Mayra is waiting for approval",
            message = if (pendingActions.size == 1) {
                pendingActions.first().second
            } else {
                "${pendingActions.size} actions are waiting for your approval."
            },
            fingerprint = "approval:${pendingActions.map { it.first }.sorted().joinToString(",")}"
        )
        blockedPlans > 0 || waitingConfirmationSteps > 0 -> RuntimeAttentionAlert(
            title = "A Mayra workflow is paused",
            message = "$blockedPlans blocked workflow${if (blockedPlans == 1) "" else "s"} · $waitingConfirmationSteps step${if (waitingConfirmationSteps == 1) "" else "s"} waiting.",
            fingerprint = "workflow:$blockedPlans:$waitingConfirmationSteps"
        )
        else -> null
    }
}

internal fun RuntimeControlSnapshot.toAttentionAlert(): RuntimeAttentionAlert? =
    buildRuntimeAttentionAlert(
        runtimeFailures = runtime.failedRequests,
        planFailures = plans.failedPlans.toLong(),
        pendingActions = pendingActions.map { it.id to it.title },
        blockedPlans = plans.blockedPlans,
        waitingConfirmationSteps = plans.waitingConfirmationSteps
    )

object RuntimeAttentionNotifier {
    private const val CHANNEL_ID = "mayra_runtime_attention"
    private const val NOTIFICATION_ID = 4102
    private const val PREFS = "runtime_attention_notifications"
    private const val LAST_FINGERPRINT = "last_fingerprint"

    fun scanAndNotify(context: Context, snapshot: RuntimeControlSnapshot): Boolean {
        val appContext = context.applicationContext
        val alert = snapshot.toAttentionAlert() ?: run {
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().remove(LAST_FINGERPRINT).apply()
            return false
        }
        if (!canPostNotifications(appContext)) return false

        val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.getString(LAST_FINGERPRINT, null) == alert.fingerprint) return false

        ensureChannel(appContext)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        preferences.edit().putString(LAST_FINGERPRINT, alert.fingerprint).apply()
        return true
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Runtime attention",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when Mayra needs approval or a workflow requires attention."
            }
        )
    }
}
