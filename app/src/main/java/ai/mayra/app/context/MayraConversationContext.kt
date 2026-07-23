package ai.mayra.app.context

import java.util.UUID


enum class ContextSource { USER, ASSISTANT, VOICE, VISION, NOTIFICATION, AGENT, CALENDAR, MEMORY, SYSTEM }
enum class ContextEntityType { PERSON, APP, EVENT, PLACE, DOCUMENT, IMAGE, TASK, WORKFLOW, DATE_TIME, TOPIC, OTHER }

data class ContextEntity(
    val id: String = UUID.randomUUID().toString(),
    val type: ContextEntityType,
    val canonicalName: String,
    val aliases: Set<String> = emptySet(),
    val attributes: Map<String, String> = emptyMap(),
    val confidence: Double = 1.0,
    val sensitive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(canonicalName.isNotBlank())
        require(confidence in 0.0..1.0)
        require(attributes.size <= 30)
    }
}

data class ContextTurn(
    val id: String = UUID.randomUUID().toString(),
    val source: ContextSource,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val topic: String? = null,
    val entityIds: Set<String> = emptySet(),
    val sensitive: Boolean = false,
    val interrupted: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(text.length <= 8_000)
        require(metadata.size <= 30)
    }
}

data class PendingConversationAction(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val requiredFields: Set<String>,
    val collectedFields: Map<String, String> = emptyMap(),
    val expiresAt: Long,
    val sensitive: Boolean = false
) {
    val complete: Boolean get() = requiredFields.all(collectedFields::containsKey)
}

data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val startedAt: Long,
    val updatedAt: Long,
    val activeTopic: String? = null,
    val turns: List<ContextTurn> = emptyList(),
    val entities: Map<String, ContextEntity> = emptyMap(),
    val pendingAction: PendingConversationAction? = null,
    val interruptedTurnId: String? = null,
    val expiresAt: Long
)

data class ReferenceResolution(
    val original: String,
    val resolvedText: String,
    val entity: ContextEntity? = null,
    val confidence: Double,
    val clarificationNeeded: Boolean,
    val explanation: String? = null
)

