package ai.mayra.app.core.memory

import ai.mayra.app.data.local.ConversationEntity
import ai.mayra.app.data.local.MemoryEntity
import ai.mayra.app.data.repository.ConversationRepository
import ai.mayra.app.data.repository.MemoryRepository
import java.util.UUID

class MayraMemoryManager(
    private val memoryRepository: MemoryRepository,
    private val conversationRepository: ConversationRepository,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun remember(
        content: String,
        category: String = CATEGORY_GENERAL,
        importance: Int = DEFAULT_IMPORTANCE,
        sourceConversationId: Long? = null
    ): Long {
        val normalizedContent = content.trim()
        require(normalizedContent.isNotEmpty()) { "Memory content cannot be blank." }

        val now = clock()
        return memoryRepository.remember(
            MemoryEntity(
                category = category.trim().ifBlank { CATEGORY_GENERAL },
                content = normalizedContent,
                importance = importance.coerceIn(MIN_IMPORTANCE, MAX_IMPORTANCE),
                sourceConversationId = sourceConversationId,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun recall(query: String, limit: Int = DEFAULT_RECALL_LIMIT): List<MemoryEntity> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        return memoryRepository.find(normalizedQuery, limit.coerceIn(1, MAX_RECALL_LIMIT))
    }

    suspend fun appendUserMessage(sessionId: String, message: String): Long =
        appendMessage(sessionId, ROLE_USER, message)

    suspend fun appendAssistantMessage(sessionId: String, message: String): Long =
        appendMessage(sessionId, ROLE_ASSISTANT, message)

    suspend fun recentConversation(
        sessionId: String,
        limit: Int = DEFAULT_CONVERSATION_LIMIT
    ): List<ConversationEntity> = conversationRepository.recent(
        sessionId = sessionId,
        limit = limit.coerceIn(1, MAX_CONVERSATION_LIMIT)
    )

    suspend fun clearConversation(sessionId: String): Int =
        conversationRepository.clear(sessionId.trim())

    fun createSessionId(): String = UUID.randomUUID().toString()

    private suspend fun appendMessage(sessionId: String, role: String, message: String): Long {
        val normalizedSessionId = sessionId.trim()
        val normalizedMessage = message.trim()
        require(normalizedSessionId.isNotEmpty()) { "Session id cannot be blank." }
        require(normalizedMessage.isNotEmpty()) { "Message cannot be blank." }

        return conversationRepository.append(
            ConversationEntity(
                sessionId = normalizedSessionId,
                role = role,
                message = normalizedMessage,
                createdAt = clock()
            )
        )
    }

    companion object {
        const val CATEGORY_GENERAL = "general"
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val MIN_IMPORTANCE = 0
        const val MAX_IMPORTANCE = 10
        const val DEFAULT_IMPORTANCE = 5
        const val DEFAULT_RECALL_LIMIT = 10
        const val MAX_RECALL_LIMIT = 50
        const val DEFAULT_CONVERSATION_LIMIT = 30
        const val MAX_CONVERSATION_LIMIT = 100
    }
}
