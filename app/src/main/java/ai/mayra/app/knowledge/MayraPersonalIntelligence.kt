package ai.mayra.app.knowledge

import java.util.UUID

enum class RecommendationType { FOLLOW_UP, REMINDER, PROJECT, PERSON, NOTE, ROUTINE, SAFETY }
enum class RecommendationAction { SUGGEST, ASK, DEFER, SUPPRESS }

data class PersonalContext(
    val hourOfDay: Int,
    val userAvailable: Boolean,
    val quietHours: Boolean,
    val activeEntityIds: Set<String> = emptySet(),
    val activeTags: Set<String> = emptySet(),
    val now: Long = System.currentTimeMillis()
) {
    init { require(hourOfDay in 0..23) }
}

data class PersonalRecommendation(
    val id: String = UUID.randomUUID().toString(),
    val type: RecommendationType,
    val title: String,
    val explanation: String,
    val action: RecommendationAction,
    val score: Double,
    val linkedEntityId: String? = null,
    val linkedNoteId: String? = null,
    val expiresAt: Long? = null
) {
    init { require(score in 0.0..1.0) }
}

data class PersonalSearchResult(
    val knowledge: List<KnowledgeSearchHit>,
    val memory: List<PersonalMemoryHit>
)

data class PersonalIntelligenceDiagnostics(
    val knowledge: KnowledgeDiagnostics,
    val memory: PersonalMemoryDiagnostics,
    val generatedRecommendations: Int,
    val suppressedRecommendations: Int
)

