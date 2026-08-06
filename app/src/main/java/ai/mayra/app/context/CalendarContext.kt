package ai.mayra.app.context

import java.time.LocalDateTime

/** Privacy-first J6 calendar aggregate. Free-form calendar content is deliberately excluded. */
data class CalendarContextSnapshot(
    val capturedAt: LocalDateTime,
    val access: ContextValue<CalendarAggregate> = ContextValue.NotGranted
)

data class CalendarAggregate(
    val remainingEventsToday: Int,
    val busyNow: Boolean,
    val minutesUntilNextEvent: Int?
) {
    init {
        require(remainingEventsToday >= 0) { "remainingEventsToday must be non-negative" }
        require(minutesUntilNextEvent == null || minutesUntilNextEvent >= 0) {
            "minutesUntilNextEvent must be non-negative when present"
        }
        require(remainingEventsToday > 0 || minutesUntilNextEvent == null) {
            "minutesUntilNextEvent requires at least one remaining event"
        }
    }
}

/**
 * Safe metadata accepted by the deterministic calendar aggregator.
 *
 * Deliberately contains no title, description, location, attendee, organizer, meeting URL,
 * calendar/account name or notes.
 */
data class CalendarEventMetadata(
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime
) {
    init {
        require(!endsAt.isBefore(startsAt)) { "event end must not be before start" }
    }
}

fun aggregateCalendarMetadata(
    items: List<CalendarEventMetadata>,
    capturedAt: LocalDateTime
): CalendarContextSnapshot {
    val endOfDay = capturedAt.toLocalDate().plusDays(1).atStartOfDay()
    val relevant = items
        .filter { it.endsAt.isAfter(capturedAt) && it.startsAt.isBefore(endOfDay) }
        .sortedBy(CalendarEventMetadata::startsAt)

    val busyNow = relevant.any {
        !capturedAt.isBefore(it.startsAt) && capturedAt.isBefore(it.endsAt)
    }
    val next = relevant.firstOrNull { it.startsAt.isAfter(capturedAt) }
    val minutesUntilNext = next?.let {
        java.time.Duration.between(capturedAt, it.startsAt).toMinutes().toInt().coerceAtLeast(0)
    }

    return CalendarContextSnapshot(
        capturedAt = capturedAt,
        access = ContextValue.Available(
            value = CalendarAggregate(
                remainingEventsToday = relevant.size,
                busyNow = busyNow,
                minutesUntilNextEvent = minutesUntilNext
            ),
            source = ContextSource.CALENDAR_PROVIDER
        )
    )
}

fun CalendarContextSnapshot.summaryLine(): String = when (val value = access) {
    ContextValue.NotGranted -> "Calendar not enabled"
    ContextValue.Unavailable -> "Calendar context unavailable"
    is ContextValue.Available -> {
        val aggregate = value.value
        when {
            aggregate.remainingEventsToday == 0 -> "No more calendar events today"
            aggregate.busyNow -> "Busy now · ${aggregate.remainingEventsToday} remaining today"
            aggregate.minutesUntilNextEvent != null ->
                "${aggregate.remainingEventsToday} remaining · next in ${aggregate.minutesUntilNextEvent} min"
            else -> "${aggregate.remainingEventsToday} calendar events remaining today"
        }
    }
}
