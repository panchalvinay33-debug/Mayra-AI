package ai.mayra.app.memory

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
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemoryEntity?

    @Query("SELECT * FROM memories ORDER BY importance DESC, updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<MemoryEntity>

    @Query(
        """
        SELECT * FROM memories
        WHERE content LIKE '%' || :query || '%'
           OR category LIKE '%' || :query || '%'
        ORDER BY importance DESC, updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun search(query: String, limit: Int): List<MemoryEntity>

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun clearAll()
}
