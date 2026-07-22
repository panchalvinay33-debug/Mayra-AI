package ai.mayra.app.core.intelligence

import java.time.Instant

data class LlmGenerationOptions(
    val temperature: Double = 0.3,
    val maxOutputCharacters: Int = 4_000,
    val stopSequences: List<String> = emptyList()
) {
    init {
        require(temperature in 0.0..2.0) { "Temperature must be between 0 and 2." }
        require(maxOutputCharacters > 0) { "Maximum output characters must be positive." }
    }
}

data class LlmRequest(
    val prompt: AssembledPrompt,
    val options: LlmGenerationOptions = LlmGenerationOptions()
)

data class LlmResponse(
    val providerId: String,
    val content: String,
    val createdAt: Instant = Instant.now(),
    val finishReason: String = "stop",
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(providerId.isNotBlank()) { "Provider id cannot be blank." }
        require(content.isNotBlank()) { "LLM response content cannot be blank." }
    }
}

interface LlmProvider {
    val id: String
    val priority: Int get() = 0

    suspend fun isAvailable(): Boolean
    suspend fun generate(request: LlmRequest): LlmResponse
}