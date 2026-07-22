package ai.mayra.app.core.intelligence

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class LlmExecutionTelemetryTest {

    @Test
    fun `history is bounded and metrics aggregate outcomes`() {
        val telemetry = LlmExecutionTelemetry(maxEvents = 2)
        val now = Instant.parse("2026-07-22T10:00:00Z")

        telemetry.record(event("s1", LlmExecutionOutcome.SUCCESS, 2, 20, now))
        telemetry.record(event("s1", LlmExecutionOutcome.CACHE_HIT, 0, 20, now))
        telemetry.record(event("s2", LlmExecutionOutcome.GENERATION_FAILED, 3, 0, now))

        assertEquals(2, telemetry.snapshot().size)
        assertEquals(1, telemetry.snapshot("s1").size)

        val metrics = telemetry.metrics()
        assertEquals(2, metrics.total)
        assertEquals(1, metrics.successes)
        assertEquals(1, metrics.cacheHits)
        assertEquals(1, metrics.failures)
        assertEquals(1.5, metrics.averageAttempts)
        assertEquals(10.0, metrics.averageResponseCharacters)
    }

    private fun event(
        sessionId: String,
        outcome: LlmExecutionOutcome,
        attempts: Int,
        responseCharacters: Int,
        at: Instant
    ) = LlmExecutionEvent(
        sessionId = sessionId,
        providerId = "provider",
        outcome = outcome,
        attempts = attempts,
        promptCharacters = 50,
        responseCharacters = responseCharacters,
        startedAt = at,
        completedAt = at
    )
}
