package ai.mayra.app.data.repository

import ai.mayra.app.data.local.ConversationDao
import ai.mayra.app.data.local.ConversationEntity
import ai.mayra.app.data.local.GoalDao
import ai.mayra.app.data.local.GoalEntity
import ai.mayra.app.data.local.MemoryDao
import ai.mayra.app.data.local.MemoryEntity
import ai.mayra.app.data.local.ReminderDao
import ai.mayra.app.data.local.ReminderEntity
import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val dao: MemoryDao) {
    fun observeAll(): Flow<List<MemoryEntity>> = dao.observeAll()
    suspend fun remember(memory: MemoryEntity): Long = dao.upsert(memory)
    suspend fun find(query: String, limit: Int = 25): List<MemoryEntity> = dao.search(query, limit)
    suspend fun getByCategory(category: String, limit: Int = 50): List<MemoryEntity> =
        dao.getByCategory(category, limit)
    suspend fun forget(memory: MemoryEntity) = dao.delete(memory)
    suspend fun prune(maxImportance: Int, olderThan: Long): Int = dao.prune(maxImportance, olderThan)
}

class ConversationRepository(private val dao: ConversationDao) {
    fun observeSession(sessionId: String): Flow<List<ConversationEntity>> = dao.observeSession(sessionId)
    suspend fun append(message: ConversationEntity): Long = dao.insert(message)
    suspend fun recent(sessionId: String, limit: Int = 30): List<ConversationEntity> =
        dao.recent(sessionId, limit).asReversed()
    suspend fun clear(sessionId: String): Int = dao.clearSession(sessionId)
    suspend fun deleteOlderThan(olderThan: Long): Int = dao.deleteOlderThan(olderThan)
}

class ReminderRepository(private val dao: ReminderDao) {
    fun observePending(): Flow<List<ReminderEntity>> = dao.observePending()
    suspend fun schedule(reminder: ReminderEntity): Long = dao.upsert(reminder)
    suspend fun get(id: Long): ReminderEntity? = dao.getById(id)
    suspend fun due(now: Long = System.currentTimeMillis()): List<ReminderEntity> = dao.due(now)
    suspend fun complete(id: Long): Boolean = dao.markCompleted(id) > 0
    suspend fun reschedule(id: Long, triggerAt: Long): Boolean = dao.reschedule(id, triggerAt) > 0
    suspend fun cancel(reminder: ReminderEntity) = dao.delete(reminder)
}

class GoalRepository(private val dao: GoalDao) {
    fun observeActive(): Flow<List<GoalEntity>> = dao.observeByStatus()
    suspend fun save(goal: GoalEntity): Long = dao.upsert(goal)
    suspend fun get(id: Long): GoalEntity? = dao.getById(id)
    suspend fun remove(goal: GoalEntity) = dao.delete(goal)
}
