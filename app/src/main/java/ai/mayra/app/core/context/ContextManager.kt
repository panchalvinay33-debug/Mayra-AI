package ai.mayra.app.core.context

import java.util.UUID

/**
 * Lightweight in-memory conversation context for Mayra's offline-first runtime.
 *
 * The manager intentionally has no Android dependency so it can be unit-tested
 * and reused by future voice, text, automation, and plugin entry points.
 */
class ContextManager(
    private val maxTurns: Int = DEFAULT_MAX_TURNS
) {
    init {
        require(maxTurns > 0) { "maxTurns must be greater than zero" }
    }

    private val sessions = mutableMapOf<String, ArrayDeque<ConversationTurn>>()

    @Synchronized
    fun createSession(): String {
        val sessionId = UUID.randomUUID().toString()
        sessions[sessionId] = ArrayDeque()
        return sessionId
    }

    @Synchronized
    fun addTurn(
        sessionId: String,
        role: ConversationRole,
        text: String,
        timestampMillis: Long = System.currentTimeMillis()
    ) {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) return

        val turns = sessions.getOrPut(sessionId) { ArrayDeque() }
        turns.addLast(
            ConversationTurn(
                role = role,
                text = normalizedText,
                timestampMillis = timestampMillis
            )
        )

        while (turns.size > maxTurns) {
            turns.removeFirst()
        }
    }

    @Synchronized
    fun getRecentTurns(
        sessionId: String,
        limit: Int = maxTurns
    ): List<ConversationTurn> {
        if (limit <= 0) return emptyList()
        return sessions[sessionId]
            ?.takeLast(limit.coerceAtMost(maxTurns))
            .orEmpty()
    }

    @Synchronized
    fun latestUserMessage(sessionId: String): String? =
        sessions[sessionId]
            ?.lastOrNull { it.role == ConversationRole.USER }
            ?.text

    @Synchronized
    fun clearSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    @Synchronized
    fun clearAll() {
        sessions.clear()
    }

    companion object {
        const val DEFAULT_MAX_TURNS = 20
    }
}

data class ConversationTurn(
    val role: ConversationRole,
    val text: String,
    val timestampMillis: Long
)

enum class ConversationRole {
    USER,
    ASSISTANT,
    SYSTEM
}
