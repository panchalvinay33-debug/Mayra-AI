package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Instant
import java.util.UUID

enum class ConversationSessionStatus {
    ACTIVE,
    CLOSED
}

data class ConversationSession(
    val id: String,
    val title: String?,
    val status: ConversationSessionStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Owns conversation-session lifecycle while delegating bounded message storage to
 * [ConversationContextManager]. The class is framework independent and safe to use
 * from Android, JVM tests, or a future server runtime.
 */
class ConversationSessionManager(
    private val contextManager: ConversationContextManager = ConversationContextManager(),
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    private val sessions = linkedMapOf<String, ConversationSession>()

    @Synchronized
    fun create(
        title: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): ConversationSession {
        val normalizedTitle = title?.trim()?.takeIf { it.isNotEmpty() }
        val id = idFactory().also {
            require(it.isNotBlank()) { "Generated session id cannot be blank." }
            require(it !in sessions) { "Generated session id already exists: $it" }
        }
        val now = clock.instant()
        return ConversationSession(
            id = id,
            title = normalizedTitle,
            status = ConversationSessionStatus.ACTIVE,
            createdAt = now,
            updatedAt = now,
            metadata = metadata.toMap()
        ).also { sessions[id] = it }
    }

    @Synchronized
    fun get(sessionId: String): ConversationSession? = sessions[sessionId]

    @Synchronized
    fun requireActive(sessionId: String): ConversationSession {
        val session = sessions[sessionId]
            ?: throw NoSuchElementException("Conversation session not found: $sessionId")
        check(session.status == ConversationSessionStatus.ACTIVE) {
            "Conversation session is closed: $sessionId"
        }
        return session
    }

    @Synchronized
    fun append(
        sessionId: String,
        message: ConversationMessage
    ): ConversationContextSnapshot {
        val session = requireActive(sessionId)
        val snapshot = contextManager.append(sessionId, message)
        sessions[sessionId] = session.copy(updatedAt = clock.instant())
        return snapshot
    }

    @Synchronized
    fun snapshot(sessionId: String): ConversationContextSnapshot {
        requireSession(sessionId)
        return contextManager.snapshot(sessionId)
    }

    @Synchronized
    fun updateMetadata(
        sessionId: String,
        metadata: Map<String, String>
    ): ConversationSession {
        val session = requireActive(sessionId)
        return session.copy(
            updatedAt = clock.instant(),
            metadata = session.metadata + metadata
        ).also { sessions[sessionId] = it }
    }

    @Synchronized
    fun rename(sessionId: String, title: String?): ConversationSession {
        val session = requireActive(sessionId)
        val normalizedTitle = title?.trim()?.takeIf { it.isNotEmpty() }
        return session.copy(
            title = normalizedTitle,
            updatedAt = clock.instant()
        ).also { sessions[sessionId] = it }
    }

    @Synchronized
    fun close(sessionId: String): ConversationSession {
        val session = requireActive(sessionId)
        return session.copy(
            status = ConversationSessionStatus.CLOSED,
            updatedAt = clock.instant()
        ).also { sessions[sessionId] = it }
    }

    @Synchronized
    fun delete(sessionId: String): Boolean {
        val removed = sessions.remove(sessionId) != null
        if (removed) contextManager.clear(sessionId)
        return removed
    }

    @Synchronized
    fun list(
        status: ConversationSessionStatus? = null
    ): List<ConversationSession> = sessions.values
        .asSequence()
        .filter { status == null || it.status == status }
        .sortedWith(compareByDescending<ConversationSession> { it.updatedAt }.thenBy { it.id })
        .toList()

    private fun requireSession(sessionId: String): ConversationSession {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
        return sessions[sessionId]
            ?: throw NoSuchElementException("Conversation session not found: $sessionId")
    }
}