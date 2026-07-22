package ai.mayra.app.core.intelligence

data class ConversationSummaryPolicy(
    val maxCharacters: Int = 1_200,
    val maxPoints: Int = 8,
    val includeToolMessages: Boolean = false
) {
    init {
        require(maxCharacters > 0) { "Maximum summary characters must be positive." }
        require(maxPoints > 0) { "Maximum summary points must be positive." }
    }
}

data class ConversationSummary(
    val text: String,
    val sourceMessageIds: List<String>,
    val coveredMessages: Int,
    val truncated: Boolean
)

/** Creates a stable extractive summary without requiring a second model call. */
class ConversationSummarizer(
    private val policy: ConversationSummaryPolicy = ConversationSummaryPolicy()
) {
    fun summarize(messages: List<ConversationMessage>): ConversationSummary {
        val candidates = messages
            .filter { it.role != ConversationRole.SYSTEM }
            .filter { policy.includeToolMessages || it.role != ConversationRole.TOOL }
            .mapNotNull { message ->
                normalize(message.content).takeIf(String::isNotBlank)?.let { message to it }
            }

        val selected = mutableListOf<Pair<ConversationMessage, String>>()
        var used = 0
        var truncated = false

        for ((message, normalized) in candidates.asReversed()) {
            if (selected.size >= policy.maxPoints) {
                truncated = true
                continue
            }
            val prefix = when (message.role) {
                ConversationRole.USER -> "User: "
                ConversationRole.ASSISTANT -> "Mayra: "
                ConversationRole.TOOL -> "Tool: "
                ConversationRole.SYSTEM -> "System: "
            }
            val available = policy.maxCharacters - used - prefix.length - 2
            if (available <= 0) {
                truncated = true
                continue
            }
            val clipped = normalized.take(available)
            if (clipped.length < normalized.length) truncated = true
            selected += message to "$prefix$clipped"
            used += prefix.length + clipped.length + 2
        }

        val ordered = selected.asReversed()
        return ConversationSummary(
            text = ordered.joinToString(separator = "\n") { "- ${it.second}" },
            sourceMessageIds = ordered.map { it.first.id },
            coveredMessages = ordered.size,
            truncated = truncated || ordered.size < candidates.size
        )
    }

    private fun normalize(content: String): String =
        content.trim().replace(Regex("\\s+"), " ")
}