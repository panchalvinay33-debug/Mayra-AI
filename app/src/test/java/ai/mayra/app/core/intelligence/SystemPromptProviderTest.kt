package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SystemPromptProviderTest {
    @Test
    fun `static provider normalizes and deduplicates`() {
        val provider = StaticSystemPromptProvider(listOf(" Be helpful ", "Be helpful", "Use Hindi"))
        assertEquals(listOf("Be helpful", "Use Hindi"), provider.instructions())
    }

    @Test
    fun `metadata provider emits instruction only when value exists`() {
        val provider = MetadataSystemPromptProvider("language", "Reply in")
        assertEquals(listOf("Reply in Hindi"), provider.instructions(mapOf("language" to " Hindi ")))
        assertTrue(provider.instructions(emptyMap()).isEmpty())
    }

    @Test
    fun `composite provider keeps stable distinct order`() {
        val provider = CompositeSystemPromptProvider(
            listOf(
                StaticSystemPromptProvider(listOf("Be concise", "Be safe")),
                StaticSystemPromptProvider(listOf("Be safe", "Use context"))
            )
        )
        assertEquals(listOf("Be concise", "Be safe", "Use context"), provider.instructions())
    }
}
