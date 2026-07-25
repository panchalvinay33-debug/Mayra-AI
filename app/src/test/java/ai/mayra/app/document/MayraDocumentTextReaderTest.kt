package ai.mayra.app.document

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
