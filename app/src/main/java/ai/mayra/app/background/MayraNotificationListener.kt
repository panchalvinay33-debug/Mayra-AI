package ai.mayra.app.background

import ai.mayra.app.context.AttentionAction
import ai.mayra.app.context.AttentionContext
import ai.mayra.app.context.ContextNotification
import ai.mayra.app.context.MayraContextHolder
import ai.mayra.app.safety.MayraGlobalStopStore
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

        val sourcePackage = sbn.packageName.orEmpty()
        val privacyPolicy = NotificationPrivacyStore(applicationContext).policyFor(sourcePackage)
        val extras = notification.extras
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (rawTitle.isBlank() && rawText.isBlank()) return

        val sensitivity = NotificationContentGuard.classify(
            title = rawTitle,
            text = rawText,
            secretVisibility = notification.visibility == Notification.VISIBILITY_SECRET
        )
        val stopped = MayraGlobalStopStore(applicationContext).snapshot().stopped
        val decision = MayraNotificationSafetyPolicy.decide(
            privacyMode = privacyPolicy.mode,
            allowReply = privacyPolicy.allowReply,
            sensitivity = sensitivity,
            rawConversationKey = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString(),
            globalStopActive = stopped,
            proactiveSuggestionsEnabled = preferences.proactiveSuggestionsEnabled
        )
        if (!decision.capture) return

        val (safeTitle, safeText) = NotificationContentGuard.sanitize(
            title = rawTitle,
            text = rawText,
            sensitivity = sensitivity,
            mode = privacyPolicy.mode
        )
        val notificationId = sbn.key ?: "$sourcePackage:${sbn.id}:${sbn.postTime}"
        val replyAvailable = decision.registerReply && MayraNotificationReplyRuntime.register(
            notificationId = notificationId,
            sourcePackage = sourcePackage,
            notification = notification
        )
        val appLabel = resolveAppLabel(sourcePackage)

        val record = MayraNotificationRecord(
            id = notificationId,
            sourcePackage = sourcePackage,
            appLabel = appLabel,
            title = safeTitle,
            text = safeText,
            postedAt = sbn.postTime,
            conversationKey = decision.safeConversationKey,
            groupKey = sbn.groupKey?.replace(Regex("[\\r\\n\\t]+"), " ")?.take(160),
            sensitivity = sensitivity,
            replyAvailable = replyAvailable,
            clearable = sbn.isClearable,
            ongoing = sbn.isOngoing
        )
        MayraNotificationIntelligenceRuntime.store.upsert(record)

        val event = AmbientEvent(
            sourcePackage = sourcePackage,
            title = safeTitle.take(MAX_TITLE_LENGTH),
            text = safeText.take(MAX_TEXT_LENGTH),
            timestamp = sbn.postTime
        )
        if (preferences.retainLocalHistory) AmbientEventStore(applicationContext).append(event)

        val normalized = ContextNotification(
            id = notificationId,
            sourcePackage = sourcePackage,
            appLabel = appLabel,
            title = event.title,
            text = event.text,
            postedAt = event.timestamp,
            conversationKey = record.conversationKey,
            categoryHint = notification.category,
            ongoing = sbn.isOngoing,
            silent = notification.priority <= Notification.PRIORITY_LOW,
            clearable = sbn.isClearable,
            sensitiveHint = sensitivity != NotificationSensitivity.NORMAL,
            groupKey = record.groupKey
        )
        val locked = (getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)
            ?.isDeviceLocked == true
        val insight = MayraContextHolder.runtime.analyzeNotification(
            normalized,
            AttentionContext(
                now = System.currentTimeMillis(),
                deviceLocked = locked,
                quietHours = false,
                userBusy = false
            )
        )
        if (!decision.allowProactiveTask) return

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

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val sourcePackage = sbn?.packageName.orEmpty()
        val notificationId = sbn?.key ?: return
        if (sourcePackage == packageName) return
        MayraNotificationIntelligenceRuntime.store.remove(notificationId)
        MayraNotificationReplyRuntime.remove(notificationId)
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
