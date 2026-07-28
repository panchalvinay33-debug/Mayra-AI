package ai.mayra.app.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraHttpConversationalProviderTest {
    private val request = MayraProviderRequest("Namaste", emptyList(), "hi-IN")

    @Test fun disabledProviderDoesNotOpenNetwork() = runBlocking {
        var opened = false
        val provider = MayraHttpConversationalProvider(
            config(enabled = false),
            MayraProviderCredentialSource { "token" },
            MayraHttpConnectionFactory { opened = true; FakeConnection(it, 200, "{\"text\":\"ok\"}") }
        )

        val result = provider.answer(request)

        assertTrue(result is MayraProviderResult.PermanentFailure)
        assertEquals(false, opened)
        assertEquals(MayraProviderHealthState.DISABLED, provider.health().state)
    }

    @Test fun missingCredentialDoesNotOpenNetwork() = runBlocking {
        var opened = false
        val provider = MayraHttpConversationalProvider(
            config(),
            MayraProviderCredentialSource { null },
            MayraHttpConnectionFactory { opened = true; FakeConnection(it, 200, "{\"text\":\"ok\"}") }
        )

        val result = provider.answer(request)

        assertTrue(result is MayraProviderResult.PermanentFailure)
        assertEquals(false, opened)
        assertEquals(MayraProviderHealthState.MISSING_CREDENTIAL, provider.health().state)
    }

    @Test fun successfulJsonTextIsReturnedAndHealthBecomesReady() = runBlocking {
        val connection = FakeConnection(URL("https://example.test"), 200, "{\"text\":\"Namaste \\u0926\\u094b\\u0938\\u094d\\u0924\"}")
        val provider = provider(connection)

        val result = provider.answer(request) as MayraProviderResult.Success

        assertEquals("Namaste दोस्त", result.text)
        assertEquals(MayraProviderHealthState.READY, provider.health().state)
        assertTrue(connection.writtenBody().contains("\"locale\":\"hi-IN\""))
        assertEquals("Bearer token", connection.requestProperties["Authorization"]?.single())
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
            MayraProviderCredentialSource { "token" },
            MayraHttpConnectionFactory { connection }
        )

        val result = provider.answer(request)

        assertTrue(result is MayraProviderResult.PermanentFailure)
        assertTrue((result as MayraProviderResult.PermanentFailure).reason.contains("size limit"))
    }

    private fun provider(connection: FakeConnection) = MayraHttpConversationalProvider(
        config(),
        MayraProviderCredentialSource { "token" },
        MayraHttpConnectionFactory { connection }
    )

    private fun config(enabled: Boolean = true, maxResponseBytes: Int = 256_000) = MayraHttpProviderConfig(
        endpoint = "https://example.test/v1/chat",
        model = "mayra-test",
        enabled = enabled,
        maxResponseBytes = maxResponseBytes
    )

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
