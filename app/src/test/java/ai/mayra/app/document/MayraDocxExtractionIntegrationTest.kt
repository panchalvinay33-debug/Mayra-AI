package ai.mayra.app.document

import ai.mayra.app.TestMayraApplication
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestMayraApplication::class)
class MayraDocxExtractionIntegrationTest {
    @Test
    fun extractsBodyHeaderAndTableTextFromGeneratedDocx() {
        val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
        val file = File(context.cacheDir, "generated.docx")
        writeDocx(
            file,
            mapOf(
                "word/document.xml" to wordDocumentXml(
                    "<w:p><w:r><w:t>Payment terms are thirty days</w:t></w:r></w:p>" +
                        "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>Invoice</w:t></w:r></w:p></w:tc>" +
                        "<w:tc><w:p><w:r><w:t>INV-2026-41</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
                ),
                "word/header1.xml" to wordHeaderXml("Confidential customer agreement")
            )
        )

        val result = MayraDocumentTextExtractor(context).extract(document(file))

        assertTrue(result is DocumentExtractionResult.Success)
        val success = result as DocumentExtractionResult.Success
        assertTrue(success.text.contains("Payment terms are thirty days"))
        assertTrue(success.text.contains("Invoice"))
        assertTrue(success.text.contains("INV-2026-41"))
        assertTrue(success.text.contains("Confidential customer agreement"))
        assertFalse(success.truncated)
    }

    @Test
    fun missingMainDocumentPartReturnsClearFailure() {
        val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
        val file = File(context.cacheDir, "missing-main.docx")
        writeDocx(file, mapOf("word/header1.xml" to wordHeaderXml("Header only")))

        val result = MayraDocumentTextExtractor(context).extract(document(file))

        assertTrue(result is DocumentExtractionResult.Failure)
        assertTrue((result as DocumentExtractionResult.Failure).reason.contains("main document XML"))
    }

    @Test
    fun oversizedDocxIsRejectedBeforeOpeningStream() {
        val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
        val result = MayraDocumentTextExtractor(context).extract(
            MayraDocument(
                uri = "content://missing/large.docx",
                name = "large.docx",
                mimeType = DOCX_MIME,
                sizeBytes = MayraDocxTextExtractor.MAX_DOCX_BYTES + 1,
                addedAt = 1L
            )
        )

        assertTrue(result is DocumentExtractionResult.Failure)
        assertTrue((result as DocumentExtractionResult.Failure).reason.contains("25 MB"))
    }

    @Test
    fun legacyDocExplainsConversionPath() {
        val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
        val result = MayraDocumentTextExtractor(context).extract(
            MayraDocument(
                uri = "content://docs/legacy.doc",
                name = "legacy.doc",
                mimeType = "application/msword",
                sizeBytes = 100,
                addedAt = 2L
            )
        )

        assertTrue(result is DocumentExtractionResult.Unsupported)
        assertTrue((result as DocumentExtractionResult.Unsupported).reason.contains("DOCX"))
    }

    private fun document(file: File) = MayraDocument(
        uri = Uri.fromFile(file).toString(),
        name = file.name,
        mimeType = DOCX_MIME,
        sizeBytes = file.length(),
        addedAt = 1L
    )

    private fun writeDocx(file: File, entries: Map<String, String>) {
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            entries.forEach { (name, xml) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(xml.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    private fun wordDocumentXml(body: String) =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<w:document xmlns:w=\"$WORD_NAMESPACE\"><w:body>$body</w:body></w:document>"

    private fun wordHeaderXml(text: String) =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<w:hdr xmlns:w=\"$WORD_NAMESPACE\"><w:p><w:r><w:t>$text</w:t></w:r></w:p></w:hdr>"

    private companion object {
        const val DOCX_MIME =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        const val WORD_NAMESPACE =
            "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    }
}
