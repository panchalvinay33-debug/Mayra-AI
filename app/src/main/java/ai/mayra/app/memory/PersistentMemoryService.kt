package ai.mayra.app.memory

/** Coordinates persistent memory retrieval and prompt formatting for the AI layer. */
class PersistentMemoryService(
    private val repository: MemoryRepository,
    private val contextBuilder: MemoryContextBuilder = MemoryContextBuilder()
) {
    suspend fun contextFor(
        message: String,
        searchLimit: Int = DEFAULT_SEARCH_LIMIT
    ): String {
        val cleanMessage = message.trim()
        if (cleanMessage.isEmpty()) return ""

        val matched = repository.search(cleanMessage, searchLimit)
        val memories = if (matched.isNotEmpty()) {
            matched
        } else {
            repository.getRecent(searchLimit)
        }

        return contextBuilder.build(memories)
    }

    suspend fun rememberUserFact(
        content: String,
        category: String = MemoryEntity.CATEGORY_GENERAL,
        importance: Int = MemoryEntity.DEFAULT_IMPORTANCE
    ): Long = repository.remember(
        content = content,
        category = category,
        source = SOURCE_EXPLICIT_USER_MEMORY,
        importance = importance
    )

    suspend fun forget(memoryId: Long) {
        require(memoryId > 0) { "memoryId must be greater than zero." }
        repository.forget(memoryId)
    }

    companion object {
        const val DEFAULT_SEARCH_LIMIT = 12
        const val SOURCE_EXPLICIT_USER_MEMORY = "explicit_user_memory"
    }
}
