package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraDocumentInventoryTest {
    @Test
    fun emptyLibraryProducesZeroSnapshot() {
        val inventory = MayraDocumentInventory.build(emptyList(), emptySet())

        assertEquals(0, inventory.totalDocuments)
        assertEquals(0, inventory.indexedDocuments)
        assertEquals(0, inventory.needsIndexing)
        assertTrue(inventory.formatCounts.isEmpty())
        assertFalse(inventory.fullyIndexed)
    }

    @Test
    fun classifiesReadyPlannedAndUnknownFormats() {
        val documents = listOf(
            document("content://docs/notes", "notes.txt", "text/plain"),
            document("content://docs/report", "report.pdf", "application/pdf"),
            document(
                "content://docs/letter",
                "letter.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ),
            document("content://docs/legacy", "legacy.doc", "application/msword"),
            document("content://docs/archive", "archive.xyz", "application/x-unknown")
        )

        val inventory = MayraDocumentInventory.build(
            documents = documents,
            indexedUris = setOf(documents[0].uri, documents[1].uri, documents[2].uri)
        )

        assertEquals(5, inventory.totalDocuments)
        assertEquals(3, inventory.indexedDocuments)
        assertEquals(2, inventory.needsIndexing)
        assertEquals(3, inventory.readyFormatDocuments)
        assertEquals(1, inventory.plannedFormatDocuments)
        assertEquals(1, inventory.unknownFormatDocuments)
        assertEquals(1, inventory.formatCounts["plain-text"])
        assertEquals(1, inventory.formatCounts["pdf"])
        assertEquals(1, inventory.formatCounts["docx"])
        assertEquals(1, inventory.formatCounts["legacy-doc"])
        assertEquals(1, inventory.formatCounts["unknown"])
        assertFalse(inventory.fullyIndexed)
    }

    @Test
    fun fullyIndexedRequiresAtLeastOneDocument() {
        val document = document("content://docs/one", "one.md", "text/markdown")
        val inventory = MayraDocumentInventory.build(listOf(document), setOf(document.uri))

        assertTrue(inventory.fullyIndexed)
        assertEquals(0, inventory.needsIndexing)
        assertTrue(inventory.userMessage().contains("1 saved document"))
        assertTrue(inventory.userMessage().contains("1 indexed"))
    }

    @Test
    fun indexedUrisOutsideCurrentLibraryAreIgnored() {
        val document = document("content://docs/current", "current.json", "application/json")
        val inventory = MayraDocumentInventory.build(
            documents = listOf(document),
            indexedUris = setOf("content://docs/orphan")
        )

        assertEquals(0, inventory.indexedDocuments)
        assertEquals(1, inventory.needsIndexing)
    }

    private fun document(uri: String, name: String, mime: String) = MayraDocument(
        uri = uri,
        name = name,
        mimeType = mime,
        sizeBytes = 100,
        addedAt = 1L
    )
}
