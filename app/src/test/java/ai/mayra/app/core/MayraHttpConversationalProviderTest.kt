package ai.mayra.app.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraHttpConversationalProviderTest {
    private val request = MayraProviderRequest("Namaste", emptyList(), "hi-IN")

    @Test fun disabledProviderDoesNotOpenNetwork() = runBlocking {
        var opened = false
        val provider = MayraHttpConversationalProvider(
            config(enabled = false),
            MayraProviderCredentialSource { "token-value-123" },
            MayraHttpConnectionFactory { opened = true; FakeConnection(it, 200, simpleText("ok")) }
        )

        val result = provider.answer(request)

        assertTrue(result is MayraProviderResult.PermanentFailure)
        assertFalse(opened)
        assertEquals(MayraProviderHealthState.DISABLED, provider.health().state)
    }

    @Test fun missingCredentialDoesNotOpenNetwork() = runBlocking {
        var opened = false
        val provider = MayraHttpConversationalProvider(
            config(),
            MayraProviderCredentialSource { null },
            MayraHttpConnectionFactory { opened = true; FakeConnection(it, 200, simpleText("ok")) }
        )

        val result = provider.answer(request)

        assertTrue(result is MayraProviderResult.PermanentFailure)
        assertFalse(opened)
        assertEquals(MayraProviderHealthState.MISSING_CREDENTIAL, provider.health().state)
    }

    @Test fun openAiResponsesOutputTextIsReturnedAndHealthBecomesReady() = runBlocking {
        val response = """{"output":[{"type":"message","content":[{"type":"output_text","text":"Namaste \\u0926\\u094b\\u0938\\u094d\\u0924"}]}]}"""
        val connection = FakeConnection(URL("https://api.openai.com/v1/responses"), 200, response)
        val provider = provider(connection)

        val result = provider.answer(request) as MayraProviderResult.Success

        assertEquals("Namaste दोस्त", result.text)
        assertEquals(MayraProviderHealthState.READY, provider.health().state)
        assertEquals("Bearer token-value-123", connection.requestProperties["Authorization"]?.single())
        val body = connection.writtenBody()
        assertTrue(body.contains("\"model\":\"gpt-5.6\""))
        assertTrue(body.contains("\"input\":["))
        assertTrue(body.contains("\"store\":false"))
        assertTrue(body.contains("\"max_output_tokens\":1200"))
        assertFalse(body.contains("\"locale\""))
    }

    @Test fun compatibleTopLevelTextIsStillAccepted() = runBlocking {
        val result = provider(FakeConnection(URL("https://example.test"), 200, simpleText("Hello")))
            .answer(request) as MayraProviderResult.Success
        assertEquals("Hello", result.text)
    }

    @Test fun retryableHttpCodesAreTemporary() = runBlocking {
        val provider = provider(FakeConnection(URL("https://example.test"), 429, "{\"error\":\"busy\"}"))
        assertTrue(provider.answer(request) is MayraProviderResult.TemporaryFailure)
        assertEquals(MayraProviderHealthState.TEMPORARY_FAILURE, provider.health().state)
    }

    @Test fun clientErrorsArePermanent() = runBlocking {
        val provider = provider(FakeConnection(URL("https://example.test"), 401, "{\"error\":\"unauthorized\"}"))
        assertTrue(provider.answer(request) is MayraProviderResult.PermanentFailure)
        assertEquals(MayraProviderHealthState.PERMANENT_FAILURE, provider.health().state)
    }

    @Test fun oversizedResponseIsRejected() = runBlocking {
        val connection = FakeConnection(URL("https://example.test"), 200, "x".repeat(2_000))
        val provider = MayraHttpConversationalProvider(
            config(maxResponseBytes = 1_024),
            MayraProviderCredentialSource { "token-value-123" },
            MayraHttpConnectionFactory { connection }
        )

        val result = provider.answer(request)

        assertTrue(result is MayraProviderResult.PermanentFailure)
        assertTrue((result as MayraProviderResult.PermanentFailure).reason.contains("size limit"))
    }

    private fun provider(connection: FakeConnection) = MayraHttpConversationalProvider(
        config(),
        MayraProviderCredentialSource { "token-value-123" },
        MayraHttpConnectionFactory { connection }
    )

    private fun config(enabled: Boolean = true, maxResponseBytes: Int = 256_000) = MayraHttpProviderConfig(
        endpoint = "https://api.openai.com/v1/responses",
        model = "gpt-5.6",
        enabled = enabled,
        maxResponseBytes = maxResponseBytes
    )

    private fun simpleText(value: String) = "{\"text\":\"$value\"}"

    private class FakeConnection(
        url: URL,
        private val code: Int,
        private val response: String
    ) : HttpURLConnection(url) {
        private val output = ByteArrayOutputStream()
        val requestProperties = linkedMapOf<String, MutableList<String>>()

        override fun connect() = Unit
        override fun disconnect() = Unit
        override fun usingProxy(): Boolean = false
        override fun getResponseCode(): Int = code
        override fun getInputStream(): InputStream = ByteArrayInputStream(response.toByteArray())
        override fun getErrorStream(): InputStream = ByteArrayInputStream(response.toByteArray())
        override fun getOutputStream() = output
        override fun setRequestProperty(key: String, value: String) {
            requestProperties[key] = mutableListOf(value)
        }
        override fun getRequestProperties(): MutableMap<String, MutableList<String>> = requestProperties
        fun writtenBody(): String = output.toString(Charsets.UTF_8.name())
    }
}
