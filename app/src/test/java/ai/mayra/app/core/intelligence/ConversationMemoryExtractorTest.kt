package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationMemoryExtractorTest {

    @Test
    fun `extracts durable user facts with inferred tags`() {
        val candidates = ConversationMemoryExtractor().extract(
            listOf(
                ConversationMessage(ConversationRole.USER, "My name is Vinay."),
                ConversationMessage(ConversationRole.ASSISTANT, "Nice to meet you."),
                ConversationMessage(ConversationRole.USER, "I prefer concise Hindi replies.")
            )
        )

        assertEquals(2, candidates.size)
        assertTrue(candidates.first().tags.contains("identity"))
        assertTrue(candidates.last().tags.contains("preference"))
    }

    @Test
    fun `ignores transient and assistant messages`() {
        val candidates = ConversationMemoryExtractor().extract(
            listOf(
                ConversationMessage(ConversationRole.USER, "Today I like tea."),
                ConversationMessage(ConversationRole.ASSISTANT, "Remember that the user likes coffee."),
                ConversationMessage(ConversationRole.USER, "What is the weather?")
            )
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `deduplicates normalized facts and respects limit`() {
        val extractor = ConversationMemoryExtractor(maxCandidates = 2)
        val candidates = extractor.extract(
            listOf(
                ConversationMessage(ConversationRole.USER, "My name is Vinay"),
                ConversationMessage(ConversationRole.USER, "My name is Vinay!"),
                ConversationMessage(ConversationRole.USER, "My goal is to build Mayra"),
                ConversationMessage(ConversationRole.USER, "I prefer accurate answers")
            )
        )

        assertEquals(2, candidates.size)
        assertEquals("My goal is to build Mayra", candidates.first().content)
    }
}
