package ai.mayra.app.context

import ai.mayra.app.reminder.ReminderState
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReminderContextTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val capturedAt = LocalDateTime.of(2026, 8, 6, 10, 0)
    private val nowMillis = capturedAt.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun aggregatesOnlyCoarseOwnedReminderMetadata() {
        val snapshot = aggregateReminderMetadata(
            items = listOf(
                ReminderMetadata(nowMillis - 60_000L, ReminderState.DUE),
                ReminderMetadata(nowMillis + 30 * 60_000L, ReminderState.SCHEDULED),
                ReminderMetadata(nowMillis + 60 * 60_000L, ReminderState.COMPLETED)
            ),
            capturedAt = capturedAt,
            zoneId = zone
        )

        val available = snapshot.access as ContextValue.Available
        assertEquals(2, available.value.activeCount)
        assertEquals(1, available.value.dueOrOverdueCount)
        assertEquals(30, available.value.minutesUntilNextReminder)
        assertEquals(ContextSource.REMINDERS, available.source)
        assertEquals("2 active · 1 due or overdue", snapshot.summaryLine())
    }

    @Test
    fun nextReminderSummaryUsesTimingOnly() {
        val snapshot = aggregateReminderMetadata(
            items = listOf(
                ReminderMetadata(nowMillis + 45 * 60_000L, ReminderState.SCHEDULED)
            ),
            capturedAt = capturedAt,
            zoneId = zone
        )

        assertEquals("1 active · next in 45 min", snapshot.summaryLine())
    }

    @Test
    fun noActiveRemindersHasStableSummary() {
        val snapshot = aggregateReminderMetadata(
            items = listOf(
                ReminderMetadata(nowMillis, ReminderState.COMPLETED),
                ReminderMetadata(nowMillis, ReminderState.CANCELLED)
            ),
            capturedAt = capturedAt,
            zoneId = zone
        )

        assertEquals("No active reminders", snapshot.summaryLine())
    }

    @Test
    fun metadataContractContainsNoReminderTextFields() {
        val names = ReminderMetadata::class.java.declaredFields.map { it.name }.toSet()

        assertFalse("title" in names)
        assertFalse("detail" in names)
        assertFalse("description" in names)
        assertFalse("note" in names)
        assertFalse("content" in names)
    }
}
