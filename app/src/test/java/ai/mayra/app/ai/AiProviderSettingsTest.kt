package ai.mayra.app.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AiProviderSettingsTest {
    @Test
    fun `local provider is valid without key`() {
        val config = AiProviderConfig(provider = AiProviderKind.LOCAL_ONLY)

        assertNull(config.validationMessage(null))
        assertFalse(config.onlineEnabled)
        assertEquals("Local assistant active", config.status())
    }

    @Test
    fun `OpenAI requires a key when none is stored`() {
        val config = AiProviderConfig(
            provider = AiProviderKind.OPENAI,
            model = "gpt-5-mini",
            apiKeyConfigured = false
        )

        assertEquals("Enter an OpenAI API key.", config.validationMessage(""))
        assertEquals("OpenAI key required", config.status())
    }

    @Test
    fun `OpenAI accepts previously stored key`() {
        val config = AiProviderConfig(
            provider = AiProviderKind.OPENAI,
            model = "gpt-5-mini",
            apiKeyConfigured = true
        )

        assertNull(config.validationMessage(null))
        assertTrue(config.onlineEnabled)
    }

    @Test
    fun `new key must use expected prefix`() {
        val config = AiProviderConfig(
            provider = AiProviderKind.OPENAI,
            model = "gpt-5-mini"
        )

        assertEquals(
            "OpenAI API keys normally start with sk-.",
            config.validationMessage("not-a-key")
        )
    }

    @Test
    fun `connected status never exposes key`() {
        val config = AiProviderConfig(
            provider = AiProviderKind.OPENAI,
            model = "gpt-5-mini",
            apiKeyConfigured = true,
            lastConnectionSuccessAt = 123L,
            lastConnectionMessage = "Connected successfully"
        )

        val status = config.status()
        assertEquals("OpenAI connected · gpt-5-mini", status)
        assertFalse(status.contains("sk-"))
    }

    @Test
    fun `blank model is rejected`() {
        val config = AiProviderConfig(
            provider = AiProviderKind.OPENAI,
            model = " ",
            apiKeyConfigured = true
        )

        assertEquals("Enter an OpenAI model name.", config.validationMessage(null))
        assertFalse(config.onlineEnabled)
    }
}