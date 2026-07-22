package ai.mayra.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("SELECT * FROM memories ORDER BY importance DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories WHERE category = :category ORDER BY importance DESC, updatedAt DESC LIMIT :limit")
    suspend fun getByCategory(category: String, limit: Int = 50): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE content LIKE '%' || :query || '%' ORDER BY importance DESC, updatedAt DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 25): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE importance <= :maxImportance AND updatedAt < :olderThan")
    suspend fun prune(maxImportance: Int, olderThan: Long): Int
}

@Dao
interface ConversationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ConversationEntity): Long

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeSession(sessionId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(sessionId: String, limit: Int = 30): List<ConversationEntity>

    @Query("DELETE FROM conversations WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String): Int

    @Query("DELETE FROM conversations WHERE createdAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long): Int
}

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE completed = 0 ORDER BY triggerAt ASC")
    fun observePending(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE completed = 0 AND triggerAt <= :now ORDER BY triggerAt ASC")
    suspend fun due(now: Long): List<ReminderEntity>

    @Query("UPDATE reminders SET completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long): Int
}

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE status = :status ORDER BY priority DESC, updatedAt DESC")
    fun observeByStatus(status: String = "active"): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): GoalEntity?
}
