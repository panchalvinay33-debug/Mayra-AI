package ai.mayra.app.core.intelligence

import java.time.Instant
import java.util.UUID

enum class ConversationRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

data class ConversationMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ConversationRole,
    val content: String,
    val createdAt: Instant = Instant.now(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(content.isNotBlank()) { "Conversation message content cannot be blank." }
    }
}

data class ConversationContextSnapshot(
    val sessionId: String,
    val messages: List<ConversationMessage>,
    val estimatedCharacters: Int
)

/**
 * Maintains a deterministic, bounded conversation window for one or more sessions.
 * Oldest non-system messages are evicted first while system instructions are retained.
 */
class ConversationContextManager(
    private val maxMessages: Int = 40,
    private val maxCharacters: Int = 16_000
) {
    private val sessions = linkedMapOf<String, MutableList<ConversationMessage>>()

    init {
        require(maxMessages > 0) { "Maximum message count must be positive." }
        require(maxCharacters > 0) { "Maximum character count must be positive." }
    }

    @Synchronized
    fun append(sessionId: String, message: ConversationMessage): ConversationContextSnapshot {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
        val messages = sessions.getOrPut(sessionId) { mutableListOf() }
        messages += message
        trim(messages)
        return snapshotOf(sessionId, messages)
    }

    @Synchronized
    fun replace(sessionId: String, messages: List<ConversationMessage>): ConversationContextSnapshot {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
        val mutable = messages.toMutableList()
        trim(mutable)
        sessions[sessionId] = mutable
        return snapshotOf(sessionId, mutable)
    }

    @Synchronized
    fun snapshot(sessionId: String): ConversationContextSnapshot =
        snapshotOf(sessionId, sessions[sessionId].orEmpty())

    @Synchronized
    fun clear(sessionId: String): Boolean = sessions.remove(sessionId) != null

    @Synchronized
    fun activeSessionIds(): Set<String> = sessions.keys.toSet()

    private fun trim(messages: MutableList<ConversationMessage>) {
        while (messages.size > maxMessages || messages.sumOf { it.content.length } > maxCharacters) {
            val removableIndex = messages.indexOfFirst { it.role != ConversationRole.SYSTEM }
            if (removableIndex >= 0) {
                messages.removeAt(removableIndex)
            } else {
                messages.removeAt(0)
            }
        }
    }

    private fun snapshotOf(
        sessionId: String,
        messages: List<ConversationMessage>
    ): ConversationContextSnapshot = ConversationContextSnapshot(
        sessionId = sessionId,
        messages = messages.toList(),
        estimatedCharacters = messages.sumOf { it.content.length }
    )
}