class ConversationContextEngine(
    private val maxTurns: Int = 80,
    private val maxEntities: Int = 100,
    private val sessionTtlMillis: Long = 6 * 60 * 60 * 1000L,
    private val now: () -> Long = System::currentTimeMillis
) {
    private var session = freshSession()

    @Synchronized
    fun recordTurn(turn: ContextTurn): ConversationSession {
        ensureActive()
        val normalized = turn.copy(
            text = turn.text.trim().replace(Regex("\\s+"), " ").take(8_000),
            topic = turn.topic?.trim()?.take(120),
            metadata = turn.metadata.entries.take(30).associate { it.key.take(80) to it.value.take(500) }
        )
        val topic = normalized.topic ?: inferTopic(normalized.text) ?: session.activeTopic
        val updatedTurns = (session.turns + normalized).takeLast(maxTurns)
        session = session.copy(
            updatedAt = now(),
            activeTopic = topic,
            turns = updatedTurns,
            interruptedTurnId = if (normalized.interrupted) normalized.id else session.interruptedTurnId,
            expiresAt = now() + sessionTtlMillis
        )
        return session
    }

    @Synchronized
    fun upsertEntity(entity: ContextEntity): ContextEntity {
        ensureActive()
        val normalizedName = normalize(entity.canonicalName)
        val existing = session.entities.values.firstOrNull {
            it.type == entity.type && (normalize(it.canonicalName) == normalizedName || normalizedName in it.aliases.map(::normalize))
        }
        val merged = if (existing == null) entity.copy(
            canonicalName = entity.canonicalName.trim().take(160),
            aliases = entity.aliases.map { it.trim().take(120) }.filter(String::isNotBlank).toSet(),
            updatedAt = now()
        ) else existing.copy(
            canonicalName = entity.canonicalName.trim().ifBlank { existing.canonicalName }.take(160),
            aliases = (existing.aliases + entity.aliases + existing.canonicalName).take(30).toSet(),
            attributes = (existing.attributes + entity.attributes).entries.takeLast(30).associate { it.toPair() },
            confidence = maxOf(existing.confidence, entity.confidence),
            sensitive = existing.sensitive || entity.sensitive,
            updatedAt = now()
        )
        val entities = (session.entities - existing?.id.orEmpty() + (merged.id to merged))
            .values.sortedByDescending(ContextEntity::updatedAt).take(maxEntities).associateBy(ContextEntity::id)
        session = session.copy(entities = entities, updatedAt = now(), expiresAt = now() + sessionTtlMillis)
        return merged
    }

    @Synchronized
    fun resolveReference(text: String, preferredType: ContextEntityType? = null): ReferenceResolution {
        ensureActive()
        val normalized = normalize(text)
        val candidates = session.entities.values
            .asSequence()
            .filter { preferredType == null || it.type == preferredType }
            .map { entity -> entity to entityScore(normalized, entity) }
            .filter { it.second > 0.0 }
            .sortedWith(compareByDescending<Pair<ContextEntity, Double>> { it.second }.thenByDescending { it.first.updatedAt })
            .toList()
        val pronoun = containsPronoun(normalized)
        val lexicalBest = candidates.firstOrNull()
        val resolvedByRecency = pronoun && (lexicalBest == null || lexicalBest.second < 0.55)
        val best = if (resolvedByRecency) {
            mostRecentEntity(preferredType)?.let { it to 0.62 } ?: lexicalBest
        } else {
            lexicalBest
        }
        if (best == null) return ReferenceResolution(
            text,
            text,
            confidence = 0.0,
            clarificationNeeded = pronoun,
            explanation = if (pronoun) "Reference clear nahi hai" else null
        )
        val ambiguous = !resolvedByRecency && candidates.size > 1 && best.second - candidates[1].second < 0.12
        if (ambiguous) return ReferenceResolution(text, text, best.first, best.second, true, "Do possible references mile")
        val resolved = replacePronouns(text, best.first.canonicalName)
        return ReferenceResolution(text, resolved, best.first, best.second.coerceIn(0.0, 1.0), best.second < 0.55)
    }

    @Synchronized
    fun setPendingAction(action: PendingConversationAction?): ConversationSession {
        ensureActive()
        session = session.copy(pendingAction = action, updatedAt = now(), expiresAt = now() + sessionTtlMillis)
        return session
    }

    @Synchronized
    fun providePendingField(key: String, value: String): PendingConversationAction? {
        ensureActive()
        val pending = session.pendingAction ?: return null
        if (now() > pending.expiresAt || key !in pending.requiredFields) return pending
        val updated = pending.copy(collectedFields = pending.collectedFields + (key to value.trim().take(500)))
        session = session.copy(pendingAction = updated, updatedAt = now(), expiresAt = now() + sessionTtlMillis)
        return updated
    }

    @Synchronized
    fun resumeInterrupted(): ContextTurn? {
        ensureActive()
        val id = session.interruptedTurnId ?: return null
        val turn = session.turns.firstOrNull { it.id == id }
        session = session.copy(interruptedTurnId = null, updatedAt = now())
        return turn
    }

    @Synchronized
    fun snapshot(includeSensitive: Boolean = false): ConversationSession {
        ensureActive()
        if (includeSensitive) return session
        return session.copy(
            turns = session.turns.filterNot(ContextTurn::sensitive),
            entities = session.entities.filterValues { !it.sensitive },
            pendingAction = session.pendingAction?.takeUnless { it.sensitive }
        )
    }

    @Synchronized
    fun clear() { session = freshSession() }

    private fun ensureActive() {
        if (now() > session.expiresAt) session = freshSession()
        session.pendingAction?.takeIf { now() > it.expiresAt }?.let { session = session.copy(pendingAction = null) }
    }

    private fun freshSession(): ConversationSession {
        val timestamp = now()
        return ConversationSession(startedAt = timestamp, updatedAt = timestamp, expiresAt = timestamp + sessionTtlMillis)
    }

    private fun mostRecentEntity(type: ContextEntityType?): ContextEntity? = session.entities.values
        .filter { type == null || it.type == type }.maxByOrNull(ContextEntity::updatedAt)

    private fun entityScore(text: String, entity: ContextEntity): Double {
        val names = entity.aliases + entity.canonicalName
        val lexical = names.maxOfOrNull { alias ->
            val a = normalize(alias)
            when {
                a.isBlank() -> 0.0
                text == a -> 1.0
                text.contains(a) -> 0.88
                a.split(' ').any { it.length >= 3 && text.contains(it) } -> 0.55
                else -> 0.0
            }
        } ?: 0.0
        val age = (now() - entity.updatedAt).coerceAtLeast(0)
        val recency = (1.0 - age / sessionTtlMillis.toDouble()).coerceIn(0.0, 1.0)
        return (lexical * 0.78 + recency * 0.14 + entity.confidence * 0.08).coerceIn(0.0, 1.0)
    }

    private fun containsPronoun(value: String): Boolean = listOf(
        "usko", "usse", "uska", "iski", "isko", "it", "that", "them", "him", "her", "वो", "उसको"
    ).any { value.contains(it) }

    private fun replacePronouns(text: String, name: String): String {
        var output = text
        listOf("usko", "usse", "uska", "iski", "isko", "that one", "them", "him", "her", "उसको", "उससे").forEach {
            output = output.replace(Regex("(?i)\\b${Regex.escape(it)}\\b"), name)
        }
        return output
    }

    private fun inferTopic(text: String): String? {
        val value = normalize(text)
        return when {
            listOf("meeting", "calendar", "appointment").any(value::contains) -> "calendar"
            listOf("reminder", "task", "todo").any(value::contains) -> "tasks"
            listOf("photo", "image", "document", "bill").any(value::contains) -> "vision"
            listOf("message", "whatsapp", "call", "sms").any(value::contains) -> "communication"
            listOf("weather", "rain", "temperature").any(value::contains) -> "weather"
            else -> null
        }
    }

    private fun normalize(value: String): String = value.lowercase().trim().replace(Regex("\\s+"), " ")
}

