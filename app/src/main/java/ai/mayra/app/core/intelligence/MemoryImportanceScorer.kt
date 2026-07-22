package ai.mayra.app.core.intelligence

/** Scores candidate memories using explicit, inspectable heuristics. */
class MemoryImportanceScorer {
    fun score(content: String, tags: Set<String> = emptySet()): Int {
        val normalized = content.trim().lowercase()
        require(normalized.isNotBlank()) { "Memory content cannot be blank." }

        var score = 35
        if (normalized.length in 20..240) score += 10
        if (normalized.length > 500) score -= 10
        if (tags.isNotEmpty()) score += minOf(tags.size * 3, 12)

        val durableSignals = listOf(
            "prefer", "preference", "remember", "always", "never", "birthday",
            "name is", "lives in", "works at", "allergy", "medical", "goal"
        )
        score += durableSignals.count(normalized::contains) * 8

        val transientSignals = listOf(
            "today", "right now", "currently", "temporary", "this minute", "for now"
        )
        score -= transientSignals.count(normalized::contains) * 6

        return score.coerceIn(0, 100)
    }
}