package ai.mayra.app.core

import ai.mayra.app.reminder.MayraReminderParser
import ai.mayra.app.reminder.ReminderParseResult
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HinglishReminderGrammarTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val clock = Clock.fixed(Instant.parse("2026-07-24T11:00:00Z"), zone)

    @Test
    fun `suffix reminder dalo is recognized`() {
        val intent = AssistantIntentEngine().parse("2 min ka reminder dalo")

        val reminder = assertIs<AssistantIntent.CreateReminder>(intent)
        assertEquals("2 min", reminder.request)
    }

    @Test
    fun `suffix reminder laga do preserves task and time`() {
        val intent = AssistantIntentEngine().parse("2 minute baad pani peene ki reminder laga do")

        val reminder = assertIs<AssistantIntent.CreateReminder>(intent)
        assertTrue(reminder.request.contains("2 minute baad"))
        assertTrue(reminder.request.contains("pani peene"))
    }

    @Test
    fun `ke baad relative minutes parse correctly`() {
        val result = MayraReminderParser(clock, zone).parse("2 min ke baad pani peena")

        val parsed = assertIs<ReminderParseResult.Parsed>(result)
        assertEquals(Instant.parse("2026-07-24T11:02:00Z").toEpochMilli(), parsed.dueAt)
        assertTrue(parsed.title.contains("pani peena", ignoreCase = true))
    }

    @Test
    fun `prefix reminder command still works`() {
        val intent = AssistantIntentEngine().parse("reminder laga do 5 minute baad medicine")

        val reminder = assertIs<AssistantIntent.CreateReminder>(intent)
        assertTrue(reminder.request.contains("5 minute baad"))
        assertTrue(reminder.request.contains("medicine"))
    }
}
