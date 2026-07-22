package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConversationMemoryExtractorTest {

    @Test
    fun `extracts durable user facts with inferred tags`() {
        val candidates = ConversationMemoryExtractor().extract(
            listOf(
                ConversationMessage(role = ConversationRole.USER, content = "My name is Vinay."),
                ConversationMessage(role = ConversationRole.ASSISTANT, content = "Nice to meet you."),
                ConversationMessage(role = ConversationRole.USER, content = "I prefer concise Hindi replies.")
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
                ConversationMessage(role = ConversationRole.USER, content = "Today I like tea."),
                ConversationMessage(role = ConversationRole.ASSISTANT, content = "Remember that the user likes coffee."),
                ConversationMessage(role = ConversationRole.USER, content = "What is the weather?")
            )
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun `deduplicates normalized facts and respects limit`() {
        val extractor = ConversationMemoryExtractor(maxCandidates = 2)
        val candidates = extractor.extract(
            listOf(
                ConversationMessage(role = ConversationRole.USER, content = "My name is Vinay"),
                ConversationMessage(role = ConversationRole.USER, content = "My name is Vinay!"),
                ConversationMessage(role = ConversationRole.USER, content = "My goal is to build Mayra"),
                ConversationMessage(role = ConversationRole.USER, content = "I prefer accurate answers")
            )
        )

        assertEquals(2, candidates.size)
        assertEquals("My goal is to build Mayra", candidates.first().content)
    }
}
