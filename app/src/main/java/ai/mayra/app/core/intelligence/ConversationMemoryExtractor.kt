package ai.mayra.app.core.intelligence

/** Converts conversation turns into conservative durable-memory candidates. */
class ConversationMemoryExtractor(
    private val maxCandidates: Int = 6
) {
    init {
        require(maxCandidates > 0) { "Maximum candidates must be positive." }
    }

    fun extract(messages: List<ConversationMessage>): List<MemoryCandidate> = messages
        .asSequence()
        .filter { it.role == ConversationRole.USER }
        .map { it.content.trim().replace(Regex("\\s+"), " ") }
        .filter(String::isNotBlank)
        .filter(::looksDurable)
        .distinctBy { normalize(it) }
        .takeLast(maxCandidates)
        .map { content ->
            MemoryCandidate(
                content = content,
                tags = inferTags(content)
            )
        }
        .toList()

    private fun looksDurable(content: String): Boolean {
        val value = content.lowercase()
        val durableSignals = listOf(
            "my name", "mera naam", "i prefer", "mujhe pasand", "i like", "i dislike",
            "remember", "yaad rakh", "my goal", "mera goal", "i live", "main rehta",
            "i am allergic", "mujhe allergy", "my birthday", "mera janamdin", "always",
            "hamesha", "never", "kabhi nahi"
        )
        val transientSignals = listOf("today", "aaj", "tomorrow", "kal", "right now", "abhi")
        return durableSignals.any(value::contains) && transientSignals.none(value::contains)
    }

    private fun inferTags(content: String): Set<String> {
        val value = content.lowercase()
        return buildSet {
            if (listOf("prefer", "pasand", "like", "dislike").any(value::contains)) add("preference")
            if (listOf("goal", "target", "aim").any(value::contains)) add("goal")
            if (listOf("name", "naam", "birthday", "janamdin").any(value::contains)) add("identity")
            if (listOf("allergy", "medical", "health").any(value::contains)) add("health")
            if (listOf("live", "rehta", "location").any(value::contains)) add("location")
        }
    }

    private fun normalize(content: String): String = content
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
}
