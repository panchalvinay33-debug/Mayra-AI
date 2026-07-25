package ai.mayra.app.ai

/** Pure validation and redaction boundary for online AI configuration and diagnostics. */
object AiProviderSafetyPolicy {
    const val MAX_MODEL_LENGTH = 120
    const val MAX_API_KEY_LENGTH = 512
    const val MAX_USER_MESSAGE_LENGTH = 8_000
    const val MAX_CONTEXT_MESSAGE_LENGTH = 8_000
    const val MAX_CONNECTION_MESSAGE_LENGTH = 160

    private val modelPattern = Regex("^[A-Za-z0-9][A-Za-z0-9._:-]{0,119}$")
    private val apiKeyPattern = Regex("^sk-[A-Za-z0-9_-]{8,509}$")
    private val bearerPattern = Regex("(?i)Bearer\\s+[A-Za-z0-9._-]+")
    private val openAiKeyPattern = Regex("sk-[A-Za-z0-9_-]{8,}")
    private val controlCharacters = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")

    fun normalizeModel(value: String): String = value
        .trim()
        .replace(controlCharacters, "")
        .take(MAX_MODEL_LENGTH)

    fun validateModel(value: String): String? {
        val normalized = normalizeModel(value)
        return when {
            normalized.isBlank() -> "Enter an OpenAI model name."
            !modelPattern.matches(normalized) -> "Model name contains unsupported characters."
            else -> null
        }
    }

    fun normalizeApiKey(value: String): String = value.trim().take(MAX_API_KEY_LENGTH)

    fun validateNewApiKey(value: String): String? {
        val normalized = normalizeApiKey(value)
        return when {
            normalized.isBlank() -> "Enter an OpenAI API key."
            !apiKeyPattern.matches(normalized) -> "OpenAI API key format is not valid."
            else -> null
        }
    }

    fun sanitizeConnectionMessage(value: String?): String = value.orEmpty()
        .replace(bearerPattern, "Bearer [redacted]")
        .replace(openAiKeyPattern, "[redacted-key]")
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(controlCharacters, "")
        .trim()
        .take(MAX_CONNECTION_MESSAGE_LENGTH)
        .ifBlank { "Connection test failed." }

    fun boundUserMessage(value: String): String = value.trim().take(MAX_USER_MESSAGE_LENGTH)

    fun boundContextMessage(value: String): String = value.trim().take(MAX_CONTEXT_MESSAGE_LENGTH)

    fun requireHttpsEndpoint(endpoint: String) {
        require(endpoint.startsWith("https://", ignoreCase = true)) {
            "Online AI endpoint must use HTTPS."
        }
    }
}
