package ai.mayra.app.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FlexibleReminderIntentTest {
    private val parser = AssistantIntentEngine()

    @Test
    fun `set time reminder is recognized`() {
        val intent = assertIs<AssistantIntent.CreateReminder>(parser.parse("set 2 min reminder"))
        assertEquals("2 min", intent.request)
    }

    @Test
    fun `set reminder for time is recognized`() {
        val intent = assertIs<AssistantIntent.CreateReminder>(parser.parse("set reminder for 5 minutes"))
        assertEquals("5 minutes", intent.request)
    }

    @Test
    fun `hinglish suffix reminder is recognized`() {
        val intent = assertIs<AssistantIntent.CreateReminder>(parser.parse("2 minute baad pani peene ki reminder laga do"))
        assertEquals("2 minute baad pani peene", intent.request)
    }

    @Test
    fun `hinglish reminder set suffix is recognized`() {
        val intent = assertIs<AssistantIntent.CreateReminder>(parser.parse("2 min reminder set karo"))
        assertEquals("2 min", intent.request)
    }
}