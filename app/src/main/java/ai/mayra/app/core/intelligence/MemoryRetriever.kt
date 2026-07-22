package ai.mayra.app.core.intelligence

/** Applies prompt-focused filtering and deterministic ranking on top of a MemoryStore. */
class MemoryRetriever(
    private val store: MemoryStore
) {
    suspend fun retrieve(
        text: String,
        tags: Set<String> = emptySet(),
        limit: Int = 8,
        minimumImportance: Int = 0
    ): List<MemoryRecord> {
        require(text.isNotBlank()) { "Memory retrieval text cannot be blank." }
        require(limit > 0) { "Memory retrieval limit must be positive." }
        require(minimumImportance in 0..100) { "Minimum importance must be between 0 and 100." }

        return store.search(
            MemoryQuery(
                text = text.trim(),
                tags = tags.map(String::trim).filter(String::isNotBlank).toSet(),
                limit = (limit * 3).coerceAtLeast(limit)
            )
        )
            .asSequence()
            .filter { it.importance >= minimumImportance }
            .distinctBy { it.id }
            .sortedWith(
                compareByDescending<MemoryRecord> { it.importance }
                    .thenByDescending { it.accessCount }
                    .thenByDescending { it.createdAt }
            )
            .take(limit)
            .toList()
    }
}