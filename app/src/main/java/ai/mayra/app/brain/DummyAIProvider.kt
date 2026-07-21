package ai.mayra.app.brain

/** Development provider used until a production backend is configured. */
class DummyAIProvider : AIProvider {
    override val name: String = "dummy"

    override suspend fun generate(request: AIRequest): AIResponse {
        val cleaned = request.message.trim()
        val reply = if (cleaned.isBlank()) {
            "Please type a message so I can help."
        } else {
            "Mayra received: $cleaned"
        }
        return AIResponse(text = reply, provider = name)
    }
}
