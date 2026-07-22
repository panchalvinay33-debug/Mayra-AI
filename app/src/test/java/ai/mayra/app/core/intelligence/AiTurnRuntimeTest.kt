package ai.mayra.app.core.intelligence

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiTurnRuntimeTest {

    @Test
    fun `successful turn persists messages memory and diagnostics`() = runTest {
        val store = InMemoryMemoryStore()
        val sessions = ConversationSessionManager(idFactory = { "session-1" })
        sessions.create(title = "Test")
        val provider = FixedProvider("remote", "  Namaste Vinay.  ")
        val runtime = runtime(sessions, store, provider, PromptCache())

        val result = runtime.execute(
            AiTurnRuntimeRequest(
                sessionId = "session-1",
                userInput = "My name is Vinay and I prefer concise replies."
            )
        )

        assertEquals("Namaste Vinay.", result.response.content)
        assertFalse(result.execution.fromCache)
        assertEquals(2, sessions.snapshot("session-1").messages.size)
        assertTrue(store.all().isNotEmpty())
        assertEquals(1, runtime.runtimeMetrics().successfulTurns)
        assertEquals("remote", runtime.runtimeEvents().single().providerId)
    }

    @Test
    fun `identical request uses execution cache`() = runTest {
        val store = InMemoryMemoryStore()
        val sessions = ConversationSessionManager(idFactory = { "session-1" })
        sessions.create()
        val provider = FixedProvider("remote", "Cached answer.")
        val runtime = runtime(sessions, store, provider, PromptCache())

        val request = AiTurnRuntimeRequest(sessionId = "session-1", userInput = "Hello Mayra")
        val first = runtime.execute(request)
        sessions.delete("session-1")

        val freshSessions = ConversationSessionManager(idFactory = { "session-1" })
        freshSessions.create()
        val secondRuntime = runtime(freshSessions, store, provider, sharedCache)
        val second = secondRuntime.execute(request)

        assertFalse(first.execution.fromCache)
        assertTrue(second.execution.fromCache)
    }

    private val sharedCache = PromptCache()

    private fun runtime(
        sessions: ConversationSessionManager,
        store: InMemoryMemoryStore,
        provider: LlmProvider,
        cache: PromptCache
    ): AiTurnRuntime {
        val engine = LlmExecutionEngine(
            router = LlmProviderRouter(listOf(provider)),
            cache = cache,
            retryPolicy = LlmRetryPolicy(maxAttempts = 1, initialDelayMillis = 0),
            sleeper = {}
        )
        return AiTurnRuntime(
            sessions = sessions,
            memoryRetriever = MemoryRetriever(store),
            promptBuilder = PromptBuilder(),
            executionEngine = engine,
            memoryConsolidator = MemoryConsolidator(store),
            budgetAllocator = PromptBudgetAllocator(
                minimumInputCharacters = 500,
                maximumPromptCharacters = 4_000,
                defaultResponseCharacters = 1_000
            )
        )
    }

    private class FixedProvider(
        override val id: String,
        private val value: String
    ) : LlmProvider {
        var calls: Int = 0
            private set

        override suspend fun isAvailable(): Boolean = true

        override suspend fun generate(request: LlmRequest): LlmResponse {
            calls += 1
            return LlmResponse(providerId = id, content = value)
        }
    }
}
