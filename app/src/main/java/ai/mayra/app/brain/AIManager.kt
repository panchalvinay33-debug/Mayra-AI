package ai.mayra.app.brain

/** Coordinates requests and keeps provider switching out of UI code. */
class AIManager(
    private var provider: AIProvider = DummyAIProvider()
) {
    fun setProvider(newProvider: AIProvider) {
        provider = newProvider
    }

    suspend fun replyTo(
        message: String,
        conversationId: String = "default",
        systemPrompt: String? = null
    ): AIResponse {
        return runCatching {
            provider.generate(
                AIRequest(
                    message = message,
                    conversationId = conversationId,
                    systemPrompt = systemPrompt
                )
            )
        }.getOrElse { error ->
            AIResponse(
                text = "Mayra could not complete that request.",
                provider = provider.name,
                isSuccess = false,
                errorMessage = error.message
            )
        }
    }
}
