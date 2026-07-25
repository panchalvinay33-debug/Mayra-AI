package ai.mayra.app.document

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraDocumentTextReaderTest {
    @Test fun supportsKnownTextFormats() {
        assertTrue(MayraDocumentTextReader.isSupportedTextType("text/plain", "note.txt"))
        assertTrue(MayraDocumentTextReader.isSupportedTextType("application/json", "data.json"))
        assertTrue(MayraDocumentTextReader.isSupportedTextType("application/octet-stream", "readme.md"))
        assertTrue(MayraDocumentTextReader.isSupportedTextType("text/csv", "items.csv"))
    }

    @Test fun rejectsBinaryAndParserOnlyFormats() {
        assertFalse(MayraDocumentTextReader.isSupportedTextType("application/pdf", "report.pdf"))
        assertFalse(MayraDocumentTextReader.isSupportedTextType("application/msword", "report.doc"))
        assertFalse(MayraDocumentTextReader.isSupportedTextType("image/jpeg", "photo.jpg"))
    }

    @Test fun searchReturnsBoundedLineMatches() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reader = MayraDocumentTextReader(context)
        val preview = DocumentTextPreview.Ready("alpha\nBeta result\ngamma\nbeta second", truncated = false)
        val matches = reader.search(preview, "beta", maxMatches = 1)
        assertEquals(1, matches.size)
        assertEquals(2, matches.first().lineNumber)
        assertTrue(matches.first().preview.contains("Beta"))
    }

    @Test fun shortQueryDoesNotScan() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val reader = MayraDocumentTextReader(context)
        val preview = DocumentTextPreview.Ready("a lot of text", truncated = false)
        assertTrue(reader.search(preview, "a").isEmpty())
    }
}
