package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationSummarizerTest {
    @Test
    fun `summary preserves chronological order and role labels`() {
        val messages = listOf(
            ConversationMessage(id = "u1", role = ConversationRole.USER, content = "Plan my day"),
            ConversationMessage(id = "a1", role = ConversationRole.ASSISTANT, content = "Start with exercise"),
            ConversationMessage(id = "u2", role = ConversationRole.USER, content = "Add study time")
        )

        val summary = ConversationSummarizer().summarize(messages)

        assertEquals(listOf("u1", "a1", "u2"), summary.sourceMessageIds)
        assertTrue(summary.text.contains("User: Plan my day"))
        assertTrue(summary.text.contains("Mayra: Start with exercise"))
        assertFalse(summary.truncated)
    }

    @Test
    fun `tool messages are excluded by default and point count is bounded`() {
        val messages = listOf(
            ConversationMessage(id = "tool", role = ConversationRole.TOOL, content = "internal result"),
            ConversationMessage(id = "u1", role = ConversationRole.USER, content = "one"),
            ConversationMessage(id = "a1", role = ConversationRole.ASSISTANT, content = "two"),
            ConversationMessage(id = "u2", role = ConversationRole.USER, content = "three")
        )

        val summary = ConversationSummarizer(
            ConversationSummaryPolicy(maxCharacters = 200, maxPoints = 2)
        ).summarize(messages)

        assertEquals(listOf("a1", "u2"), summary.sourceMessageIds)
        assertTrue(summary.truncated)
        assertFalse(summary.text.contains("internal result"))
    }
}