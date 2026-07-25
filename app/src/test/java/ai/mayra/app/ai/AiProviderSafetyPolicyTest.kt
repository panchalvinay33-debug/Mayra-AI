package ai.mayra.app.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiProviderSafetyPolicyTest {
    @Test
    fun `valid model survives normalization`() {
        assertEquals("gpt-5-mini", AiProviderSafetyPolicy.normalizeModel("  gpt-5-mini  "))
        assertNull(AiProviderSafetyPolicy.validateModel("gpt-5-mini"))
    }

    @Test
    fun `model rejects whitespace and path shaped values`() {
        assertTrue(AiProviderSafetyPolicy.validateModel("bad model") != null)
        assertTrue(AiProviderSafetyPolicy.validateModel("../model") != null)
    }

    @Test
    fun `new key validation is bounded and structured`() {
        assertNull(AiProviderSafetyPolicy.validateNewApiKey("sk-abcdefgh12345678"))
        assertTrue(AiProviderSafetyPolicy.validateNewApiKey("not-a-key") != null)
        assertTrue(AiProviderSafetyPolicy.normalizeApiKey("x".repeat(900)).length <= 512)
    }

    @Test
    fun `connection message redacts bearer and key material`() {
        val sanitized = AiProviderSafetyPolicy.sanitizeConnectionMessage(
            "Failed\nAuthorization: Bearer sk-secretvalue123456 and sk-anothersecret123456"
        )

        assertFalse(sanitized.contains("secretvalue"))
        assertFalse(sanitized.contains("anothersecret"))
        assertFalse(sanitized.contains('\n'))
        assertTrue(sanitized.length <= AiProviderSafetyPolicy.MAX_CONNECTION_MESSAGE_LENGTH)
    }

    @Test
    fun `message and context are bounded`() {
        assertEquals(8_000, AiProviderSafetyPolicy.boundUserMessage("a".repeat(9_000)).length)
        assertEquals(8_000, AiProviderSafetyPolicy.boundContextMessage("b".repeat(9_000)).length)
    }

    @Test
    fun `online endpoint must use https`() {
        AiProviderSafetyPolicy.requireHttpsEndpoint("https://api.openai.com/v1/responses")
        assertFailsWith<IllegalArgumentException> {
            AiProviderSafetyPolicy.requireHttpsEndpoint("http://example.test")
        }
    }
}
