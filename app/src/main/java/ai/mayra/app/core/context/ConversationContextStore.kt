package ai.mayra.app.core.context

import java.util.ArrayDeque

enum class ConversationRole { USER, ASSISTANT, SYSTEM }

data class ConversationTurn(
    val id: Long,
    val role: ConversationRole,
    val text: String,
    val createdAt: Long
)

data class ConversationContextSnapshot(
    val turns: List<ConversationTurn>,
    val totalAcceptedTurns: Long,
    val droppedTurns: Long
)

class ConversationContextStore(
    private val capacity: Int = 24,
    private val maxTextLength: Int = 4000
) {
    init {
        require(capacity > 0)
        require(maxTextLength > 0)
    }

    private val turns = ArrayDeque<ConversationTurn>()
    private var nextId = 1L
    private var accepted = 0L
    private var dropped = 0L

    @Synchronized
    fun append(role: ConversationRole, text: String, createdAt: Long): ConversationTurn {
        val clean = text.trim().replace(Regex("\\s+"), " ").take(maxTextLength)
        require(clean.isNotBlank())

        turns.peekLast()?.let { latest ->
            if (latest.role == role && latest.text == clean) return latest
        }

        if (turns.size == capacity) {
            turns.removeFirst()
            dropped += 1
        }

        val turn = ConversationTurn(nextId++, role, clean, createdAt)
        turns.addLast(turn)
        accepted += 1
        return turn
    }

    @Synchronized
    fun recent(limit: Int = capacity): List<ConversationTurn> {
        require(limit >= 0)
        return turns.toList().takeLast(limit)
    }

    @Synchronized
    fun search(query: String, limit: Int = 5): List<ConversationTurn> {
        require(limit >= 0)
        val clean = query.trim().lowercase()
        if (clean.isBlank()) return emptyList()
        return turns.filter { it.text.lowercase().contains(clean) }.takeLast(limit).reversed()
    }

    @Synchronized
    fun clear(): Int {
        val count = turns.size
        turns.clear()
        return count
    }

    @Synchronized
    fun snapshot() = ConversationContextSnapshot(turns.toList(), accepted, dropped)
}
