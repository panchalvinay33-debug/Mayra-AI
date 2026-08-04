package ai.mayra.app.learning

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
import androidx.room.Transaction

@Entity(
    tableName = "mayra_learned_memory",
    indices = [
        Index(value = ["normalizedKey"], unique = true),
        Index(value = ["state"]),
        Index(value = ["category"]),
        Index(value = ["updatedAtEpochMs"])
    ]
)
data class LearnedMemoryEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val normalizedKey: String,
    val displayKey: String,
    val value: String,
    val category: String,
    val source: String,
    val confidence: Double,
    val persistence: String,
    val state: String,
    val policyReason: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val approvedAtEpochMs: Long? = null,
    val expiresAtEpochMs: Long? = null
)

enum class LearnedMemoryState { PENDING, APPROVED, REJECTED, FORGOTTEN }

@Dao
interface MayraLearningDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LearnedMemoryEntity): Long

    @Query("SELECT * FROM mayra_learned_memory WHERE normalizedKey = :key LIMIT 1")
    suspend fun findByKey(key: String): LearnedMemoryEntity?

    @Query("SELECT * FROM mayra_learned_memory WHERE state = :state ORDER BY updatedAtEpochMs DESC")
    suspend fun listByState(state: String): List<LearnedMemoryEntity>

    @Query("SELECT * FROM mayra_learned_memory WHERE state = 'APPROVED' AND (expiresAtEpochMs IS NULL OR expiresAtEpochMs > :nowEpochMs) ORDER BY updatedAtEpochMs DESC LIMIT :limit")
    suspend fun approvedForContext(nowEpochMs: Long, limit: Int): List<LearnedMemoryEntity>

    @Query("UPDATE mayra_learned_memory SET state = :state, updatedAtEpochMs = :nowEpochMs, approvedAtEpochMs = :approvedAtEpochMs WHERE normalizedKey = :key")
    suspend fun updateState(key: String, state: String, nowEpochMs: Long, approvedAtEpochMs: Long?): Int

    @Query("UPDATE mayra_learned_memory SET state = 'FORGOTTEN', value = '', updatedAtEpochMs = :nowEpochMs, approvedAtEpochMs = NULL WHERE normalizedKey = :key")
    suspend fun forget(key: String, nowEpochMs: Long): Int

    @Query("UPDATE mayra_learned_memory SET state = 'FORGOTTEN', value = '', updatedAtEpochMs = :nowEpochMs, approvedAtEpochMs = NULL WHERE state != 'FORGOTTEN'")
    suspend fun forgetAll(nowEpochMs: Long): Int
}

@Database(entities = [LearnedMemoryEntity::class], version = 1, exportSchema = true)
abstract class MayraLearningDatabase : RoomDatabase() {
    abstract fun learningDao(): MayraLearningDao

    companion object {
        @Volatile private var instance: MayraLearningDatabase? = null

        fun get(context: Context): MayraLearningDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MayraLearningDatabase::class.java,
                "mayra-learning.db"
            ).build().also { instance = it }
        }
    }
}

data class LearningSubmission(
    val decision: LearningDecision,
    val memory: LearnedMemoryEntity?
)

class MayraLearningRepository(
    private val dao: MayraLearningDao,
    private val now: () -> Long = System::currentTimeMillis
) {
    suspend fun submit(candidate: LearningCandidate): LearningSubmission {
        val decision = MayraSelfLearningPolicy.evaluate(candidate)
        if (decision is LearningDecision.Reject) return LearningSubmission(decision, null)

        val timestamp = now()
        val normalizedKey = normalizeKey(candidate.key)
        val state = when (decision) {
            is LearningDecision.AllowLowRisk -> LearnedMemoryState.APPROVED
            is LearningDecision.RequireConfirmation -> LearnedMemoryState.PENDING
            is LearningDecision.Reject -> error("handled above")
        }
        val existing = dao.findByKey(normalizedKey)
        val entity = LearnedMemoryEntity(
            id = existing?.id ?: 0,
            normalizedKey = normalizedKey,
            displayKey = candidate.key.trim(),
            value = candidate.value.trim(),
            category = candidate.category.name,
            source = candidate.source.name,
            confidence = candidate.confidence.coerceIn(0.0, 1.0),
            persistence = candidate.persistence.name,
            state = state.name,
            policyReason = decision.reason,
            createdAtEpochMs = existing?.createdAtEpochMs ?: timestamp,
            updatedAtEpochMs = timestamp,
            approvedAtEpochMs = if (state == LearnedMemoryState.APPROVED) timestamp else null,
            expiresAtEpochMs = expiryFor(candidate.persistence, timestamp)
        )
        val id = dao.upsert(entity)
        return LearningSubmission(decision, entity.copy(id = if (entity.id == 0L) id else entity.id))
    }

    suspend fun approve(key: String): Boolean {
        val timestamp = now()
        return dao.updateState(normalizeKey(key), LearnedMemoryState.APPROVED.name, timestamp, timestamp) == 1
    }

    suspend fun reject(key: String): Boolean = dao.updateState(
        normalizeKey(key), LearnedMemoryState.REJECTED.name, now(), null
    ) == 1

    suspend fun forget(key: String): Boolean = dao.forget(normalizeKey(key), now()) == 1

    suspend fun forgetAll(): Int = dao.forgetAll(now())

    suspend fun pending(): List<LearnedMemoryEntity> = dao.listByState(LearnedMemoryState.PENDING.name)

    suspend fun approvedContext(limit: Int = 12): List<LearnedMemoryEntity> =
        dao.approvedForContext(now(), limit.coerceIn(1, 20))

    companion object {
        fun normalizeKey(raw: String): String = raw.trim().lowercase().replace(Regex("[^a-z0-9._-]+"), "_").trim('_').take(80)

        private fun expiryFor(persistence: LearningPersistence, now: Long): Long? = when (persistence) {
            LearningPersistence.SESSION -> now + 24L * 60L * 60L * 1000L
            LearningPersistence.LONG_TERM -> null
            LearningPersistence.PERMANENT -> null
        }
    }
}
