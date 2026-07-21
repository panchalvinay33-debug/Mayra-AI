package ai.mayra.app.memory

import kotlinx.coroutines.flow.Flow

class MemoryRepository(
    private val memoryDao: MemoryDao
) {
    fun observeAll(): Flow<List<MemoryEntity>> = memoryDao.observeAll()

    suspend fun remember(
        content: String,
        category: String = MemoryEntity.CATEGORY_GENERAL,
        source: String = MemoryEntity.SOURCE_CONVERSATION,
        importance: Int = MemoryEntity.DEFAULT_IMPORTANCE
    ): Long {
        val normalizedContent = content.trim()
        require(normalizedContent.isNotEmpty()) { "Memory content cannot be blank." }

        return memoryDao.insert(
            MemoryEntity(
                content = normalizedContent,
                category = category.trim().ifBlank { MemoryEntity.CATEGORY_GENERAL },
                source = source.trim().ifBlank { MemoryEntity.SOURCE_CONVERSATION },
                importance = importance.coerceIn(
                    MemoryEntity.MIN_IMPORTANCE,
                    MemoryEntity.MAX_IMPORTANCE
                )
            )
        )
    }

    suspend fun getById(id: Long): MemoryEntity? = memoryDao.getById(id)

    suspend fun getRecent(limit: Int = DEFAULT_RESULT_LIMIT): List<MemoryEntity> =
        memoryDao.getRecent(limit.coerceIn(1, MAX_RESULT_LIMIT))

    suspend fun search(
        query: String,
        limit: Int = DEFAULT_RESULT_LIMIT
    ): List<MemoryEntity> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()

        return memoryDao.search(
            query = normalizedQuery,
            limit = limit.coerceIn(1, MAX_RESULT_LIMIT)
        )
    }

    suspend fun update(memory: MemoryEntity) {
        memoryDao.update(
            memory.copy(
                content = memory.content.trim(),
                category = memory.category.trim().ifBlank { MemoryEntity.CATEGORY_GENERAL },
                source = memory.source.trim().ifBlank { MemoryEntity.SOURCE_CONVERSATION },
                importance = memory.importance.coerceIn(
                    MemoryEntity.MIN_IMPORTANCE,
                    MemoryEntity.MAX_IMPORTANCE
                ),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun forget(id: Long) {
        memoryDao.deleteById(id)
    }

    suspend fun clearAll() {
        memoryDao.clearAll()
    }

    companion object {
        const val DEFAULT_RESULT_LIMIT = 20
        const val MAX_RESULT_LIMIT = 100
    }
}
