package ai.mayra.app.background

import ai.mayra.app.context.AttentionAction
import ai.mayra.app.context.AttentionContext
import ai.mayra.app.context.ContextNotification
import ai.mayra.app.context.MayraContextHolder
import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Receives notifications only after explicit Notification Access is enabled by the user. */
class MayraNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return

        val preferences = AmbientPreferenceStore(applicationContext).read()
        if (!preferences.notificationIntelligenceEnabled) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        val event = AmbientEvent(
            sourcePackage = sbn.packageName.orEmpty(),
            title = title.take(MAX_TITLE_LENGTH),
            text = text.take(MAX_TEXT_LENGTH),
            timestamp = sbn.postTime
        )
        if (preferences.retainLocalHistory) AmbientEventStore(applicationContext).append(event)

        val normalized = ContextNotification(
            id = sbn.key ?: "${sbn.packageName}:${sbn.id}:${sbn.postTime}",
            sourcePackage = sbn.packageName.orEmpty(),
            appLabel = resolveAppLabel(sbn.packageName.orEmpty()),
            title = event.title,
            text = event.text,
            postedAt = event.timestamp,
            conversationKey = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.take(200),
            categoryHint = notification.category,
            ongoing = sbn.isOngoing,
            silent = notification.priority <= Notification.PRIORITY_LOW,
            clearable = sbn.isClearable,
            sensitiveHint = notification.visibility == Notification.VISIBILITY_SECRET,
            groupKey = sbn.groupKey?.take(300)
        )
        val locked = (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isDeviceLocked == true
        val insight = MayraContextHolder.runtime.analyzeNotification(
            normalized,
            AttentionContext(
                now = System.currentTimeMillis(),
                deviceLocked = locked,
                quietHours = false,
                userBusy = false
            )
        )
        if (!preferences.proactiveSuggestionsEnabled) return

        val taskType = when (insight.action) {
            AttentionAction.INTERRUPT -> "context.notification_interrupt"
            AttentionAction.ASK -> "context.notification_ask"
            AttentionAction.SUMMARIZE -> "context.notification_summary"
            AttentionAction.DEFER -> "context.notification_deferred"
            AttentionAction.IGNORE, AttentionAction.STORE_ONLY -> null
        } ?: return

        BackgroundTaskQueue(applicationContext).enqueue(
            BackgroundTask(
                type = taskType,
                payload = insight.summary.take(MAX_QUEUE_PAYLOAD),
                createdAt = event.timestamp
            )
        )
    }

    private fun resolveAppLabel(packageName: String): String = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val MAX_TITLE_LENGTH = 500
        const val MAX_TEXT_LENGTH = 1_000
        const val MAX_QUEUE_PAYLOAD = 500
    }
}
