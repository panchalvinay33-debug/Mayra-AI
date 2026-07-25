package ai.mayra.app.memory

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Owner-visible memory categories. Sensitive secrets must never be stored here. */
enum class MayraMemoryKind { FACT, PREFERENCE, RELATIONSHIP, ROUTINE, PROJECT, DECISION, NOTE }

enum class MayraMemorySource { USER_CONFIRMED, USER_NOTE, CONVERSATION_SUMMARY, SYSTEM_SUGGESTION }

@Entity(
    tableName = "mayra_memories",
    indices = [Index("normalizedText"), Index("kind"), Index("updatedAt"), Index("isArchived")]
)
data class MayraMemoryEntity(
    @androidx.room.PrimaryKey val id: String = UUID.randomUUID().toString(),
    val kind: MayraMemoryKind,
    val title: String,
    val text: String,
    val normalizedText: String,
    val tags: String = "",
    val source: MayraMemorySource,
    val confidence: Float = 1f,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null
)

@Entity(tableName = "mayra_notes", indices = [Index("updatedAt"), Index("isArchived"), Index("isPinned")])
data class MayraNoteEntity(
    @androidx.room.PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val category: String = "Personal",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Dao
interface MayraMemoryDao {
    @Query("SELECT * FROM mayra_memories WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<MayraMemoryEntity>>

    @Query("SELECT * FROM mayra_memories WHERE isArchived = 0 AND (normalizedText LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC LIMIT :limit")
    suspend fun search(query: String, limit: Int = 20): List<MayraMemoryEntity>

    @Query("SELECT * FROM mayra_memories WHERE isArchived = 0 ORDER BY isPinned DESC, COALESCE(lastUsedAt, updatedAt) DESC LIMIT :limit")
    suspend fun recentRelevant(limit: Int = 12): List<MayraMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: MayraMemoryEntity)

    @Update suspend fun update(memory: MayraMemoryEntity)

    @Query("UPDATE mayra_memories SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE mayra_memories SET lastUsedAt = :now WHERE id IN (:ids)")
    suspend fun markUsed(ids: List<String>, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM mayra_memories WHERE isArchived = 1 AND updatedAt < :before")
    suspend fun purgeArchived(before: Long): Int

    @Query("SELECT COUNT(*) FROM mayra_memories WHERE isArchived = 0")
    suspend fun activeCount(): Int
}

@Dao
interface MayraNoteDao {
    @Query("SELECT * FROM mayra_notes WHERE isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeActive(): Flow<List<MayraNoteEntity>>

    @Query("SELECT * FROM mayra_notes WHERE isArchived = 0 AND (title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    suspend fun search(query: String): List<MayraNoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: MayraNoteEntity)

    @Query("UPDATE mayra_notes SET isArchived = 1, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long = System.currentTimeMillis())
}

@Database(entities = [MayraMemoryEntity::class, MayraNoteEntity::class], version = 1, exportSchema = true)
abstract class MayraPersonalDatabase : RoomDatabase() {
    abstract fun memoryDao(): MayraMemoryDao
    abstract fun noteDao(): MayraNoteDao

    companion object {
        @Volatile private var instance: MayraPersonalDatabase? = null
        fun get(context: Context): MayraPersonalDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MayraPersonalDatabase::class.java,
                "mayra_personal.db"
            ).build().also { instance = it }
        }
    }
}

class MayraMemoryEngine(context: Context) {
    private val dao = MayraPersonalDatabase.get(context).memoryDao()

    fun observeMemories(): Flow<List<MayraMemoryEntity>> = dao.observeActive()

    suspend fun remember(
        kind: MayraMemoryKind,
        title: String,
        text: String,
        tags: Set<String> = emptySet(),
        source: MayraMemorySource = MayraMemorySource.USER_CONFIRMED,
        confidence: Float = 1f
    ): MayraMemoryEntity {
        require(title.isNotBlank() && text.isNotBlank())
        require(!SensitiveMemoryGuard.looksSensitive("$title $text")) { "Sensitive secret-like content cannot be saved to Mayra memory." }
        val memory = MayraMemoryEntity(
            kind = kind,
            title = title.trim().take(100),
            text = text.trim().take(2_000),
            normalizedText = normalize("$title $text ${tags.joinToString(" ")}"),
            tags = tags.map(String::trim).filter(String::isNotBlank).distinct().joinToString(","),
            source = source,
            confidence = confidence.coerceIn(0f, 1f)
        )
        dao.upsert(memory)
        return memory
    }

    suspend fun recall(query: String, limit: Int = 8): List<MayraMemoryEntity> {
        val normalized = normalize(query)
        if (normalized.length < 2) return dao.recentRelevant(limit)
        val matches = dao.search(normalized, limit)
        if (matches.isNotEmpty()) dao.markUsed(matches.map { it.id })
        return matches
    }

    suspend fun contextForPrompt(query: String, maxCharacters: Int = 1_800): String {
        val memories = recall(query, 10)
        return memories.joinToString("\n") { "- [${it.kind}] ${it.title}: ${it.text}" }.take(maxCharacters)
    }

    suspend fun archive(id: String) = dao.archive(id)
    suspend fun activeCount(): Int = dao.activeCount()
    suspend fun cleanupArchived(retentionDays: Int = 30): Int =
        dao.purgeArchived(System.currentTimeMillis() - retentionDays.coerceAtLeast(1) * 86_400_000L)

    private fun normalize(value: String): String = value.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}

object SensitiveMemoryGuard {
    private val signals = listOf(
        Regex("\\b(?:otp|cvv|pin|password|passcode)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d{12,19}\\b"),
        Regex("\\b(?:api[_ -]?key|secret[_ -]?key|private[_ -]?key)\\b", RegexOption.IGNORE_CASE)
    )
    fun looksSensitive(text: String): Boolean = signals.any { it.containsMatchIn(text) }
}
