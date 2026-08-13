package ai.mayra.app.background

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ai.mayra.app.context.NotificationAggregate
import ai.mayra.app.context.NotificationContextStore

/** Receives notifications only after explicit Notification Access is enabled by the user. */
class MayraNotificationListener : NotificationListenerService() {
    private val intelligence by lazy { NotificationIntelligenceEngine() }
    private val contextStore by lazy { NotificationContextStore(applicationContext) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        publishAggregate()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        publishAggregate()

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
            title = title.take(MAX_FIELD_LENGTH),
            text = text.take(MAX_FIELD_LENGTH),
            timestamp = sbn.postTime
        )

        if (preferences.retainLocalHistory) {
            AmbientEventStore(applicationContext).append(event)
        }

        val insight = intelligence.analyze(event)
        if (!preferences.proactiveSuggestionsEnabled) return

        insight.suggestedAction?.let { action ->
            BackgroundTaskQueue(applicationContext).enqueue(
                BackgroundTask(
                    type = action,
                    payload = insight.summary,
                    createdAt = event.timestamp
                )
            )
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        publishAggregate()
    }

    private fun publishAggregate() {
        val active = runCatching {
            activeNotifications.orEmpty().filterNot { it.packageName == packageName }
        }.getOrDefault(emptyList())
        val attention = active.count { item ->
            val notification = item.notification
            notification.priority >= Notification.PRIORITY_HIGH || notification.fullScreenIntent != null
        }
        contextStore.write(
            NotificationAggregate(
                activeCount = active.size,
                attentionCount = attention,
                categoryCounts = emptyMap()
            )
        )
    }

    private companion object {
        const val MAX_FIELD_LENGTH = 500
    }
}
