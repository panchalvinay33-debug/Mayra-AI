package ai.mayra.app.core.intelligence

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptBuilderTest {

    @Test
    fun `build preserves system memory context and current user ordering`() {
        val prompt = PromptBuilder().build(
            PromptRequest(
                sessionId = "s1",
                userInput = "What should I do today?",
                systemInstructions = listOf("Be helpful"),
                context = listOf(
                    ConversationMessage(role = ConversationRole.USER, content = "Earlier question"),
                    ConversationMessage(role = ConversationRole.ASSISTANT, content = "Earlier answer")
                ),
                memories = listOf(
                    MemoryRecord(id = "m1", content = "User likes concise plans", importance = 90)
                )
            )
        )

        assertEquals(ConversationRole.SYSTEM, prompt.messages.first().role)
        assertTrue(prompt.messages.any { it.metadata["memoryId"] == "m1" })
        assertEquals("What should I do today?", prompt.messages.last().content)
        assertEquals(listOf("m1"), prompt.includedMemoryIds)
    }

    @Test
    fun `higher importance memories are selected first`() {
        val prompt = PromptBuilder().build(
            PromptRequest(
                sessionId = "s1",
                userInput = "Plan",
                memories = listOf(
                    MemoryRecord(id = "low", content = "Low", importance = 10, createdAt = Instant.EPOCH),
                    MemoryRecord(id = "high", content = "High", importance = 90, createdAt = Instant.EPOCH)
                ),
                budget = PromptBudget(maxCharacters = 200, reservedResponseCharacters = 20, maxMemoryItems = 1)
            )
        )

        assertEquals(listOf("high"), prompt.includedMemoryIds)
    }

    @Test
    fun `old context is dropped before current user input`() {
        val prompt = PromptBuilder().build(
            PromptRequest(
                sessionId = "s1",
                userInput = "Newest request",
                context = List(10) { index ->
                    ConversationMessage(role = ConversationRole.USER, content = "old-$index-" + "x".repeat(30))
                },
                budget = PromptBudget(maxCharacters = 140, reservedResponseCharacters = 20)
            )
        )

        assertTrue(prompt.truncated)
        assertEquals("Newest request", prompt.messages.last().content)
        assertTrue(prompt.estimatedCharacters <= prompt.messages.sumOf { it.content.length + 8 })
    }
}