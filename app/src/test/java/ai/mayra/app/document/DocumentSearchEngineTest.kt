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
    fun `exact phrase ranks above separated terms`() {
        val exact = invoice.copy(uri = "content://exact", name = "Exact.txt")
        val separated = contract.copy(uri = "content://separated", name = "Separated.txt")
        val hits = DocumentSearchEngine.search(
            documents = listOf(separated, exact),
            indexedText = mapOf(
                exact to "The contract specifies payment terms of net thirty days.",
                separated to "Payment is due after approval. Other terms apply later."
            ),
            query = "payment terms"
        )

        assertEquals(exact, hits.first().document)
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
    fun `content snippet preserves original capitalization`() {
        val hits = DocumentSearchEngine.search(
            documents = listOf(contract),
            indexedText = mapOf(
                contract to "The Payment Terms require Rahul Traders to pay within 30 Days."
            ),
            query = "payment terms"
        )

        assertEquals(1, hits.size)
        assertTrue(hits.single().snippet.contains("Payment Terms"))
        assertTrue(hits.single().snippet.contains("Rahul Traders"))
        assertTrue(hits.single().snippet.contains("30 Days"))
    }

    @Test
    fun `content term matching does not match inside a larger word`() {
        val hits = DocumentSearchEngine.search(
            documents = listOf(contract),
            indexedText = mapOf(contract to "Internet access is included in the office agreement."),
            query = "net"
        )

        assertTrue(hits.isEmpty())
    }

    @Test
    fun `assistant filler words are ignored`() {
        val terms = DocumentSearchEngine.tokenizeQuery(
            "Mayra please meri documents mein invoice search karo"
        )

        assertEquals(listOf("invoice"), terms)
        assertFalse("documents" in terms)
        assertFalse("mayra" in terms)
        assertFalse("please" in terms)
        assertFalse("karo" in terms)
    }

    @Test
    fun `english command and possessive fillers are ignored`() {
        val terms = DocumentSearchEngine.tokenizeQuery(
            "Please find and show my files with payment terms"
        )

        assertEquals(listOf("payment", "terms"), terms)
        assertFalse("my" in terms)
        assertFalse("please" in terms)
        assertFalse("find" in terms)
        assertFalse("show" in terms)
    }

    @Test
    fun `blank or filler-only query has no results`() {
        val hits = DocumentSearchEngine.search(
            documents = listOf(invoice),
            indexedText = mapOf(invoice to "invoice"),
            query = "Mayra please documents mein search karo batao"
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
