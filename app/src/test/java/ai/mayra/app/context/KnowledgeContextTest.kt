package ai.mayra.app.context

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KnowledgeContextTest {
    private val now = LocalDateTime.of(2026, 8, 6, 18, 0)

    @Test
    fun summariesExposeOnlyCountsAndReadiness() {
        val snapshot = KnowledgeContextSnapshot(
            capturedAt = now,
            memory = ContextValue.Available(MemoryAggregate(3), ContextSource.MEMORY),
            documents = ContextValue.Available(
                DocumentAggregate(savedCount = 5, currentIndexedCount = 3, needsAttentionCount = 2),
                ContextSource.DOCUMENT_LIBRARY
            )
        )

        assertEquals(
            listOf("Memory · 3 saved", "Library · 3/5 current · 2 need attention"),
            snapshot.summaryLines()
        )
    }

    @Test
    fun aggregateContractsCannotCarryPrivateContent() {
        val memoryFields = MemoryAggregate::class.java.declaredFields
            .map { it.name.lowercase() }
            .filterNot { it.startsWith("$") }
        val documentFields = DocumentAggregate::class.java.declaredFields
            .map { it.name.lowercase() }
            .filterNot { it.startsWith("$") }

        assertEquals(listOf("savedcount"), memoryFields)
        assertEquals(
            setOf("savedcount", "currentindexedcount", "needsattentioncount"),
            documentFields.toSet()
        )

        val forbiddenRawFields = setOf(
            "key", "value", "title", "name", "text", "content", "snippet", "uri",
            "body", "detail", "path", "filename", "documentname", "memoryvalue"
        )
        assertFalse((memoryFields + documentFields).any { it in forbiddenRawFields })
    }

    @Test(expected = IllegalArgumentException::class)
    fun documentAggregateRejectsImpossibleCounts() {
        DocumentAggregate(savedCount = 2, currentIndexedCount = 2, needsAttentionCount = 1)
    }
}
