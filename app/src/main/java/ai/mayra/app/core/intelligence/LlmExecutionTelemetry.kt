package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class LlmExecutionOutcome {
    SUCCESS,
    CACHE_HIT,
    VALIDATION_FAILED,
    GENERATION_FAILED
}

data class LlmExecutionEvent(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val providerId: String?,
    val outcome: LlmExecutionOutcome,
    val attempts: Int,
    val promptCharacters: Int,
    val responseCharacters: Int,
    val startedAt: Instant,
    val completedAt: Instant,
    val detail: String? = null
) {
    init {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
        require(attempts >= 0) { "Attempts cannot be negative." }
        require(promptCharacters >= 0) { "Prompt character count cannot be negative." }
        require(responseCharacters >= 0) { "Response character count cannot be negative." }
        require(!completedAt.isBefore(startedAt)) { "Completion cannot precede start." }
    }
}

data class LlmExecutionMetrics(
    val total: Int,
    val successes: Int,
    val cacheHits: Int,
    val failures: Int,
    val averageAttempts: Double,
    val averageResponseCharacters: Double
)

/** Bounded in-memory execution history for diagnostics and product metrics. */
class LlmExecutionTelemetry(
    private val maxEvents: Int = 500,
    private val clock: Clock = Clock.systemUTC()
) {
    private val events = ArrayDeque<LlmExecutionEvent>()

    init {
        require(maxEvents > 0) { "Maximum event count must be positive." }
    }

    fun now(): Instant = clock.instant()

    @Synchronized
    fun record(event: LlmExecutionEvent) {
        events.addLast(event)
        while (events.size > maxEvents) events.removeFirst()
    }

    @Synchronized
    fun snapshot(sessionId: String? = null): List<LlmExecutionEvent> =
        events.filter { sessionId == null || it.sessionId == sessionId }

    @Synchronized
    fun metrics(): LlmExecutionMetrics {
        if (events.isEmpty()) return LlmExecutionMetrics(0, 0, 0, 0, 0.0, 0.0)
        val successCount = events.count {
            it.outcome == LlmExecutionOutcome.SUCCESS || it.outcome == LlmExecutionOutcome.CACHE_HIT
        }
        val cacheHits = events.count { it.outcome == LlmExecutionOutcome.CACHE_HIT }
        return LlmExecutionMetrics(
            total = events.size,
            successes = successCount,
            cacheHits = cacheHits,
            failures = events.size - successCount,
            averageAttempts = events.map { it.attempts }.average(),
            averageResponseCharacters = events.map { it.responseCharacters }.average()
        )
    }

    @Synchronized
    fun clear() = events.clear()
}
