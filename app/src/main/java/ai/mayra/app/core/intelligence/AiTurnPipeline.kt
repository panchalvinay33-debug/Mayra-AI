package ai.mayra.app.core.intelligence

/** Coordinates retrieval, prompt assembly, provider routing, and conversation persistence. */
class AiTurnPipeline(
    private val sessions: ConversationSessionManager,
    private val memoryRetriever: MemoryRetriever,
    private val promptBuilder: PromptBuilder,
    private val providerRouter: LlmProviderRouter,
    private val defaultSystemInstructions: List<String> = listOf(
        "You are Mayra, a helpful and reliable mobile assistant.",
        "Be concise, truthful, and ask for clarification only when required."
    )
) {
    suspend fun execute(
        sessionId: String,
        userInput: String,
        memoryTags: Set<String> = emptySet(),
        preferredProviderId: String? = null,
        options: LlmGenerationOptions = LlmGenerationOptions()
    ): LlmResponse {
        require(userInput.isNotBlank()) { "User input cannot be blank." }

        val session = sessions.get(sessionId)
            ?: throw NoSuchElementException("Unknown conversation session: $sessionId")
        check(session.status == ConversationSessionStatus.ACTIVE) {
            "Conversation session is closed: $sessionId"
        }

        val contextBeforeTurn = sessions.snapshot(sessionId).messages
        val memories = memoryRetriever.retrieve(
            text = userInput,
            tags = memoryTags,
            limit = 8
        )
        val prompt = promptBuilder.build(
            PromptRequest(
                sessionId = sessionId,
                userInput = userInput,
                systemInstructions = defaultSystemInstructions,
                context = contextBeforeTurn,
                memories = memories,
                metadata = mapOf("sessionTitle" to session.title)
            )
        )

        sessions.append(
            sessionId,
            ConversationMessage(role = ConversationRole.USER, content = userInput.trim())
        )

        return try {
            val response = providerRouter.generate(
                request = LlmRequest(prompt = prompt, options = options),
                preferredProviderId = preferredProviderId
            )
            sessions.append(
                sessionId,
                ConversationMessage(
                    role = ConversationRole.ASSISTANT,
                    content = response.content,
                    metadata = mapOf("providerId" to response.providerId)
                )
            )
            response
        } catch (error: Throwable) {
            sessions.append(
                sessionId,
                ConversationMessage(
                    role = ConversationRole.TOOL,
                    content = "Generation failed: ${error.message ?: error::class.simpleName.orEmpty()}",
                    metadata = mapOf("error" to "true")
                )
            )
            throw error
        }
    }
}