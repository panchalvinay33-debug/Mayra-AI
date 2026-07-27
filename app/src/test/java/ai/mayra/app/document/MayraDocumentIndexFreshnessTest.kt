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
    fun matchingSizeAndModifiedTimeIsCurrent() {
        val fingerprint = DocumentIndexFingerprint(
            parserId = "pdf",
            parserVersion = 1,
            sourceSizeBytes = 1_024,
            recordedAt = 10L,
            sourceLastModifiedAt = 500L
        )

        assertEquals(
            DocumentIndexState.CURRENT,
            MayraDocumentIndexFreshness.evaluate(
                document = pdf,
                hasIndexedContent = true,
                fingerprint = fingerprint,
                currentSource = DocumentSourceSnapshot(1_024, 500L)
            )
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
    fun changedModifiedTimeMakesSameSizeFileStale() {
        val fingerprint = DocumentIndexFingerprint(
            parserId = "pdf",
            parserVersion = 1,
            sourceSizeBytes = 1_024,
            recordedAt = 10L,
            sourceLastModifiedAt = 500L
        )

        assertEquals(
            DocumentIndexState.STALE_SOURCE,
            MayraDocumentIndexFreshness.evaluate(
                document = pdf,
                hasIndexedContent = true,
                fingerprint = fingerprint,
                currentSource = DocumentSourceSnapshot(1_024, 700L)
            )
        )
    }

    @Test
    fun oldFingerprintWithoutModifiedTimeIsLegacyWhenProviderNowReportsIt() {
        val oldFingerprint = DocumentIndexFingerprint("pdf", 1, 1_024, 10L)

        assertEquals(
            DocumentIndexState.LEGACY,
            MayraDocumentIndexFreshness.evaluate(
                document = pdf,
                hasIndexedContent = true,
                fingerprint = oldFingerprint,
                currentSource = DocumentSourceSnapshot(1_024, 700L)
            )
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
    fun unknownSourceMetadataDoesNotCreateFalseStaleResult() {
        val unknownSize = pdf.copy(sizeBytes = -1)
        val fingerprint = DocumentIndexFingerprint("pdf", 1, -1, 10L)

        assertEquals(
            DocumentIndexState.CURRENT,
            MayraDocumentIndexFreshness.evaluate(
                document = unknownSize,
                hasIndexedContent = true,
                fingerprint = fingerprint,
                currentSource = DocumentSourceSnapshot(-1, -1)
            )
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
