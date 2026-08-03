package ai.mayra.app.assistant

import org.junit.Assert.assertEquals
import org.junit.Test

class MayraSpeechLocalePolicyTest {
    @Test
    fun `device locale is tried first then India fallbacks`() {
        assertEquals(
            listOf("gu-IN", "hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("gu-IN")
        )
    }

    @Test
    fun `duplicates are removed case insensitively`() {
        assertEquals(
            listOf("hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("HI-in")
        )
    }

    @Test
    fun `blank device locale still has safe fallbacks`() {
        assertEquals(
            listOf("hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("")
        )
    }
}
