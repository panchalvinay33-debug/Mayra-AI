package ai.mayra.app.knowledge

import android.content.Context
import java.util.UUID

/** Personal entities Mayra can relate without storing secrets in plaintext. */
enum class KnowledgeEntityType { PERSON, PLACE, EVENT, PROJECT, DEVICE, ACCOUNT_REFERENCE, INTEREST, PREFERENCE, TOPIC }

enum class KnowledgeRelationType {
    RELATED_TO, FAMILY_OF, FRIEND_OF, WORKS_ON, LOCATED_AT, ATTENDED, OWNS, PREFERS, MENTIONED_WITH, DEPENDS_ON
}

data class KnowledgeEntity(
    val id: String = UUID.randomUUID().toString(),
    val type: KnowledgeEntityType,
    val name: String,
    val aliases: Set<String> = emptySet(),
    val attributes: Map<String, String> = emptyMap(),
    val tags: Set<String> = emptySet(),
    val confidence: Double = 0.5,
    val importance: Int = 1,
    val source: String = "user",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val lastVerifiedAt: Long? = null,
    val expiresAt: Long? = null,
    val usageCount: Int = 0,
    val sensitive: Boolean = false
) {
    init {
        require(name.isNotBlank())
        require(confidence in 0.0..1.0)
        require(importance in 1..5)
        require(usageCount >= 0)
    }

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean = expiresAt?.let { it <= now } == true
}

data class KnowledgeRelation(
    val id: String = UUID.randomUUID().toString(),
    val fromId: String,
    val toId: String,
    val type: KnowledgeRelationType,
    val confidence: Double = 0.5,
    val source: String = "inference",
    val createdAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long? = null
) {
    init {
        require(fromId != toId)
        require(confidence in 0.0..1.0)
    }
}

data class KnowledgeSearchHit(
    val entity: KnowledgeEntity,
    val score: Double,
    val matchedTerms: Set<String>
)

data class KnowledgeDiagnostics(
    val entities: Int,
    val relations: Int,
    val people: Int,
    val projects: Int,
    val sensitiveItems: Int,
    val expiredItems: Int,
    val averageConfidence: Double
)

class MayraKnowledgeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun upsert(entity: KnowledgeEntity): KnowledgeEntity {
        val normalized = entity.copy(
            name = entity.name.trim().take(MAX_FIELD),
            aliases = entity.aliases.map(String::trim).filter(String::isNotBlank).take(MAX_ALIASES).toSet(),
            attributes = entity.attributes.entries.take(MAX_ATTRIBUTES).associate { sanitize(it.key) to sanitize(it.value) },
            tags = entity.tags.map(::sanitize).filter(String::isNotBlank).take(MAX_TAGS).toSet(),
            updatedAt = System.currentTimeMillis()
        )
        saveEntities(entities().filterNot { it.id == normalized.id } + normalized)
        return normalized
    }

    @Synchronized
    fun relate(relation: KnowledgeRelation): KnowledgeRelation {
        require(get(relation.fromId) != null) { "Unknown from entity" }
        require(get(relation.toId) != null) { "Unknown to entity" }
        saveRelations(relations().filterNot { it.id == relation.id } + relation)
        return relation
    }

    fun get(id: String): KnowledgeEntity? = entities().firstOrNull { it.id == id }

    fun entities(includeExpired: Boolean = false, now: Long = System.currentTimeMillis()): List<KnowledgeEntity> =
        preferences.getStringSet(KEY_ENTITIES, emptySet()).orEmpty()
            .mapNotNull(::decodeEntity)
            .filter { includeExpired || !it.isExpired(now) }
            .sortedWith(compareByDescending<KnowledgeEntity> { it.importance }.thenByDescending { it.updatedAt })

    fun relations(): List<KnowledgeRelation> = preferences.getStringSet(KEY_RELATIONS, emptySet()).orEmpty()
        .mapNotNull(::decodeRelation)
        .sortedByDescending { it.createdAt }

    fun related(entityId: String): List<Pair<KnowledgeRelation, KnowledgeEntity>> = relations().mapNotNull { relation ->
        val otherId = when (entityId) {
            relation.fromId -> relation.toId
            relation.toId -> relation.fromId
            else -> return@mapNotNull null
        }
        get(otherId)?.let { relation to it }
    }

    @Synchronized
    fun recordUse(entityId: String): KnowledgeEntity? {
        val entity = get(entityId) ?: return null
        return upsert(entity.copy(usageCount = entity.usageCount + 1))
    }

    @Synchronized
    fun verify(entityId: String, confidence: Double = 1.0): KnowledgeEntity? {
        val entity = get(entityId) ?: return null
        return upsert(entity.copy(confidence = confidence.coerceIn(0.0, 1.0), lastVerifiedAt = System.currentTimeMillis()))
    }

    @Synchronized
    fun remove(entityId: String): Boolean {
        val current = entities(includeExpired = true)
        if (current.none { it.id == entityId }) return false
        saveEntities(current.filterNot { it.id == entityId })
        saveRelations(relations().filterNot { it.fromId == entityId || it.toId == entityId })
        return true
    }

    @Synchronized
    fun prune(maxEntities: Int = 500, maxRelations: Int = 1200, now: Long = System.currentTimeMillis()) {
        require(maxEntities > 0 && maxRelations > 0)
        val retainedEntities = entities(includeExpired = true)
            .filterNot { it.isExpired(now) }
            .sortedWith(compareByDescending<KnowledgeEntity> { it.importance }.thenByDescending { it.usageCount }.thenByDescending { it.updatedAt })
            .take(maxEntities)
        val ids = retainedEntities.map { it.id }.toSet()
        val retainedRelations = relations().filter { it.fromId in ids && it.toId in ids }.take(maxRelations)
        saveEntities(retainedEntities)
        saveRelations(retainedRelations)
    }

    fun diagnostics(now: Long = System.currentTimeMillis()): KnowledgeDiagnostics {
        val all = entities(includeExpired = true, now = now)
        return KnowledgeDiagnostics(
            entities = all.size,
            relations = relations().size,
            people = all.count { it.type == KnowledgeEntityType.PERSON },
            projects = all.count { it.type == KnowledgeEntityType.PROJECT },
            sensitiveItems = all.count { it.sensitive },
            expiredItems = all.count { it.isExpired(now) },
            averageConfidence = if (all.isEmpty()) 0.0 else all.map { it.confidence }.average()
        )
    }

    private fun saveEntities(items: List<KnowledgeEntity>) {
        preferences.edit().putStringSet(KEY_ENTITIES, items.map(::encodeEntity).toSet()).apply()
    }

    private fun saveRelations(items: List<KnowledgeRelation>) {
        preferences.edit().putStringSet(KEY_RELATIONS, items.map(::encodeRelation).toSet()).apply()
    }

    private fun encodeEntity(e: KnowledgeEntity): String = listOf(
        e.id, e.type.name, sanitize(e.name), encodeSet(e.aliases), encodeMap(e.attributes), encodeSet(e.tags),
        e.confidence, e.importance, sanitize(e.source), e.createdAt, e.updatedAt, e.lastVerifiedAt ?: -1L,
        e.expiresAt ?: -1L, e.usageCount, e.sensitive
    ).joinToString(FIELD_SEPARATOR)

    private fun decodeEntity(raw: String): KnowledgeEntity? {
        val p = raw.split(FIELD_SEPARATOR)
        if (p.size != 15) return null
        return runCatching {
            KnowledgeEntity(
                id = p[0], type = KnowledgeEntityType.valueOf(p[1]), name = p[2], aliases = decodeSet(p[3]),
                attributes = decodeMap(p[4]), tags = decodeSet(p[5]), confidence = p[6].toDouble(), importance = p[7].toInt(),
                source = p[8], createdAt = p[9].toLong(), updatedAt = p[10].toLong(),
                lastVerifiedAt = p[11].toLong().takeIf { it >= 0 }, expiresAt = p[12].toLong().takeIf { it >= 0 },
                usageCount = p[13].toInt(), sensitive = p[14].toBooleanStrict()
            )
        }.getOrNull()
    }

    private fun encodeRelation(r: KnowledgeRelation): String = listOf(
        r.id, r.fromId, r.toId, r.type.name, r.confidence, sanitize(r.source), r.createdAt, r.lastVerifiedAt ?: -1L
    ).joinToString(FIELD_SEPARATOR)

    private fun decodeRelation(raw: String): KnowledgeRelation? {
        val p = raw.split(FIELD_SEPARATOR)
        if (p.size != 8) return null
        return runCatching {
            KnowledgeRelation(p[0], p[1], p[2], KnowledgeRelationType.valueOf(p[3]), p[4].toDouble(), p[5], p[6].toLong(), p[7].toLong().takeIf { it >= 0 })
        }.getOrNull()
    }

    private fun encodeSet(values: Set<String>): String = values.joinToString(ITEM_SEPARATOR) { sanitize(it) }
    private fun decodeSet(raw: String): Set<String> = raw.split(ITEM_SEPARATOR).filter(String::isNotBlank).toSet()
    private fun encodeMap(values: Map<String, String>): String = values.entries.joinToString(ITEM_SEPARATOR) { "${sanitize(it.key)}$MAP_SEPARATOR${sanitize(it.value)}" }
    private fun decodeMap(raw: String): Map<String, String> = raw.split(ITEM_SEPARATOR).mapNotNull {
        val i = it.indexOf(MAP_SEPARATOR)
        if (i <= 0) null else it.substring(0, i) to it.substring(i + MAP_SEPARATOR.length)
    }.toMap()
    private fun sanitize(value: String): String = value.replace(FIELD_SEPARATOR, " ").replace(ITEM_SEPARATOR, " ").replace(MAP_SEPARATOR, " ").take(MAX_FIELD)

    private companion object {
        const val FILE_NAME = "mayra_knowledge_graph"
        const val KEY_ENTITIES = "entities"
        const val KEY_RELATIONS = "relations"
        const val FIELD_SEPARATOR = "\u001C"
        const val ITEM_SEPARATOR = "\u001D"
        const val MAP_SEPARATOR = "\u001E"
        const val MAX_FIELD = 1200
        const val MAX_ALIASES = 12
        const val MAX_ATTRIBUTES = 30
        const val MAX_TAGS = 20
    }
}

