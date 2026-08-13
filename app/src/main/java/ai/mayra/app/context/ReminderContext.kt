package ai.mayra.app.context

import ai.mayra.app.reminder.ReminderState
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Privacy-safe aggregate of Mayra-owned reminders. Reminder text never crosses this boundary. */
data class ReminderContextSnapshot(
    val capturedAt: LocalDateTime,
    val access: ContextValue<ReminderAggregate> = ContextValue.Unavailable
)

data class ReminderAggregate(
    val activeCount: Int,
    val dueOrOverdueCount: Int,
    val minutesUntilNextReminder: Int?
) {
    init {
        require(activeCount >= 0) { "activeCount must be non-negative" }
        require(dueOrOverdueCount in 0..activeCount) {
            "dueOrOverdueCount must be between zero and activeCount"
        }
        require(minutesUntilNextReminder == null || minutesUntilNextReminder >= 0) {
            "minutesUntilNextReminder must be non-negative when present"
        }
        require(activeCount > 0 || minutesUntilNextReminder == null) {
            "next reminder requires at least one active reminder"
        }
    }
}

/** Safe metadata accepted by the deterministic reminder aggregator. No title or detail fields. */
data class ReminderMetadata(
    val dueAtEpochMillis: Long,
    val state: ReminderState
) {
    init {
        require(dueAtEpochMillis >= 0L) { "dueAtEpochMillis must be non-negative" }
    }
}

fun aggregateReminderMetadata(
    items: List<ReminderMetadata>,
    capturedAt: LocalDateTime,
    zoneId: ZoneId = ZoneId.systemDefault()
): ReminderContextSnapshot {
    val nowMillis = capturedAt.atZone(zoneId).toInstant().toEpochMilli()
    val activeStates = setOf(
        ReminderState.SCHEDULED,
        ReminderState.SNOOZED,
        ReminderState.DUE,
        ReminderState.MISSED
    )
    val active = items.filter { it.state in activeStates }
    val dueOrOverdue = active.count {
        it.state == ReminderState.DUE || it.state == ReminderState.MISSED || it.dueAtEpochMillis <= nowMillis
    }
    val nextDue = active
        .asSequence()
        .filter { it.dueAtEpochMillis > nowMillis }
        .minOfOrNull(ReminderMetadata::dueAtEpochMillis)
    val minutesUntilNext = nextDue?.let {
        ((it - nowMillis) / 60_000L).coerceAtLeast(0L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    return ReminderContextSnapshot(
        capturedAt = capturedAt,
        access = ContextValue.Available(
            value = ReminderAggregate(
                activeCount = active.size,
                dueOrOverdueCount = dueOrOverdue,
                minutesUntilNextReminder = minutesUntilNext
            ),
            source = ContextSource.REMINDERS
        )
    )
}

internal fun reminderEpochToLocalDateTime(epochMillis: Long, zoneId: ZoneId): LocalDateTime =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()

fun ReminderContextSnapshot.summaryLine(): String = when (val value = access) {
    ContextValue.NotGranted -> "Reminders not enabled"
    ContextValue.Unavailable -> "Reminder context unavailable"
    is ContextValue.Available -> {
        val aggregate = value.value
        when {
            aggregate.activeCount == 0 -> "No active reminders"
            aggregate.dueOrOverdueCount > 0 ->
                "${aggregate.activeCount} active · ${aggregate.dueOrOverdueCount} due or overdue"
            aggregate.minutesUntilNextReminder != null ->
                "${aggregate.activeCount} active · next in ${aggregate.minutesUntilNextReminder} min"
            else -> "${aggregate.activeCount} active reminders"
        }
    }
}
