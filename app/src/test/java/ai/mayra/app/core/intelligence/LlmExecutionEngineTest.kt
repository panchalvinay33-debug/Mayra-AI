package ai.mayra.app.core.intelligence

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmExecutionEngineTest {

    @Test
    fun `successful response is normalized cached and measured`() = runTest {
        val provider = QueueProvider("remote", ArrayDeque(listOf(Result.success("  Hello Mayra.  "))))
        val engine = engine(provider = provider, cache = PromptCache())
        val request = executionRequest()

        val first = engine.execute(request)
        val second = engine.execute(request)

        assertEquals("Hello Mayra.", first.response.content)
        assertFalse(first.fromCache)
        assertTrue(second.fromCache)
        assertEquals(1, provider.generationCalls)
        assertEquals(2, engine.executionMetrics().total)
        assertEquals(1, engine.executionMetrics().cacheHits)
        assertEquals(1, engine.providerHealth("remote").successes)
    }

    @Test
    fun `transient generation failure is retried without real delay`() = runTest {
        val provider = QueueProvider(
            "remote",
            ArrayDeque(
                listOf(
                    Result.failure(IllegalStateException("network")),
                    Result.success("Recovered.")
                )
            )
        )
        val delays = mutableListOf<Long>()
        val engine = engine(provider = provider, sleeper = delays::add)

        val result = engine.execute(executionRequest(cacheable = false))

        assertEquals(2, result.attempts)
        assertEquals(listOf(10L), delays)
        assertEquals(2, provider.generationCalls)
    }

    @Test
    fun `invalid response is retried and provider health is updated`() = runTest {
        val provider = QueueProvider(
            "local",
            ArrayDeque(listOf(Result.success("x"), Result.success("Valid answer.")))
        )
        val engine = LlmExecutionEngine(
            router = LlmProviderRouter(listOf(provider)),
            validator = ResponseValidator(ResponseValidationPolicy(minCharacters = 2)),
            retryPolicy = LlmRetryPolicy(maxAttempts = 2, initialDelayMillis = 0),
            sleeper = {}
        )

        val result = engine.execute(executionRequest(cacheable = false))

        assertEquals("Valid answer.", result.response.content)
        assertEquals(2, result.attempts)
        val health = engine.providerHealth("local")
        assertEquals(1, health.failures)
        assertEquals(1, health.successes)
        assertEquals(0, health.consecutiveFailures)
    }

    @Test
    fun `permanent failure records telemetry and propagates`() = runTest {
        val provider = QueueProvider(
            "remote",
            ArrayDeque(listOf(Result.failure(IllegalArgumentException("bad request"))))
        )
        val engine = LlmExecutionEngine(
            router = LlmProviderRouter(listOf(provider)),
            retryPolicy = LlmRetryPolicy(
                maxAttempts = 3,
                initialDelayMillis = 0,
                retryable = { it !is NoAvailableLlmProviderException }
            ),
            sleeper = {}
        )

        assertFailsWith<NoAvailableLlmProviderException> {
            engine.execute(executionRequest(cacheable = false))
        }
        assertEquals(LlmExecutionOutcome.GENERATION_FAILED, engine.executionEvents().single().outcome)
    }

    private fun engine(
        provider: LlmProvider,
        cache: PromptCache? = null,
        sleeper: suspend (Long) -> Unit = {}
    ) = LlmExecutionEngine(
        router = LlmProviderRouter(listOf(provider)),
        cache = cache,
        retryPolicy = LlmRetryPolicy(maxAttempts = 3, initialDelayMillis = 10),
        sleeper = sleeper
    )

    private fun executionRequest(cacheable: Boolean = true): LlmExecutionRequest {
        val prompt = AssembledPrompt(
            sessionId = "session-1",
            messages = listOf(PromptMessage(ConversationRole.USER, "Hello")),
            includedMemoryIds = emptyList(),
            estimatedCharacters = 13,
            truncated = false,
            metadata = emptyMap()
        )
        return LlmExecutionRequest(LlmRequest(prompt), cacheable = cacheable)
    }

    private class QueueProvider(
        override val id: String,
        private val responses: ArrayDeque<Result<String>>
    ) : LlmProvider {
        var generationCalls: Int = 0
            private set

        override suspend fun isAvailable(): Boolean = true

        override suspend fun generate(request: LlmRequest): LlmResponse {
            generationCalls += 1
            val result = responses.removeFirstOrNull()
                ?: error("No queued provider response.")
            return LlmResponse(providerId = id, content = result.getOrThrow())
        }
    }
}
