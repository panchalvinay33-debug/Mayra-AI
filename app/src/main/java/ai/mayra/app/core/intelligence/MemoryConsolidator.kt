package ai.mayra.app.core.intelligence

data class MemoryConsolidationPolicy(
    val minimumImportance: Int = 45,
    val duplicateThreshold: Double = 0.82,
    val maxCandidatesPerTurn: Int = 6
) {
    init {
        require(minimumImportance in 0..100) { "Minimum importance must be between 0 and 100." }
        require(duplicateThreshold in 0.0..1.0) { "Duplicate threshold must be between 0 and 1." }
        require(maxCandidatesPerTurn > 0) { "Maximum candidates must be positive." }
    }
}

data class MemoryCandidate(
    val content: String,
    val tags: Set<String> = emptySet()
) {
    init {
        require(content.isNotBlank()) { "Memory candidate cannot be blank." }
    }
}

data class MemoryConsolidationResult(
    val saved: List<MemoryRecord>,
    val skippedLowImportance: List<MemoryCandidate>,
    val skippedDuplicates: List<MemoryCandidate>
)

class MemoryConsolidator(
    private val store: MemoryStore,
    private val scorer: MemoryImportanceScorer = MemoryImportanceScorer(),
    private val policy: MemoryConsolidationPolicy = MemoryConsolidationPolicy()
) {
    suspend fun consolidate(candidates: List<MemoryCandidate>): MemoryConsolidationResult {
        val saved = mutableListOf<MemoryRecord>()
        val lowImportance = mutableListOf<MemoryCandidate>()
        val duplicates = mutableListOf<MemoryCandidate>()
        val existing = store.all().toMutableList()

        candidates
            .take(policy.maxCandidatesPerTurn)
            .forEach { candidate ->
                val importance = scorer.score(candidate.content, candidate.tags)
                if (importance < policy.minimumImportance) {
                    lowImportance += candidate
                    return@forEach
                }

                val duplicate = existing.any { similarity(it.content, candidate.content) >= policy.duplicateThreshold }
                if (duplicate) {
                    duplicates += candidate
                    return@forEach
                }

                val record = MemoryRecord(
                    content = candidate.content.trim(),
                    tags = candidate.tags.map(String::trim).filter(String::isNotBlank).toSet(),
                    importance = importance
                )
                store.save(record)
                existing += record
                saved += record
            }

        return MemoryConsolidationResult(saved, lowImportance, duplicates)
    }

    private fun similarity(left: String, right: String): Double {
        val leftTokens = tokenize(left)
        val rightTokens = tokenize(right)
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) return 0.0
        val intersection = leftTokens.intersect(rightTokens).size.toDouble()
        val union = leftTokens.union(rightTokens).size.toDouble()
        return intersection / union
    }

    private fun tokenize(value: String): Set<String> = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.length > 1 }
        .toSet()
}