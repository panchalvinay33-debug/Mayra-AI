package ai.mayra.app.brain

/** Input passed to an AI provider. */
data class AIRequest(
    val message: String,
    val conversationId: String = "default",
    val systemPrompt: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