class MayraKnowledgeSearch(private val store: MayraKnowledgeStore) {
    fun search(query: String, limit: Int = 20, includeSensitive: Boolean = false): List<KnowledgeSearchHit> {
        require(limit > 0)
        val terms = tokenize(query)
        if (terms.isEmpty()) return emptyList()
        return store.entities().asSequence()
            .filter { includeSensitive || !it.sensitive }
            .mapNotNull { entity ->
                val nameTokens = tokenize(entity.name) + entity.aliases.flatMap(::tokenize)
                val tagTokens = entity.tags.flatMap(::tokenize)
                val attributeTokens = entity.attributes.flatMap { tokenize(it.key) + tokenize(it.value) }
                val matched = terms.filter { term -> nameTokens.any { it.contains(term) } || tagTokens.any { it.contains(term) } || attributeTokens.any { it.contains(term) } }.toSet()
                if (matched.isEmpty()) null else {
                    val coverage = matched.size.toDouble() / terms.size
                    val exactBoost = if (entity.name.equals(query.trim(), ignoreCase = true)) 0.35 else 0.0
                    val score = (coverage * 0.55 + entity.confidence * 0.2 + entity.importance / 5.0 * 0.15 + minOf(entity.usageCount, 20) / 20.0 * 0.1 + exactBoost).coerceAtMost(1.0)
                    KnowledgeSearchHit(entity, score, matched)
                }
            }
            .sortedByDescending { it.score }
            .take(limit)
            .toList()
    }

    private fun tokenize(value: String): Set<String> = value.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }.toSet()
}
