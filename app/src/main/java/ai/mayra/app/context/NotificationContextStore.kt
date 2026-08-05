package ai.mayra.app.context

import android.content.Context
import java.time.LocalDateTime

/** Stores only aggregate notification metadata. No title, body, sender or conversation text. */
class NotificationContextStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun write(aggregate: NotificationAggregate, capturedAtEpochMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putInt(KEY_ACTIVE_COUNT, aggregate.activeCount)
            .putInt(KEY_ATTENTION_COUNT, aggregate.attentionCount)
            .putLong(KEY_CAPTURED_AT, capturedAtEpochMillis)
            .apply()
    }

    fun read(accessGranted: Boolean, now: LocalDateTime = LocalDateTime.now()): NotificationContextSnapshot {
        if (!accessGranted) return NotificationContextSnapshot(now, ContextValue.NotGranted)
        if (!preferences.contains(KEY_ACTIVE_COUNT)) {
            return NotificationContextSnapshot(now, ContextValue.Unavailable)
        }
        val active = preferences.getInt(KEY_ACTIVE_COUNT, 0).coerceAtLeast(0)
        val attention = preferences.getInt(KEY_ATTENTION_COUNT, 0).coerceIn(0, active)
        return NotificationContextSnapshot(
            capturedAt = now,
            access = ContextValue.Available(
                NotificationAggregate(active, attention, emptyMap()),
                ContextSource.NOTIFICATION_ACCESS
            )
        )
    }

    private companion object {
        const val PREFERENCES = "mayra_notification_context"
        const val KEY_ACTIVE_COUNT = "active_count"
        const val KEY_ATTENTION_COUNT = "attention_count"
        const val KEY_CAPTURED_AT = "captured_at"
    }
}
