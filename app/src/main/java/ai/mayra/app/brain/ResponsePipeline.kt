package ai.mayra.app.brain

/** Applies final response normalization before a reply reaches memory or UI. */
class ResponsePipeline(
    private val maximumCharacters: Int = 12_000
) {
    init {
        require(maximumCharacters > 0) { "maximumCharacters must be positive" }
    }

    fun process(response: AIResponse): AIResponse {
        if (!response.isSuccess) return response

        val normalized = response.text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
            .take(maximumCharacters)

        if (normalized.isEmpty()) {
            return response.copy(
                text = "Mayra received an empty response. Please try again.",
                isSuccess = false,
                errorMessage = "Provider returned empty text"
            )
        }

        return response.copy(text = normalized)
    }
}
