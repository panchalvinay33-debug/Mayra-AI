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
    fun `duplicates are normalized and removed case insensitively`() {
        assertEquals(
            listOf("hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("HI-in")
        )
    }

    @Test
    fun `underscore locale is normalized to bcp47`() {
        assertEquals(
            listOf("gu-IN", "hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("gu_IN")
        )
    }

    @Test
    fun `blank or undetermined device locale still has safe fallbacks`() {
        assertEquals(
            listOf("hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("")
        )
        assertEquals(
            listOf("hi-IN", "en-IN", "en-US"),
            MayraSpeechLocalePolicy.candidates("und")
        )
    }

    @Test
    fun `installed locales follow preferred order before extra installed languages`() {
        assertEquals(
            listOf("hi-IN", "en-IN", "fr-FR"),
            MayraSpeechLocalePolicy.installedCandidates(
                preferred = listOf("gu-IN", "hi-IN", "en-IN", "en-US"),
                installed = listOf("fr-fr", "EN_in", "HI-in")
            )
        )
    }

    @Test
    fun `empty installed locale list stays empty`() {
        assertEquals(
            emptyList<String>(),
            MayraSpeechLocalePolicy.installedCandidates(
                preferred = listOf("hi-IN", "en-IN", "en-US"),
                installed = emptyList()
            )
        )
    }
}
