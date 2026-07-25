package ai.mayra.app.voice

import ai.mayra.app.settings.MayraLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MayraSpeechTextPolicyTest {
    @Test
    fun `markdown links and code are not read literally`() {
        val spoken = MayraSpeechTextPolicy.prepare(
            "# Answer\n- Open https://example.com\n```kotlin\nprintln(\"secret\")\n```"
        )

        assertFalse(spoken.contains("https://"))
        assertFalse(spoken.contains("println"))
        assertFalse(spoken.contains("#"))
        assertTrue(spoken.contains("Answer"))
        assertTrue(spoken.contains("link"))
    }

    @Test
    fun `devanagari selects Hindi voice`() {
        assertEquals("hi-IN", MayraSpeechTextPolicy.languageTag("नमस्ते, मैं मायरा हूँ", MayraLanguage.HINGLISH))
    }

    @Test
    fun `roman Hinglish selects Indian English voice`() {
        assertEquals("en-IN", MayraSpeechTextPolicy.languageTag("Namaste, main Mayra hoon", MayraLanguage.HINGLISH))
    }
}