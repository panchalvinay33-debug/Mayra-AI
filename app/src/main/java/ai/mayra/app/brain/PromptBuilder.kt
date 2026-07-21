package ai.mayra.app.brain

/** Builds provider-ready prompts from system instructions and conversation context. */
class PromptBuilder {

    fun buildSystemPrompt(
        basePrompt: String?,
        longTermContext: String,
        recentConversation: String
    ): String? {
        val sections = buildList {
            basePrompt
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(::add)

            longTermContext
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let { add("Relevant long-term memory:\n$it") }

            recentConversation
                .trim()
                .takeIf(String::isNotEmpty)
                ?.let { add("Recent conversation:\n$it") }
        }

        return sections
            .joinToString(separator = "\n\n")
            .takeIf(String::isNotEmpty)
    }
}
