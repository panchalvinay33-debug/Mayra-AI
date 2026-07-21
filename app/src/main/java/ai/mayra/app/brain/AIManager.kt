package ai.mayra.app.brain

import ai.mayra.app.memory.ConversationMemory
import ai.mayra.app.memory.MemoryEntry
import ai.mayra.app.memory.PersistentMemoryService
import ai.mayra.app.skills.SkillRegistry

/** Coordinates local skills, short-term memory, persistent memory, and providers. */
class AIManager(
    private var provider: AIProvider = DummyAIProvider(),
    private val memory: ConversationMemory = ConversationMemory(),
    private val skills: SkillRegistry = SkillRegistry(),
    private val persistentMemory: PersistentMemoryService? = null,
    private val promptBuilder: PromptBuilder = PromptBuilder(),
    private val responsePipeline: ResponsePipeline = ResponsePipeline()
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
            return responsePipeline.process(
                AIResponse(
                    text = result.text,
                    provider = "skill:${localSkill.id}",
                    isSuccess = result.isSuccess,
                    errorMessage = if (result.isSuccess) null else result.text
                )
            )
        }

        val recentConversation = memory.asPromptContext(conversationId)
        val longTermContext = persistentMemory
            ?.contextFor(cleanMessage)
            .orEmpty()
        memory.remember(conversationId, MemoryEntry(MemoryEntry.Role.USER, cleanMessage))

        return runCatching {
            provider.generate(
                AIRequest(
                    message = cleanMessage,
                    conversationId = conversationId,
                    systemPrompt = promptBuilder.buildSystemPrompt(
                        basePrompt = systemPrompt,
                        longTermContext = longTermContext,
                        recentConversation = recentConversation
                    ),
                    metadata = mapOf(
                        "memoryEntries" to memory.recent(conversationId).size.toString(),
                        "hasPersistentMemory" to longTermContext.isNotBlank().toString()
                    )
                )
            )
        }.fold(
            onSuccess = { rawResponse ->
                val response = responsePipeline.process(rawResponse)
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

    suspend fun rememberUserFact(
        content: String,
        category: String = "general",
        importance: Int = 3
    ): Long? = persistentMemory?.rememberUserFact(
        content = content,
        category = category,
        importance = importance
    )

    fun clearConversation(conversationId: String = "default") {
        memory.clear(conversationId)
    }
}
