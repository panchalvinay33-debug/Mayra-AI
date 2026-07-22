package ai.mayra.app.core.intelligence

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationRuntimeDiagnosticsTest {

    @Test
    fun `records bounded events and aggregates metrics`() {
        val diagnostics = ConversationRuntimeDiagnostics(maxEvents = 2)
        diagnostics.record(event("s1", success = true, cache = false, attempts = 2, prompt = 100))
        diagnostics.record(event("s1", success = true, cache = true, attempts = 0, prompt = 200))
        diagnostics.record(event("s2", success = false, cache = false, attempts = 1, prompt = 300))

        val metrics = diagnostics.metrics()

        assertEquals(2, metrics.turns)
        assertEquals(1, metrics.successfulTurns)
        assertEquals(1, metrics.failedTurns)
        assertEquals(1, metrics.cacheHits)
        assertEquals(1, metrics.totalAttempts)
        assertEquals(250.0, metrics.averagePromptCharacters)
        assertEquals(1, diagnostics.snapshot("s1").size)
    }

    private fun event(
        sessionId: String,
        success: Boolean,
        cache: Boolean,
        attempts: Int,
        prompt: Int
    ) = ConversationTurnDiagnostic(
        sessionId = sessionId,
        startedAt = Instant.EPOCH,
        completedAt = Instant.EPOCH.plusMillis(10),
        originalContextCharacters = 0,
        promptCharacters = prompt,
        droppedMessages = 0,
        retrievedMemories = 0,
        savedMemories = 0,
        providerId = "test",
        attempts = attempts,
        fromCache = cache,
        success = success
    )
}
