package ai.mayra.app.core.intelligence

/** A provider-neutral prompt message. */
data class PromptMessage(
    val role: ConversationRole,
    val content: String,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(content.isNotBlank()) { "Prompt message content cannot be blank." }
    }
}

data class PromptBudget(
    val maxCharacters: Int = 24_000,
    val reservedResponseCharacters: Int = 4_000,
    val maxMemoryItems: Int = 8
) {
    init {
        require(maxCharacters > 0) { "Maximum prompt characters must be positive." }
        require(reservedResponseCharacters >= 0) { "Reserved response characters cannot be negative." }
        require(reservedResponseCharacters < maxCharacters) {
            "Reserved response characters must be smaller than the total budget."
        }
        require(maxMemoryItems >= 0) { "Maximum memory items cannot be negative." }
    }

    val inputCharacterLimit: Int get() = maxCharacters - reservedResponseCharacters
}

data class PromptRequest(
    val sessionId: String,
    val userInput: String,
    val systemInstructions: List<String> = emptyList(),
    val context: List<ConversationMessage> = emptyList(),
    val memories: List<MemoryRecord> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val budget: PromptBudget = PromptBudget()
) {
    init {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
        require(userInput.isNotBlank()) { "User input cannot be blank." }
    }
}

data class AssembledPrompt(
    val sessionId: String,
    val messages: List<PromptMessage>,
    val includedMemoryIds: List<String>,
    val estimatedCharacters: Int,
    val truncated: Boolean,
    val metadata: Map<String, String>
)