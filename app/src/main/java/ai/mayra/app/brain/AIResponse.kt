package ai.mayra.app.brain

/** Result returned by an AI provider. */
data class AIResponse(
    val text: String,
    val provider: String,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)
