package ai.mayra.app.memory

/** Builds a compact prompt section from persistent memories. */
class MemoryContextBuilder(
    private val maxItems: Int = DEFAULT_MAX_ITEMS,
    private val maxCharacters: Int = DEFAULT_MAX_CHARACTERS
) {
    init {
        require(maxItems > 0) { "maxItems must be greater than zero." }
        require(maxCharacters > 0) { "maxCharacters must be greater than zero." }
    }

    fun build(memories: List<MemoryEntity>): String {
        if (memories.isEmpty()) return ""

        val ranked = memories
            .asSequence()
            .filter { it.content.isNotBlank() }
            .sortedWith(
                compareByDescending<MemoryEntity> { it.importance }
                    .thenByDescending { it.updatedAt }
            )
            .take(maxItems)
            .toList()

        if (ranked.isEmpty()) return ""

        val header = "Useful user memories:\n"
        val result = StringBuilder(header)

        for (memory in ranked) {
            val line = "- [${memory.category}] ${memory.content.trim()}\n"
            if (result.length + line.length > maxCharacters) break
            result.append(line)
        }

        return if (result.length == header.length) "" else result.toString().trimEnd()
    }

    companion object {
        const val DEFAULT_MAX_ITEMS = 8
        const val DEFAULT_MAX_CHARACTERS = 1_500
    }
}
