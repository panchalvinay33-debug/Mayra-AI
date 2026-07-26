package ai.mayra.app.document

import ai.mayra.app.TestMayraApplication
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestMayraApplication::class)
class MayraPdfExtractionSafetyTest {
    private val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()

    @Test
    fun oversizedPdfIsRejectedBeforeOpeningItsContentUri() {
        val document = MayraDocument(
            uri = "content://missing/oversized.pdf",
            name = "oversized.pdf",
            mimeType = "application/pdf",
            sizeBytes = 51L * 1_048_576L,
            addedAt = 1L
        )

        val result = MayraDocumentTextExtractor(context).extract(document)

        assertTrue(result is DocumentExtractionResult.Failure)
        assertTrue((result as DocumentExtractionResult.Failure).reason.contains("50 MB"))
    }

    @Test
    fun docxRemainsExplicitlyUnsupported() {
        val document = MayraDocument(
            uri = "content://missing/report.docx",
            name = "report.docx",
            mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            sizeBytes = 1_024L,
            addedAt = 2L
        )

        val result = MayraDocumentTextExtractor(context).extract(document)

        assertTrue(result is DocumentExtractionResult.Unsupported)
        assertTrue((result as DocumentExtractionResult.Unsupported).reason.contains("DOC/DOCX"))
    }
}