package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentMaintenanceTest {
    @Test
    fun parserCatalogRecognizesSupportedTextByExtension() {
        val document = MayraDocument(
            uri = "content://notes/1",
            name = "meeting-notes.md",
            mimeType = "application/octet-stream",
            sizeBytes = 120,
            addedAt = 1L
        )

        val capability = MayraDocumentParserCatalog.capabilityFor(document)

        assertEquals("plain-text", capability?.id)
        assertEquals(ParserCapabilityState.READY, capability?.state)
    }

    @Test
    fun parserCatalogRecognizesReadyPdfParserAndSafetyLimits() {
        val document = MayraDocument(
            uri = "content://docs/2",
            name = "invoice.pdf",
            mimeType = "application/pdf",
            sizeBytes = 900,
            addedAt = 2L
        )

        val capability = MayraDocumentParserCatalog.capabilityFor(document)
        val status = MayraDocumentParserCatalog.statusText(document)

        assertEquals("pdf", capability?.id)
        assertEquals(ParserCapabilityState.READY, capability?.state)
        assertTrue(status.contains("100 pages"))
        assertTrue(status.contains("50 MB"))
        assertTrue(status.contains("OCR"))
    }

    @Test
    fun parserCatalogSeparatesReadyDocxFromLegacyDoc() {
        val docx = MayraDocument(
            uri = "content://docs/contract.docx",
            name = "contract.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            sizeBytes = 800,
            addedAt = 3L
        )
        val legacy = docx.copy(
            uri = "content://docs/contract.doc",
            name = "contract.doc",
            mimeType = "application/msword"
        )

        val docxCapability = MayraDocumentParserCatalog.capabilityFor(docx)
        val legacyCapability = MayraDocumentParserCatalog.capabilityFor(legacy)

        assertEquals("docx", docxCapability?.id)
        assertEquals(ParserCapabilityState.READY, docxCapability?.state)
        assertTrue(MayraDocumentParserCatalog.statusText(docx).contains("ZIP/XML"))
        assertEquals("legacy-doc", legacyCapability?.id)
        assertEquals(ParserCapabilityState.PLANNED, legacyCapability?.state)
        assertTrue(MayraDocumentParserCatalog.statusText(legacy).contains("DOCX"))
    }

    @Test
    fun unknownFormatReturnsNoRegisteredParser() {
        val document = MayraDocument(
            uri = "content://docs/3",
            name = "archive.xyz",
            mimeType = "application/x-unknown",
            sizeBytes = 10,
            addedAt = 3L
        )

        assertEquals(null, MayraDocumentParserCatalog.capabilityFor(document))
        assertTrue(MayraDocumentParserCatalog.statusText(document).contains("No parser"))
    }

    @Test
    fun healthyReportRequiresEveryDocumentToCompleteWithoutFailure() {
        val healthy = DocumentMaintenanceReport(
            totalDocuments = 3,
            indexed = 2,
            unsupported = 1,
            failed = 0,
            blank = 0,
            truncated = 1,
            removedOrphanedIndexes = 1,
            messages = emptyList()
        )
        val unhealthy = healthy.copy(failed = 1, indexed = 1)

        assertTrue(healthy.healthy)
        assertEquals(3, healthy.completed)
        assertFalse(unhealthy.healthy)
        assertTrue(healthy.userMessage().contains("safely limited 1"))
        assertTrue(healthy.userMessage().contains("removed stale indexes 1"))
    }
}
