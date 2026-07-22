package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContextCompressorTest {
    @Test
    fun `small context is returned unchanged`() {
        val messages = listOf(
            ConversationMessage(id = "u1", role = ConversationRole.USER, content = "hello"),
            ConversationMessage(id = "a1", role = ConversationRole.ASSISTANT, content = "hi")
        )

        val result = ContextCompressor(ContextCompressionPolicy(maxCharacters = 100)).compress(messages)

        assertFalse(result.truncated)
        assertEquals(messages, result.messages)
        assertTrue(result.droppedMessageIds.isEmpty())
    }

    @Test
    fun `compression preserves system and newest messages`() {
        val messages = listOf(
            ConversationMessage(id = "s", role = ConversationRole.SYSTEM, content = "rules"),
            ConversationMessage(id = "old", role = ConversationRole.USER, content = "x".repeat(30)),
            ConversationMessage(id = "u", role = ConversationRole.USER, content = "latest question"),
            ConversationMessage(id = "a", role = ConversationRole.ASSISTANT, content = "latest answer")
        )

        val result = ContextCompressor(
            ContextCompressionPolicy(maxCharacters = 40, preserveNewestMessages = 2)
        ).compress(messages)

        assertTrue(result.truncated)
        assertEquals(listOf("s", "u", "a"), result.messages.map { it.id })
        assertEquals(listOf("old"), result.droppedMessageIds)
        assertTrue(result.compressedCharacters <= 40)
    }
}