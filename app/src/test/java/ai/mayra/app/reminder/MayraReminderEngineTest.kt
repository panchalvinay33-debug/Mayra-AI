package ai.mayra.app.reminder

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraReminderEngineTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-07-29T06:30:00Z") // 12:00 PM IST
    private val parser = MayraReminderParser(Clock.fixed(now, zone), zone)

    @Test
    fun `hinglish after minutes parses into owned reminder`() {
        val result = parser.parse("drinking water after 3 min") as ReminderParseResult.Parsed

        assertEquals("drinking water", result.title)
        assertEquals(now.plusSeconds(180).toEpochMilli(), result.dueAt)
        assertEquals("drinking water after 3 min", result.detail)
    }

    @Test
    fun `english in minutes parses`() {
        val result = parser.parse("take medicine in 20 minutes") as ReminderParseResult.Parsed

        assertEquals("take medicine", result.title)
        assertEquals(now.plusSeconds(20 * 60).toEpochMilli(), result.dueAt)
    }

    @Test
    fun `dot clock time with ka parses as time only reminder`() {
        val result = parser.parse("5.20 ka") as ReminderParseResult.Parsed
        val expected = Instant.parse("2026-07-29T23:50:00Z").toEpochMilli()

        assertEquals("Reminder", result.title)
        assertEquals(expected, result.dueAt)
    }

    @Test
    fun `kal subah seven parses in local timezone`() {
        val result = parser.parse("kal subah 7 baje medicine lena") as ReminderParseResult.Parsed
        val expected = Instant.parse("2026-07-30T01:30:00Z").toEpochMilli()

        assertEquals(expected, result.dueAt)
        assertTrue(result.title.contains("medicine", ignoreCase = true))
    }

    @Test
    fun `missing time requests clarification`() {
        val result = parser.parse("kal medicine lena")

        assertTrue(result is ReminderParseResult.NeedsClarification)
        assertTrue((result as ReminderParseResult.NeedsClarification).message.contains("time", ignoreCase = true))
    }

    @Test
    fun `blank reminder is invalid`() {
        assertTrue(parser.parse("   ") is ReminderParseResult.Invalid)
    }

    @Test
    fun `due reminder can transition to missed for follow-up`() {
        assertTrue(ReminderLifecyclePolicy.canMarkMissed(ReminderState.DUE))
        assertTrue(ReminderLifecyclePolicy.canMarkMissed(ReminderState.SCHEDULED))
        assertTrue(ReminderLifecyclePolicy.canMarkMissed(ReminderState.SNOOZED))
        assertFalse(ReminderLifecyclePolicy.canMarkMissed(ReminderState.COMPLETED))
        assertFalse(ReminderLifecyclePolicy.canMarkMissed(ReminderState.CANCELLED))
    }
}
