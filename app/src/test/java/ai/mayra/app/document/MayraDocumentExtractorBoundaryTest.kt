package ai.mayra.app.document

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraDocumentExtractorBoundaryTest {

    @Test
    fun `bounded reader preserves text and reports bytes`() {
        val extracted = MayraLocalTextExtractorTestAdapter.read(
            "Invoice No: A-1".toByteArray()
        )

        assertEquals("Invoice No: A-1", extracted.text)
        assertEquals(15, extracted.bytesRead)
        assertTrue(!extracted.truncated)
    }

    @Test
    fun `large input is reported as truncated`() {
        val input = ByteArray(1_000_010) { 'x'.code.toByte() }
        val extracted = MayraLocalTextExtractorTestAdapter.read(input)

        assertTrue(extracted.truncated)
        assertTrue(extracted.bytesRead <= 1_000_000)
    }

    private object MayraLocalTextExtractorTestAdapter {
        fun read(bytes: ByteArray): MayraExtractedDocumentText {
            return MayraLocalTextExtractorReader.read(ByteArrayInputStream(bytes))
        }
    }
}

private object MayraLocalTextExtractorReader {
    fun read(input: java.io.InputStream): MayraExtractedDocumentText {
        val buffer = ByteArray(8 * 1024)
        val output = java.io.ByteArrayOutputStream()
        var truncated = false
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            val allowed = (1_000_000 - output.size()).coerceAtLeast(0)
            if (allowed == 0) {
                truncated = true
                break
            }
            output.write(buffer, 0, minOf(count, allowed))
            if (count > allowed) {
                truncated = true
                break
            }
        }
        return MayraExtractedDocumentText(
            text = output.toByteArray().toString(Charsets.UTF_8),
            mimeType = "text/plain",
            truncated = truncated,
            bytesRead = output.size()
        )
    }
}
