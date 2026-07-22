package ai.mayra.app.core.intelligence

data class ResponseValidationPolicy(
    val minCharacters: Int = 1,
    val maxCharacters: Int = 8_000,
    val blockedPhrases: Set<String> = emptySet(),
    val requireTerminalPunctuation: Boolean = false
) {
    init {
        require(minCharacters >= 0) { "Minimum character count cannot be negative." }
        require(maxCharacters > 0) { "Maximum character count must be positive." }
        require(minCharacters <= maxCharacters) { "Minimum cannot exceed maximum." }
    }
}

data class ResponseValidationResult(
    val valid: Boolean,
    val normalizedContent: String,
    val violations: List<String>
)

class ResponseValidator(
    private val policy: ResponseValidationPolicy = ResponseValidationPolicy()
) {
    fun validate(content: String): ResponseValidationResult {
        val normalized = content.trim()
        val violations = mutableListOf<String>()

        if (normalized.length < policy.minCharacters) violations += "response_too_short"
        if (normalized.length > policy.maxCharacters) violations += "response_too_long"

        val lowered = normalized.lowercase()
        policy.blockedPhrases
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .filter { lowered.contains(it.lowercase()) }
            .forEach { violations += "blocked_phrase:$it" }

        if (policy.requireTerminalPunctuation && normalized.isNotEmpty() &&
            normalized.last() !in setOf('.', '!', '?', '।')) {
            violations += "missing_terminal_punctuation"
        }

        return ResponseValidationResult(
            valid = violations.isEmpty(),
            normalizedContent = normalized,
            violations = violations.distinct()
        )
    }

    fun requireValid(content: String): String {
        val result = validate(content)
        require(result.valid) { "Invalid LLM response: ${result.violations.joinToString()}" }
        return result.normalizedContent
    }
}
