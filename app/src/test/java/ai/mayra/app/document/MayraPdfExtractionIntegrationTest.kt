package ai.mayra.app.document

import ai.mayra.app.TestMayraApplication
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestMayraApplication::class)
class MayraPdfExtractionIntegrationTest {
    @Test
    fun extractsSearchableTextFromGeneratedPdf() {
        val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
        PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "generated-text.pdf")
        PDDocument().use { pdf ->
            val page = PDPage()
            pdf.addPage(page)
            PDPageContentStream(pdf, page).use { content ->
                content.beginText()
                content.setFont(PDType1Font.HELVETICA, 12f)
                content.newLineAtOffset(72f, 720f)
                content.showText("Payment terms are thirty days from invoice date")
                content.endText()
            }
            pdf.save(file)
        }

        val result = MayraDocumentTextExtractor(context).extract(
            MayraDocument(
                uri = Uri.fromFile(file).toString(),
                name = file.name,
                mimeType = "application/pdf",
                sizeBytes = file.length(),
                addedAt = 1L
            )
        )

        assertTrue(result is DocumentExtractionResult.Success)
        val success = result as DocumentExtractionResult.Success
        assertTrue(success.text.contains("Payment terms are thirty days from invoice date"))
        assertFalse(success.truncated)
    }

    @Test
    fun emptyPdfReturnsBlankSuccessfulExtractionForFutureOcrHandling() {
        val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
        PDFBoxResourceLoader.init(context)
        val file = File(context.cacheDir, "generated-empty.pdf")
        PDDocument().use { pdf ->
            pdf.addPage(PDPage())
            pdf.save(file)
        }

        val result = MayraDocumentTextExtractor(context).extract(
            MayraDocument(
                uri = Uri.fromFile(file).toString(),
                name = file.name,
                mimeType = "application/pdf",
                sizeBytes = file.length(),
                addedAt = 2L
            )
        )

        assertTrue(result is DocumentExtractionResult.Success)
        assertTrue((result as DocumentExtractionResult.Success).text.isBlank())
    }
}
