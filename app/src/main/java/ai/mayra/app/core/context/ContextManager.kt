package ai.mayra.app.core.context

import ai.mayra.app.core.memory.MayraMemoryManager
import ai.mayra.app.data.local.ConversationEntity
import ai.mayra.app.data.local.MemoryEntity

data class ConversationContext(
    val sessionId: String,
    val messages: List<ConversationEntity>,
    val recalledMemories: List<MemoryEntity>
) {
    val lastUserMessage: String?
        get() = messages.lastOrNull { it.role == MayraMemoryManager.ROLE_USER }?.message

    val lastAssistantMessage: String?
        get() = messages.lastOrNull { it.role == MayraMemoryManager.ROLE_ASSISTANT }?.message
}

class ContextManager(
    private val memoryManager: MayraMemoryManager
) {
    suspend fun build(
        sessionId: String,
        memoryQuery: String? = null,
        conversationLimit: Int = DEFAULT_CONVERSATION_LIMIT,
        memoryLimit: Int = DEFAULT_MEMORY_LIMIT
    ): ConversationContext {
        val normalizedSessionId = sessionId.trim()
        require(normalizedSessionId.isNotEmpty()) { "Session id cannot be blank." }

        val messages = memoryManager.recentConversation(
            sessionId = normalizedSessionId,
            limit = conversationLimit.coerceIn(1, MAX_CONVERSATION_LIMIT)
        )

        val effectiveQuery = memoryQuery
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: messages.lastOrNull { it.role == MayraMemoryManager.ROLE_USER }?.message

        val memories = if (effectiveQuery.isNullOrBlank()) {
            emptyList()
        } else {
            memoryManager.recall(
                query = effectiveQuery,
                limit = memoryLimit.coerceIn(1, MAX_MEMORY_LIMIT)
            )
        }

        return ConversationContext(
            sessionId = normalizedSessionId,
            messages = messages,
            recalledMemories = memories
        )
    }

    suspend fun recordExchange(
        sessionId: String,
        userMessage: String,
        assistantMessage: String
    ): Pair<Long, Long> {
        val userId = memoryManager.appendUserMessage(sessionId, userMessage)
        val assistantId = memoryManager.appendAssistantMessage(sessionId, assistantMessage)
        return userId to assistantId
    }

    companion object {
        const val DEFAULT_CONVERSATION_LIMIT = 20
        const val MAX_CONVERSATION_LIMIT = 100
        const val DEFAULT_MEMORY_LIMIT = 8
        const val MAX_MEMORY_LIMIT = 50
    }
}
