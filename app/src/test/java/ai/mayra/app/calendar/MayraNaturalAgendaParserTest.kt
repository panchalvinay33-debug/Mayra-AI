package ai.mayra.app.calendar

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraNaturalAgendaParserTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-07-24T06:00:00Z").toEpochMilli()

    @Test
    fun `tomorrow event is parsed with explicit time`() {
        val result = MayraNaturalAgendaParser(zone).parse("Add event tomorrow 4 PM team meeting", now)
        assertTrue(result is AgendaParseResult.Parsed)
        val event = (result as AgendaParseResult.Parsed).event
        assertTrue(event.title.contains("team", ignoreCase = true))
        assertEquals(16, Instant.ofEpochMilli(event.startsAt).atZone(zone).hour)
    }

    @Test
    fun `daily recurrence is retained`() {
        val result = MayraNaturalAgendaParser(zone).parse("Every day 8 AM medicine check", now) as AgendaParseResult.Parsed
        assertEquals(AgendaRecurrence.DAILY, result.event.recurrence)
    }

    @Test
    fun `missing time asks for clarification`() {
        val result = MayraNaturalAgendaParser(zone).parse("Add event tomorrow team meeting", now)
        assertTrue(result is AgendaParseResult.NeedsClarification)
    }

    @Test
    fun `past same-day time moves to next day`() {
        val result = MayraNaturalAgendaParser(zone).parse("Add event today 8 AM breakfast", now) as AgendaParseResult.Parsed
        assertTrue(result.event.startsAt > now)
    }
}