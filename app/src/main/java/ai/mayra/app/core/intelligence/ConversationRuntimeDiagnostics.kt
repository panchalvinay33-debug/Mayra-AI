package ai.mayra.app.core.intelligence

import java.time.Duration
import java.time.Instant

data class ConversationTurnDiagnostic(
    val sessionId: String,
    val startedAt: Instant,
    val completedAt: Instant,
    val originalContextCharacters: Int,
    val promptCharacters: Int,
    val droppedMessages: Int,
    val retrievedMemories: Int,
    val savedMemories: Int,
    val providerId: String?,
    val attempts: Int,
    val fromCache: Boolean,
    val success: Boolean,
    val detail: String? = null
) {
    val duration: Duration get() = Duration.between(startedAt, completedAt)
}

data class ConversationRuntimeMetrics(
    val turns: Int,
    val successfulTurns: Int,
    val failedTurns: Int,
    val cacheHits: Int,
    val totalAttempts: Int,
    val averagePromptCharacters: Double,
    val averageDurationMillis: Double
)

class ConversationRuntimeDiagnostics(
    private val maxEvents: Int = 200,
    private val now: () -> Instant = Instant::now
) {
    private val events = ArrayDeque<ConversationTurnDiagnostic>()

    init {
        require(maxEvents > 0) { "Maximum diagnostic events must be positive." }
    }

    fun now(): Instant = now.invoke()

    @Synchronized
    fun record(event: ConversationTurnDiagnostic) {
        events.addLast(event)
        while (events.size > maxEvents) events.removeFirst()
    }

    @Synchronized
    fun snapshot(sessionId: String? = null): List<ConversationTurnDiagnostic> = events
        .filter { sessionId == null || it.sessionId == sessionId }

    @Synchronized
    fun metrics(): ConversationRuntimeMetrics {
        val list = events.toList()
        return ConversationRuntimeMetrics(
            turns = list.size,
            successfulTurns = list.count { it.success },
            failedTurns = list.count { !it.success },
            cacheHits = list.count { it.fromCache },
            totalAttempts = list.sumOf { it.attempts },
            averagePromptCharacters = averageInts(list.map { it.promptCharacters }),
            averageDurationMillis = averageDoubles(list.map { it.duration.toMillis().toDouble() })
        )
    }

    private fun averageDoubles(values: List<Double>): Double =
        if (values.isEmpty()) 0.0 else values.average()

    private fun averageInts(values: List<Int>): Double =
        if (values.isEmpty()) 0.0 else values.average()
}
