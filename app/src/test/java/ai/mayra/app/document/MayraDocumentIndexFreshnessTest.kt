package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Test

class MayraDocumentIndexFreshnessTest {
    private val pdf = MayraDocument(
        uri = "content://docs/report.pdf",
        name = "report.pdf",
        mimeType = "application/pdf",
        sizeBytes = 1_024,
        addedAt = 1L
    )

    @Test
    fun missingContentAlwaysNeedsIndexingForReadyParser() {
        assertEquals(
            DocumentIndexState.MISSING,
            MayraDocumentIndexFreshness.evaluate(pdf, hasIndexedContent = false, fingerprint = null)
        )
    }

    @Test
    fun oldContentWithoutFingerprintIsLegacy() {
        assertEquals(
            DocumentIndexState.LEGACY,
            MayraDocumentIndexFreshness.evaluate(pdf, hasIndexedContent = true, fingerprint = null)
        )
    }

    @Test
    fun matchingFingerprintIsCurrent() {
        val fingerprint = DocumentIndexFingerprint("pdf", 1, 1_024, 10L)

        assertEquals(
            DocumentIndexState.CURRENT,
            MayraDocumentIndexFreshness.evaluate(pdf, hasIndexedContent = true, fingerprint)
        )
    }

    @Test
    fun changedSourceSizeMakesIndexStale() {
        val fingerprint = DocumentIndexFingerprint("pdf", 1, 900, 10L)

        assertEquals(
            DocumentIndexState.STALE_SOURCE,
            MayraDocumentIndexFreshness.evaluate(pdf, hasIndexedContent = true, fingerprint)
        )
    }

    @Test
    fun changedParserVersionMakesIndexStale() {
        val fingerprint = DocumentIndexFingerprint("pdf", 99, 1_024, 10L)

        assertEquals(
            DocumentIndexState.STALE_PARSER,
            MayraDocumentIndexFreshness.evaluate(pdf, hasIndexedContent = true, fingerprint)
        )
    }

    @Test
    fun unknownSourceSizeDoesNotCreateFalseStaleResult() {
        val unknownSize = pdf.copy(sizeBytes = -1)
        val fingerprint = DocumentIndexFingerprint("pdf", 1, -1, 10L)

        assertEquals(
            DocumentIndexState.CURRENT,
            MayraDocumentIndexFreshness.evaluate(unknownSize, hasIndexedContent = true, fingerprint)
        )
    }

    @Test
    fun plannedLegacyDocIsUnsupportedEvenWhenOldContentExists() {
        val doc = pdf.copy(
            name = "old.doc",
            mimeType = "application/msword"
        )

        assertEquals(
            DocumentIndexState.UNSUPPORTED,
            MayraDocumentIndexFreshness.evaluate(
                doc,
                hasIndexedContent = true,
                fingerprint = DocumentIndexFingerprint("legacy-doc", 1, 1_024, 10L)
            )
        )
    }
}
