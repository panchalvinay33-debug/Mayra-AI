package ai.mayra.app.brain

import ai.mayra.app.memory.ConversationMemory
import ai.mayra.app.memory.MemoryEntry
import ai.mayra.app.skills.SkillRegistry

/** Coordinates local skills, short-term memory, and provider switching outside UI code. */
class AIManager(
    private var provider: AIProvider = DummyAIProvider(),
    private val memory: ConversationMemory = ConversationMemory(),
    private val skills: SkillRegistry = SkillRegistry()
) {
    fun setProvider(newProvider: AIProvider) {
        provider = newProvider
    }

    suspend fun replyTo(
        message: String,
        conversationId: String = "default",
        systemPrompt: String? = null
    ): AIResponse {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) {
            return AIResponse(
                text = "Please enter a message.",
                provider = "local",
                isSuccess = false,
                errorMessage = "Blank message"
            )
        }

        val localSkill = skills.find(cleanMessage)
        if (localSkill != null) {
            val result = localSkill.execute(cleanMessage)
            if (result.metadata["action"] == "clear_memory") {
                memory.clear(conversationId)
            } else {
                memory.remember(conversationId, MemoryEntry(MemoryEntry.Role.USER, cleanMessage))
                memory.remember(conversationId, MemoryEntry(MemoryEntry.Role.MAYRA, result.text))
            }
            return AIResponse(
                text = result.text,
                provider = "skill:${localSkill.id}",
                isSuccess = result.isSuccess,
                errorMessage = if (result.isSuccess) null else result.text
            )
        }

        val priorContext = memory.asPromptContext(conversationId)
        memory.remember(conversationId, MemoryEntry(MemoryEntry.Role.USER, cleanMessage))

        return runCatching {
            provider.generate(
                AIRequest(
                    message = cleanMessage,
                    conversationId = conversationId,
                    systemPrompt = buildSystemPrompt(systemPrompt, priorContext),
                    metadata = mapOf("memoryEntries" to memory.recent(conversationId).size.toString())
                )
            )
        }.fold(
            onSuccess = { response ->
                if (response.isSuccess) {
                    memory.remember(conversationId, MemoryEntry(MemoryEntry.Role.MAYRA, response.text))
                }
                response
            },
            onFailure = { error ->
                AIResponse(
                    text = "Mayra could not complete that request.",
                    provider = provider.name,
                    isSuccess = false,
                    errorMessage = error.message
                )
            }
        )
    }

    fun clearConversation(conversationId: String = "default") {
        memory.clear(conversationId)
    }

    private fun buildSystemPrompt(basePrompt: String?, context: String): String? {
        if (basePrompt.isNullOrBlank() && context.isBlank()) return null
        return buildString {
            if (!basePrompt.isNullOrBlank()) append(basePrompt.trim())
            if (context.isNotBlank()) {
                if (isNotEmpty()) append("\n\n")
                append("Recent conversation:\n")
                append(context)
            }
        }
    }
}
