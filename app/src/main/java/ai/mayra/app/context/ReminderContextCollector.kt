package ai.mayra.app.context

import android.content.Context
import ai.mayra.app.reminder.MayraReminderStore
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reduces Mayra-owned reminders to timing/state metadata immediately. Reminder title/detail text is
 * never copied into the J6 Context Fabric snapshot.
 */
fun collectReminderContext(
    context: Context,
    capturedAt: LocalDateTime = LocalDateTime.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): ReminderContextSnapshot = runCatching {
    val metadata = MayraReminderStore(context)
        .active()
        .map { reminder ->
            ReminderMetadata(
                dueAtEpochMillis = reminder.dueAt,
                state = reminder.state
            )
        }
    aggregateReminderMetadata(metadata, capturedAt, zoneId)
}.getOrElse {
    ReminderContextSnapshot(capturedAt, ContextValue.Unavailable)
}
