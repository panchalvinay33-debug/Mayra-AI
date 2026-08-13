package ai.mayra.app.context

import java.time.LocalDateTime

/** Privacy-first J6 notification aggregate. Raw titles, text, senders and message bodies are excluded. */
data class NotificationContextSnapshot(
    val capturedAt: LocalDateTime,
    val access: ContextValue<NotificationAggregate> = ContextValue.NotGranted
)

data class NotificationAggregate(
    val activeCount: Int,
    val attentionCount: Int,
    val categoryCounts: Map<NotificationCategory, Int>
) {
    init {
        require(activeCount >= 0) { "activeCount must be non-negative" }
        require(attentionCount in 0..activeCount) {
            "attentionCount must be between zero and activeCount"
        }
        require(categoryCounts.values.all { it >= 0 }) {
            "category counts must be non-negative"
        }
    }
}

enum class NotificationCategory {
    MESSAGE,
    CALL,
    REMINDER,
    EVENT,
    SYSTEM,
    OTHER
}

/**
 * Safe metadata accepted by the deterministic aggregator.
 *
 * Deliberately contains no title, body, sender, conversation, OTP or account text fields.
 */
data class NotificationMetadata(
    val category: NotificationCategory,
    val requestsAttention: Boolean
)

fun aggregateNotificationMetadata(
    items: List<NotificationMetadata>,
    capturedAt: LocalDateTime
): NotificationContextSnapshot {
    val counts = items
        .groupingBy(NotificationMetadata::category)
        .eachCount()
        .toSortedMap(compareBy(NotificationCategory::ordinal))

    return NotificationContextSnapshot(
        capturedAt = capturedAt,
        access = ContextValue.Available(
            value = NotificationAggregate(
                activeCount = items.size,
                attentionCount = items.count(NotificationMetadata::requestsAttention),
                categoryCounts = counts
            ),
            source = ContextSource.NOTIFICATION_ACCESS
        )
    )
}

fun NotificationContextSnapshot.summaryLine(): String = when (val value = access) {
    ContextValue.NotGranted -> "Notifications not enabled"
    ContextValue.Unavailable -> "Notification context unavailable"
    is ContextValue.Available -> {
        val aggregate = value.value
        when {
            aggregate.activeCount == 0 -> "No active notifications"
            aggregate.attentionCount > 0 ->
                "${aggregate.activeCount} active · ${aggregate.attentionCount} may need attention"
            else -> "${aggregate.activeCount} active notifications"
        }
    }
}