class MayraRecommendationEngine(
    private val knowledge: MayraKnowledgeStore,
    private val memory: MayraPersonalMemory
) {
    fun generate(context: PersonalContext, limit: Int = 8): List<PersonalRecommendation> {
        require(limit > 0)
        val candidates = mutableListOf<PersonalRecommendation>()

        memory.notes().filter { it.priority >= 4 && it.archivedAt == null }.take(10).forEach { note ->
            val incomplete = note.checklist.count { !it.completed }
            val score = (0.55 + note.priority * 0.07 + if (note.pinned) 0.08 else 0.0 + minOf(incomplete, 5) * 0.025).coerceAtMost(1.0)
            candidates += PersonalRecommendation(
                type = if (note.type == PersonalNoteType.PROJECT_NOTE) RecommendationType.PROJECT else RecommendationType.NOTE,
                title = note.title,
                explanation = if (incomplete > 0) "$incomplete checklist items are still incomplete" else "This is a high-priority saved item",
                action = chooseAction(context, sensitive = note.sensitive, score = score),
                score = score,
                linkedNoteId = note.id,
                expiresAt = context.now + 6 * 60 * 60 * 1000L
            )
        }

        knowledge.entities().filter { it.importance >= 4 }.take(12).forEach { entity ->
            val staleDays = (context.now - (entity.lastVerifiedAt ?: entity.updatedAt)).coerceAtLeast(0) / DAY_MS
            if (entity.type == KnowledgeEntityType.PERSON && staleDays >= 14) {
                val score = (0.45 + entity.importance * 0.07 + minOf(staleDays, 60).toDouble() / 300).coerceAtMost(0.92)
                candidates += PersonalRecommendation(
                    type = RecommendationType.PERSON,
                    title = "Follow up with ${entity.name}",
                    explanation = "No recent verified interaction for $staleDays days",
                    action = chooseAction(context, entity.sensitive, score), score = score,
                    linkedEntityId = entity.id, expiresAt = context.now + DAY_MS
                )
            }
            if (entity.type == KnowledgeEntityType.PROJECT && entity.id in context.activeEntityIds) {
                val score = (0.6 + entity.confidence * 0.2 + entity.importance * 0.04).coerceAtMost(1.0)
                candidates += PersonalRecommendation(
                    type = RecommendationType.PROJECT,
                    title = "Continue ${entity.name}",
                    explanation = "This project is active in the current context",
                    action = chooseAction(context, entity.sensitive, score), score = score,
                    linkedEntityId = entity.id, expiresAt = context.now + 4 * 60 * 60 * 1000L
                )
            }
        }

        memory.timeline(
            from = context.now - 30 * DAY_MS,
            to = context.now,
            types = setOf(TimelineEventType.REMINDER),
            includeSensitive = false,
            limit = 40
        ).groupBy { it.title.lowercase() }.filterValues { it.size >= 3 }.forEach { (_, events) ->
            val latest = events.maxBy { it.occurredAt }
            val score = (0.5 + minOf(events.size, 10) * 0.04).coerceAtMost(0.9)
            candidates += PersonalRecommendation(
                type = RecommendationType.ROUTINE,
                title = latest.title,
                explanation = "This reminder appeared ${events.size} times recently",
                action = chooseAction(context, latest.sensitive, score), score = score,
                linkedEntityId = latest.linkedEntityIds.firstOrNull(), expiresAt = context.now + DAY_MS
            )
        }

        return candidates.distinctBy { listOf(it.type, it.title.lowercase(), it.linkedEntityId, it.linkedNoteId) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun chooseAction(context: PersonalContext, sensitive: Boolean, score: Double): RecommendationAction = when {
        sensitive -> RecommendationAction.ASK
        context.quietHours -> RecommendationAction.DEFER
        !context.userAvailable -> RecommendationAction.DEFER
        score < 0.45 -> RecommendationAction.SUPPRESS
        else -> RecommendationAction.SUGGEST
    }

    private companion object { const val DAY_MS = 24 * 60 * 60 * 1000L }
}

class MayraPersonalIntelligence(
    val knowledge: MayraKnowledgeStore,
    val memory: MayraPersonalMemory,
    private val knowledgeSearch: MayraKnowledgeSearch = MayraKnowledgeSearch(knowledge),
    private val recommendations: MayraRecommendationEngine = MayraRecommendationEngine(knowledge, memory)
) {
    private var generatedRecommendations = 0
    private var suppressedRecommendations = 0

    fun rememberEntity(entity: KnowledgeEntity): KnowledgeEntity = knowledge.upsert(entity)
    fun relate(relation: KnowledgeRelation): KnowledgeRelation = knowledge.relate(relation)
    fun saveNote(note: PersonalNote): PersonalNote = memory.saveNote(note)
    fun record(event: TimelineEvent): TimelineEvent = memory.appendEvent(event)

    fun search(query: String, includeSensitive: Boolean = false, limitPerSource: Int = 12): PersonalSearchResult = PersonalSearchResult(
        knowledge = knowledgeSearch.search(query, limitPerSource, includeSensitive),
        memory = memory.search(query, includeSensitive, limitPerSource)
    )

    fun recommendations(context: PersonalContext, limit: Int = 8): List<PersonalRecommendation> {
        val result = recommendations.generate(context, limit)
        generatedRecommendations += result.count { it.action != RecommendationAction.SUPPRESS }
        suppressedRecommendations += result.count { it.action == RecommendationAction.SUPPRESS }
        return result.filterNot { it.action == RecommendationAction.SUPPRESS }
    }

    fun linkNoteToEntity(noteId: String, entityId: String): PersonalNote? {
        require(knowledge.get(entityId) != null) { "Unknown entity" }
        val note = memory.note(noteId) ?: return null
        return memory.saveNote(note.copy(linkedEntityIds = note.linkedEntityIds + entityId), addTimeline = false)
    }

    fun recordInteraction(entityId: String, type: TimelineEventType, title: String, description: String = ""): TimelineEvent {
        val entity = knowledge.get(entityId) ?: throw IllegalArgumentException("Unknown entity")
        knowledge.recordUse(entityId)
        return memory.appendEvent(TimelineEvent(type = type, title = title, description = description, linkedEntityIds = setOf(entity.id), sensitive = entity.sensitive))
    }

    fun prune() {
        knowledge.prune()
        memory.prune()
    }

    fun diagnostics(): PersonalIntelligenceDiagnostics = PersonalIntelligenceDiagnostics(
        knowledge = knowledge.diagnostics(),
        memory = memory.diagnostics(),
        generatedRecommendations = generatedRecommendations,
        suppressedRecommendations = suppressedRecommendations
    )
}
