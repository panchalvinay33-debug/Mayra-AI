package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponseValidatorTest {
    @Test
    fun `normalizes and accepts valid response`() {
        val result = ResponseValidator().validate("  Hello Mayra.  ")
        assertTrue(result.valid)
        assertEquals("Hello Mayra.", result.normalizedContent)
    }

    @Test
    fun `reports length and punctuation violations`() {
        val validator = ResponseValidator(
            ResponseValidationPolicy(minCharacters = 5, maxCharacters = 10, requireTerminalPunctuation = true)
        )
        val result = validator.validate("hey")
        assertFalse(result.valid)
        assertTrue("response_too_short" in result.violations)
        assertTrue("missing_terminal_punctuation" in result.violations)
    }

    @Test
    fun `blocked phrases are case insensitive`() {
        val validator = ResponseValidator(
            ResponseValidationPolicy(blockedPhrases = setOf("secret token"))
        )
        val result = validator.validate("Never reveal the SECRET TOKEN.")
        assertFalse(result.valid)
        assertEquals(listOf("blocked_phrase:secret token"), result.violations)
    }

    @Test
    fun `require valid throws for rejected content`() {
        val validator = ResponseValidator(ResponseValidationPolicy(maxCharacters = 3))
        assertFailsWith<IllegalArgumentException> { validator.requireValid("too long") }
    }
}
