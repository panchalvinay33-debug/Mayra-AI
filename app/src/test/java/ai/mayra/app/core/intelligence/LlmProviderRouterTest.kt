package ai.mayra.app.core.intelligence

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LlmProviderRouterTest {

    private val request = LlmRequest(
        AssembledPrompt(
            sessionId = "s1",
            messages = listOf(PromptMessage(ConversationRole.USER, "hello")),
            includedMemoryIds = emptyList(),
            estimatedCharacters = 13,
            truncated = false,
            metadata = emptyMap()
        )
    )

    @Test
    fun `highest priority available provider is selected`() = runBlocking {
        val low = FakeProvider("low", 1, response = "low")
        val high = FakeProvider("high", 10, response = "high")

        val result = LlmProviderRouter(listOf(low, high)).generate(request)

        assertEquals("high", result.providerId)
        assertEquals(0, low.generateCalls)
    }

    @Test
    fun `router fails over after provider error`() = runBlocking {
        val broken = FakeProvider("broken", 10, failure = IllegalStateException("offline"))
        val fallback = FakeProvider("fallback", 1, response = "ok")

        val result = LlmProviderRouter(listOf(broken, fallback)).generate(request)

        assertEquals("fallback", result.providerId)
        assertEquals(1, broken.generateCalls)
        assertEquals(1, fallback.generateCalls)
    }

    @Test
    fun `preferred provider is attempted first`() = runBlocking {
        val high = FakeProvider("high", 10, response = "high")
        val preferred = FakeProvider("preferred", 1, response = "preferred")

        val result = LlmProviderRouter(listOf(high, preferred))
            .generate(request, preferredProviderId = "preferred")

        assertEquals("preferred", result.providerId)
        assertEquals(0, high.generateCalls)
    }

    @Test
    fun `all unavailable providers produce meaningful error`() = runBlocking {
        val router = LlmProviderRouter(
            listOf(FakeProvider("one", 1, available = false), FakeProvider("two", 2, available = false))
        )

        val error = assertFailsWith<NoAvailableLlmProviderException> { router.generate(request) }
        assertTrue(error.message.orEmpty().contains("unavailable"))
    }

    private class FakeProvider(
        override val id: String,
        override val priority: Int,
        private val available: Boolean = true,
        private val response: String = "response",
        private val failure: Throwable? = null
    ) : LlmProvider {
        var generateCalls: Int = 0

        override suspend fun isAvailable(): Boolean = available

        override suspend fun generate(request: LlmRequest): LlmResponse {
            generateCalls += 1
            failure?.let { throw it }
            return LlmResponse(providerId = id, content = response)
        }
    }
}