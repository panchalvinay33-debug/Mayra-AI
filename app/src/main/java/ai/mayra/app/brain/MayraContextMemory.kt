package ai.mayra.app.brain

import android.content.Context
import java.util.UUID

enum class MemoryKind { PREFERENCE, ROUTINE, CONTACT, APP, COMMAND, GOAL, CONTEXT }
enum class MemorySensitivity { PUBLIC, PERSONAL, SENSITIVE }

data class MemoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val kind: MemoryKind,
    val key: String,
    val value: String,
    val confidence: Double,
    val sensitivity: MemorySensitivity,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val expiresAt: Long? = null,
    val observationCount: Int = 1
) {
    fun isExpired(now: Long): Boolean = expiresAt?.let { it <= now } == true
}

data class HabitSignal(
    val namespace: String,
    val key: String,
    val count: Int,
    val lastObservedAt: Long
)

class MayraContextMemory(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun remember(record: MemoryRecord, allowSensitive: Boolean = false): MemoryRecord {
        require(record.key.isNotBlank()) { "Memory key cannot be blank" }
        require(record.confidence in 0.0..1.0) { "Confidence must be between 0 and 1" }
        if (record.sensitivity == MemorySensitivity.SENSITIVE && !allowSensitive) {
            throw SecurityException("Sensitive memory requires explicit opt-in")
        }

        val existing = snapshot().firstOrNull { it.kind == record.kind && it.key == record.key }
        val merged = if (existing == null) record else record.copy(
            id = existing.id,
            createdAt = existing.createdAt,
            updatedAt = maxOf(record.updatedAt, existing.updatedAt),
            observationCount = existing.observationCount + 1,
            confidence = maxOf(existing.confidence, record.confidence)
        )
        save(snapshot().filterNot { it.id == merged.id } + merged)
        return merged
    }

    @Synchronized
    fun forget(id: String): Boolean {
        val current = snapshot()
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return false
        save(updated)
        return true
    }

    @Synchronized
    fun forgetByKind(kind: MemoryKind) {
        save(snapshot().filterNot { it.kind == kind })
    }

    @Synchronized
    fun prune(now: Long = System.currentTimeMillis(), maxEntries: Int = 300) {
        val retained = snapshot()
            .filterNot { it.isExpired(now) }
            .sortedWith(compareByDescending<MemoryRecord> { it.confidence }.thenByDescending { it.updatedAt })
            .take(maxEntries)
        save(retained)
    }

    fun query(
        kind: MemoryKind? = null,
        text: String? = null,
        minimumConfidence: Double = 0.0,
        now: Long = System.currentTimeMillis()
    ): List<MemoryRecord> {
        val normalized = text?.trim()?.lowercase().orEmpty()
        return snapshot()
            .asSequence()
            .filterNot { it.isExpired(now) }
            .filter { kind == null || it.kind == kind }
            .filter { it.confidence >= minimumConfidence }
            .filter { normalized.isBlank() || it.key.lowercase().contains(normalized) || it.value.lowercase().contains(normalized) }
            .sortedWith(compareByDescending<MemoryRecord> { it.confidence }.thenByDescending { it.updatedAt })
            .toList()
    }

    fun snapshot(): List<MemoryRecord> = preferences.getStringSet(KEY_MEMORIES, emptySet()).orEmpty()
        .mapNotNull(::decodeMemory)

    @Synchronized
    fun observeHabit(namespace: String, key: String, at: Long = System.currentTimeMillis()): HabitSignal {
        require(namespace.isNotBlank() && key.isNotBlank())
        val id = "$namespace:$key"
        val all = habitSnapshot().associateBy { "${it.namespace}:${it.key}" }.toMutableMap()
        val previous = all[id]
        val updated = HabitSignal(namespace, key, (previous?.count ?: 0) + 1, at)
        all[id] = updated
        preferences.edit().putStringSet(KEY_HABITS, all.values.map(::encodeHabit).toSet()).apply()
        return updated
    }

    fun topHabits(namespace: String, limit: Int = 5): List<HabitSignal> = habitSnapshot()
        .filter { it.namespace == namespace }
        .sortedWith(compareByDescending<HabitSignal> { it.count }.thenByDescending { it.lastObservedAt })
        .take(limit)

    private fun habitSnapshot(): List<HabitSignal> = preferences.getStringSet(KEY_HABITS, emptySet()).orEmpty()
        .mapNotNull(::decodeHabit)

    private fun save(records: List<MemoryRecord>) {
        preferences.edit().putStringSet(KEY_MEMORIES, records.map(::encodeMemory).toSet()).apply()
    }

    private fun encodeMemory(record: MemoryRecord): String = listOf(
        record.id, record.kind.name, record.key, record.value, record.confidence,
        record.sensitivity.name, record.createdAt, record.updatedAt,
        record.expiresAt?.toString().orEmpty(), record.observationCount
    ).joinToString(SEPARATOR) { sanitize(it.toString()) }

    private fun decodeMemory(raw: String): MemoryRecord? {
        val p = raw.split(SEPARATOR)
        if (p.size != 10) return null
        return MemoryRecord(
            id = p[0],
            kind = runCatching { MemoryKind.valueOf(p[1]) }.getOrNull() ?: return null,
            key = p[2], value = p[3],
            confidence = p[4].toDoubleOrNull() ?: return null,
            sensitivity = runCatching { MemorySensitivity.valueOf(p[5]) }.getOrNull() ?: return null,
            createdAt = p[6].toLongOrNull() ?: return null,
            updatedAt = p[7].toLongOrNull() ?: return null,
            expiresAt = p[8].toLongOrNull(),
            observationCount = p[9].toIntOrNull() ?: return null
        )
    }

    private fun encodeHabit(signal: HabitSignal): String = listOf(
        signal.namespace, signal.key, signal.count, signal.lastObservedAt
    ).joinToString(SEPARATOR) { sanitize(it.toString()) }

    private fun decodeHabit(raw: String): HabitSignal? {
        val p = raw.split(SEPARATOR)
        if (p.size != 4) return null
        return HabitSignal(p[0], p[1], p[2].toIntOrNull() ?: return null, p[3].toLongOrNull() ?: return null)
    }

    private fun sanitize(value: String): String = value.replace(SEPARATOR, " ").take(MAX_FIELD_LENGTH)

    private companion object {
        const val FILE_NAME = "mayra_context_memory"
        const val KEY_MEMORIES = "memories"
        const val KEY_HABITS = "habits"
        const val SEPARATOR = "\u001D"
        const val MAX_FIELD_LENGTH = 1000
    }
}
