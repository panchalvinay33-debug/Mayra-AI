package ai.mayra.app.context

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarContextTest {
    private val capturedAt = LocalDateTime.of(2026, 8, 6, 9, 15)

    @Test
    fun aggregatesOnlyCoarseCalendarMetadata() {
        val snapshot = aggregateCalendarMetadata(
            items = listOf(
                CalendarEventMetadata(
                    startsAt = LocalDateTime.of(2026, 8, 6, 9, 0),
                    endsAt = LocalDateTime.of(2026, 8, 6, 9, 30)
                ),
                CalendarEventMetadata(
                    startsAt = LocalDateTime.of(2026, 8, 6, 10, 0),
                    endsAt = LocalDateTime.of(2026, 8, 6, 10, 30)
                )
            ),
            capturedAt = capturedAt
        )

        val available = snapshot.access as ContextValue.Available
        assertEquals(2, available.value.remainingEventsToday)
        assertTrue(available.value.busyNow)
        assertEquals(45, available.value.minutesUntilNextEvent)
        assertEquals(ContextSource.CALENDAR_PROVIDER, available.source)
        assertEquals("Busy now · 2 remaining today", snapshot.summaryLine())
    }

    @Test
    fun emptyDayHasStableSummary() {
        val snapshot = aggregateCalendarMetadata(emptyList(), capturedAt)

        val available = snapshot.access as ContextValue.Available
        assertEquals(0, available.value.remainingEventsToday)
        assertFalse(available.value.busyNow)
        assertEquals(null, available.value.minutesUntilNextEvent)
        assertEquals("No more calendar events today", snapshot.summaryLine())
    }

    @Test
    fun upcomingEventReportsCoarseTimeOnly() {
        val snapshot = aggregateCalendarMetadata(
            items = listOf(
                CalendarEventMetadata(
                    startsAt = LocalDateTime.of(2026, 8, 6, 10, 0),
                    endsAt = LocalDateTime.of(2026, 8, 6, 10, 30)
                )
            ),
            capturedAt = capturedAt
        )

        assertEquals("1 remaining · next in 45 min", snapshot.summaryLine())
    }

    @Test
    fun accessStatesRemainExplicit() {
        assertEquals(
            "Calendar not enabled",
            CalendarContextSnapshot(capturedAt, ContextValue.NotGranted).summaryLine()
        )
        assertEquals(
            "Calendar context unavailable",
            CalendarContextSnapshot(capturedAt, ContextValue.Unavailable).summaryLine()
        )
    }

    @Test
    fun queryWindowCoversExactlyLocalDayInIndia() {
        val zone = ZoneId.of("Asia/Kolkata")
        val window = calendarQueryWindow(LocalDate.of(2026, 8, 6), zone)

        assertEquals(86_400_000L, window.last - window.first + 1)
        assertEquals(
            LocalDateTime.of(2026, 8, 6, 0, 0),
            epochMillisToLocalDateTime(window.first, zone)
        )
        assertEquals(
            LocalDateTime.of(2026, 8, 7, 0, 0),
            epochMillisToLocalDateTime(window.last + 1, zone)
        )
    }

    @Test
    fun metadataContractContainsNoPrivateCalendarFields() {
        val names = CalendarEventMetadata::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("title" in names)
        assertFalse("description" in names)
        assertFalse("location" in names)
        assertFalse("attendees" in names)
        assertFalse("organizer" in names)
        assertFalse("calendarName" in names)
        assertFalse("accountName" in names)
        assertFalse("meetingUrl" in names)
        assertFalse("notes" in names)
    }
}
