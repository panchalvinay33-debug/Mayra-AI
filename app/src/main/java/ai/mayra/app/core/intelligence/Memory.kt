package ai.mayra.app.core.intelligence

import java.time.Instant
import java.util.UUID

/** A single memory item that can be ranked and supplied to prompts. */
data class MemoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val tags: Set<String> = emptySet(),
    val importance: Int = 50,
    val createdAt: Instant = Instant.now(),
    val lastAccessedAt: Instant? = null,
    val accessCount: Int = 0
) {
    init {
        require(content.isNotBlank()) { "Memory content cannot be blank." }
        require(importance in 0..100) { "Importance must be between 0 and 100." }
        require(accessCount >= 0) { "Access count cannot be negative." }
    }
}

/** Query used by memory stores and retrieval policies. */
data class MemoryQuery(
    val text: String,
    val tags: Set<String> = emptySet(),
    val limit: Int = 8
) {
    init {
        require(limit > 0) { "Memory query limit must be positive." }
    }
}

interface MemoryStore {
    suspend fun save(record: MemoryRecord): MemoryRecord
    suspend fun get(id: String): MemoryRecord?
    suspend fun delete(id: String): Boolean
    suspend fun search(query: MemoryQuery): List<MemoryRecord>
    suspend fun all(): List<MemoryRecord>
}

/** Deterministic in-memory implementation suitable for local runtime and tests. */
class InMemoryMemoryStore : MemoryStore {
    private val records = linkedMapOf<String, MemoryRecord>()

    override suspend fun save(record: MemoryRecord): MemoryRecord = synchronized(records) {
        records[record.id] = record
        record
    }

    override suspend fun get(id: String): MemoryRecord? = synchronized(records) { records[id] }

    override suspend fun delete(id: String): Boolean = synchronized(records) { records.remove(id) != null }

    override suspend fun all(): List<MemoryRecord> = synchronized(records) { records.values.toList() }

    override suspend fun search(query: MemoryQuery): List<MemoryRecord> {
        val terms = query.text.lowercase().split(Regex("\\s+")).filter(String::isNotBlank)
        return synchronized(records) {
            records.values
                .asSequence()
                .filter { record -> query.tags.isEmpty() || record.tags.any(query.tags::contains) }
                .map { record ->
                    val haystack = buildString {
                        append(record.content.lowercase())
                        append(' ')
                        append(record.tags.joinToString(" ").lowercase())
                    }
                    val relevance = terms.count(haystack::contains)
                    record to relevance
                }
                .filter { (_, relevance) -> terms.isEmpty() || relevance > 0 }
                .sortedWith(
                    compareByDescending<Pair<MemoryRecord, Int>> { it.second }
                        .thenByDescending { it.first.importance }
                        .thenByDescending { it.first.createdAt }
                )
                .take(query.limit)
                .map { it.first }
                .toList()
        }
    }
}
