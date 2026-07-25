package ai.mayra.app.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraDocumentExcerptTest {
    @Test fun appendsSelectedLinesWithNewline() {
        val result = appendExcerpt("First useful line", "Second useful line")
        assertEquals("First useful line\nSecond useful line", result)
    }

    @Test fun ignoresBlankSelectedLine() {
        assertEquals("Existing", appendExcerpt("Existing", "   "))
    }

    @Test fun boundsCombinedExcerpt() {
        val result = appendExcerpt("12345", "67890", maxCharacters = 8)
        assertEquals(8, result.length)
        assertTrue(result.startsWith("12345"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidMaximum() {
        appendExcerpt("", "line", maxCharacters = 0)
    }
}
