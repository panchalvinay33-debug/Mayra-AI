package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentSearchEngineTest {
    private val invoice = MayraDocument(
        uri = "content://invoice",
        name = "April Invoice.txt",
        mimeType = "text/plain",
        sizeBytes = 1200,
        addedAt = 10L
    )
    private val contract = MayraDocument(
        uri = "content://contract",
        name = "Vendor Contract.txt",
        mimeType = "text/plain",
        sizeBytes = 2200,
        addedAt = 20L
    )

    @Test
    fun `title match ranks above content-only match`() {
        val hits = DocumentSearchEngine.search(
            documents = listOf(invoice, contract),
            indexedText = mapOf(
                invoice to "General notes without the requested phrase.",
                contract to "The April invoice must be paid within thirty days."
            ),
            query = "April invoice"
        )

        assertEquals(invoice, hits.first().document)
        assertTrue(hits.first().score > hits.last().score)
    }

    @Test
    fun `content match returns contextual snippet`() {
        val hits = DocumentSearchEngine.search(
            documents = listOf(contract),
            indexedText = mapOf(
                contract to "This agreement includes confidentiality obligations and payment terms of net thirty days."
            ),
            query = "payment terms"
        )

        assertEquals(1, hits.size)
        assertTrue(hits.single().matchedContent)
        assertTrue(hits.single().snippet.contains("payment terms"))
    }

    @Test
    fun `assistant filler words are ignored`() {
        val terms = DocumentSearchEngine.tokenizeQuery(
            "Mayra meri documents mein invoice search karo"
        )

        assertEquals(listOf("invoice", "karo"), terms)
        assertFalse("documents" in terms)
        assertFalse("mayra" in terms)
    }

    @Test
    fun `blank or filler-only query has no results`() {
        val hits = DocumentSearchEngine.search(
            documents = listOf(invoice),
            indexedText = mapOf(invoice to "invoice"),
            query = "Mayra documents mein search batao"
        )

        assertTrue(hits.isEmpty())
    }

    @Test
    fun `result limit is enforced`() {
        val documents = (1..30).map {
            invoice.copy(uri = "content://$it", name = "Invoice $it.txt", addedAt = it.toLong())
        }
        val hits = DocumentSearchEngine.search(
            documents = documents,
            indexedText = documents.associateWith { "invoice record" },
            query = "invoice",
            limit = 5
        )

        assertEquals(5, hits.size)
    }
}