data class ContextFusionInput(
    val conversation: ConversationSession? = null,
    val notifications: List<NotificationInsight> = emptyList(),
    val activeWorkflowId: String? = null,
    val workflowSummary: String? = null,
    val visionSummary: String? = null,
    val nextCalendarEvent: String? = null,
    val memoryFacts: List<String> = emptyList(),
    val deviceLocked: Boolean = false,
    val userBusy: Boolean = false,
    val generatedAt: Long = System.currentTimeMillis()
)

data class FusedContextSnapshot(
    val activeTopic: String?,
    val recentConversation: List<String>,
    val relevantEntities: List<ContextEntity>,
    val attentionItems: List<NotificationInsight>,
    val activeWorkflowId: String?,
    val workflowSummary: String?,
    val visionSummary: String?,
    val nextCalendarEvent: String?,
    val memoryFacts: List<String>,
    val safeForSpeech: Boolean,
    val generatedAt: Long
)

class ContextFusionEngine {
    fun fuse(input: ContextFusionInput, maxItems: Int = 12): FusedContextSnapshot {
        require(maxItems in 1..50)
        val conversation = input.conversation
        val notifications = input.notifications
            .filterNot { it.action in setOf(AttentionAction.IGNORE, AttentionAction.STORE_ONLY) }
            .sortedByDescending(NotificationInsight::attentionScore)
            .take(maxItems)
        val safeForSpeech = !input.deviceLocked && !input.userBusy && notifications.none {
            it.sensitivity >= NotificationSensitivity.SENSITIVE && it.action == AttentionAction.INTERRUPT
        }
        return FusedContextSnapshot(
            activeTopic = conversation?.activeTopic,
            recentConversation = conversation?.turns.orEmpty().filterNot(ContextTurn::sensitive)
                .takeLast(8).map { "${it.source.name.lowercase()}: ${it.text.take(300)}" },
            relevantEntities = conversation?.entities?.values.orEmpty().filterNot(ContextEntity::sensitive)
                .sortedByDescending(ContextEntity::updatedAt).take(12),
            attentionItems = notifications,
            activeWorkflowId = input.activeWorkflowId,
            workflowSummary = input.workflowSummary?.take(500),
            visionSummary = input.visionSummary?.take(500),
            nextCalendarEvent = input.nextCalendarEvent?.take(300),
            memoryFacts = input.memoryFacts.filter(String::isNotBlank).take(maxItems).map { it.take(300) },
            safeForSpeech = safeForSpeech,
            generatedAt = input.generatedAt
        )
    }
}
