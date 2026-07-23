package ai.mayra.app.core.memory

import java.util.Locale

enum class MemoryKind { PROFILE, PREFERENCE, RELATIONSHIP, ROUTINE, PROJECT, OTHER }

data class MemoryRecord(
    val namespace: String,
    val key: String,
    val value: String,
    val kind: MemoryKind = MemoryKind.OTHER,
    val confidence: Double = 1.0,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val source: String? = null
) {
    init {
        require(namespace.isNotBlank())
        require(key.isNotBlank())
        require(value.isNotBlank())
        require(confidence in 0.0..1.0)
        require(createdAt >= 0L)
        require(updatedAt >= createdAt)
    }

    val id: String get() = "${namespace.normalizedToken()}:${key.normalizedToken()}"
}

data class MemorySearchResult(val record: MemoryRecord, val score: Int)

data class MemorySnapshot(
    val records: List<MemoryRecord>,
    val totalCount: Int,
    val namespaces: Set<String>,
    val lastUpdatedAt: Long?
)

class LongTermMemoryEngine(
    initialRecords: Iterable<MemoryRecord> = emptyList(),
    private val locale: Locale = Locale.ROOT
) {
    private val records = linkedMapOf<String, MemoryRecord>()

    init { initialRecords.forEach(::upsert) }

    @Synchronized
    fun upsert(record: MemoryRecord): MemoryRecord {
        val normalized = record.normalized(locale)
        val existing = records[normalized.id]
        val stored = if (existing == null) normalized else normalized.copy(createdAt = existing.createdAt)
        records[stored.id] = stored
        return stored
    }

    @Synchronized
    fun remember(
        namespace: String,
        key: String,
        value: String,
        kind: MemoryKind = MemoryKind.OTHER,
        confidence: Double = 1.0,
        timestamp: Long,
        source: String? = null
    ): MemoryRecord = upsert(
        MemoryRecord(namespace, key, value, kind, confidence, timestamp, timestamp, source)
    )

    @Synchronized fun get(namespace: String, key: String): MemoryRecord? = records[memoryId(namespace, key, locale)]
    @Synchronized fun forget(namespace: String, key: String): Boolean = records.remove(memoryId(namespace, key, locale)) != null

    @Synchronized
    fun clear(namespace: String? = null): Int {
        if (namespace == null) {
            val count = records.size
            records.clear()
            return count
        }
        val normalizedNamespace = namespace.normalizedToken(locale)
        val ids = records.values.filter { it.namespace == normalizedNamespace }.map { it.id }
        ids.forEach(records::remove)
        return ids.size
    }

    @Synchronized
    fun search(
        query: String,
        limit: Int = DEFAULT_SEARCH_LIMIT,
        minimumConfidence: Double = 0.0,
        kinds: Set<MemoryKind> = emptySet()
    ): List<MemorySearchResult> {
        require(limit > 0)
        require(minimumConfidence in 0.0..1.0)
        val terms = query.normalizedWords(locale)
        if (terms.isEmpty()) return emptyList()

        return records.values.asSequence()
            .filter { it.confidence >= minimumConfidence }
            .filter { kinds.isEmpty() || it.kind in kinds }
            .mapNotNull { record ->
                val score = score(record, terms)
                if (score == 0) null else MemorySearchResult(record, score)
            }
            .sortedWith(compareByDescending<MemorySearchResult> { it.score }
                .thenByDescending { it.record.confidence }
                .thenByDescending { it.record.updatedAt }
                .thenBy { it.record.id })
            .take(limit)
            .toList()
    }

    @Synchronized
    fun snapshot(): MemorySnapshot {
        val snapshotRecords = records.values.sortedWith(compareBy<MemoryRecord> { it.namespace }.thenBy { it.key })
        return MemorySnapshot(
            records = snapshotRecords,
            totalCount = snapshotRecords.size,
            namespaces = snapshotRecords.mapTo(linkedSetOf()) { it.namespace },
            lastUpdatedAt = snapshotRecords.maxOfOrNull { it.updatedAt }
        )
    }

    private fun score(record: MemoryRecord, terms: Set<String>): Int {
        val namespaceWords = record.namespace.normalizedWords(locale)
        val keyWords = record.key.normalizedWords(locale)
        val valueWords = record.value.normalizedWords(locale)
        val sourceWords = record.source.orEmpty().normalizedWords(locale)

        return terms.fold(0) { total, term ->
            total + when {
                term in keyWords -> 8
                term in namespaceWords -> 6
                term in valueWords -> 4
                term in sourceWords -> 2
                record.key.contains(term, ignoreCase = true) -> 3
                record.value.contains(term, ignoreCase = true) -> 1
                else -> 0
            }
        }
    }

    companion object { const val DEFAULT_SEARCH_LIMIT = 10 }
}

private fun MemoryRecord.normalized(locale: Locale): MemoryRecord = copy(
    namespace = namespace.normalizedToken(locale),
    key = key.normalizedToken(locale),
    value = value.normalizedText(),
    source = source?.normalizedText()?.takeIf(String::isNotBlank)
)

private fun memoryId(namespace: String, key: String, locale: Locale): String =
    "${namespace.normalizedToken(locale)}:${key.normalizedToken(locale)}"

private fun String.normalizedToken(locale: Locale = Locale.ROOT): String =
    normalizedText().lowercase(locale).replace(TOKEN_SEPARATOR_REGEX, "_").trim('_')

private fun String.normalizedWords(locale: Locale): Set<String> =
    normalizedText().lowercase(locale).split(WORD_SEPARATOR_REGEX).filterTo(linkedSetOf()) { it.isNotBlank() }

private fun String.normalizedText(): String = trim().replace(WHITESPACE_REGEX, " ")
private val WHITESPACE_REGEX = Regex("\\s+")
private val TOKEN_SEPARATOR_REGEX = Regex("[^\\p{L}\\p{N}]+")
private val WORD_SEPARATOR_REGEX = Regex("[^\\p{L}\\p{N}]+")