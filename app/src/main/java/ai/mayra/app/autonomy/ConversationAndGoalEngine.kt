package ai.mayra.app.autonomy

import android.content.Context
import java.util.UUID

enum class ConversationRole { USER, ASSISTANT, SYSTEM }
enum class ConversationState { ACTIVE, WAITING_FOR_USER, PAUSED, COMPLETED, FAILED }

data class ConversationTurn(
    val id: String = UUID.randomUUID().toString(),
    val role: ConversationRole,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String? = null,
    val attributes: Map<String, String> = emptyMap()
)

data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val state: ConversationState = ConversationState.ACTIVE,
    val summary: String = "",
    val pendingQuestion: String? = null,
    val activeGoalId: String? = null,
    val turns: List<ConversationTurn> = emptyList()
)

class ConversationStateEngine(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun create(title: String, now: Long = System.currentTimeMillis()): ConversationSession {
        require(title.isNotBlank())
        return ConversationSession(title = title.trim().take(120), createdAt = now, updatedAt = now).also(::upsert)
    }

    @Synchronized
    fun append(
        sessionId: String,
        turn: ConversationTurn,
        maxTurns: Int = 80
    ): ConversationSession {
        require(maxTurns > 0)
        val session = get(sessionId) ?: error("Unknown conversation: $sessionId")
        val updated = session.copy(
            updatedAt = maxOf(session.updatedAt, turn.timestamp),
            state = if (turn.role == ConversationRole.ASSISTANT && turn.attributes["expects_reply"] == "true") {
                ConversationState.WAITING_FOR_USER
            } else ConversationState.ACTIVE,
            pendingQuestion = if (turn.attributes["expects_reply"] == "true") turn.text else if (turn.role == ConversationRole.USER) null else session.pendingQuestion,
            turns = (session.turns + turn).takeLast(maxTurns)
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun summarize(sessionId: String, summary: String): ConversationSession {
        val session = get(sessionId) ?: error("Unknown conversation: $sessionId")
        val updated = session.copy(summary = summary.trim().take(1500), updatedAt = System.currentTimeMillis())
        upsert(updated)
        return updated
    }

    @Synchronized
    fun attachGoal(sessionId: String, goalId: String?): ConversationSession {
        val session = get(sessionId) ?: error("Unknown conversation: $sessionId")
        val updated = session.copy(activeGoalId = goalId, updatedAt = System.currentTimeMillis())
        upsert(updated)
        return updated
    }

    @Synchronized
    fun setState(sessionId: String, state: ConversationState): ConversationSession {
        val session = get(sessionId) ?: error("Unknown conversation: $sessionId")
        val updated = session.copy(state = state, updatedAt = System.currentTimeMillis())
        upsert(updated)
        return updated
    }

    fun get(id: String): ConversationSession? = snapshot().firstOrNull { it.id == id }

    fun recent(limit: Int = 10): List<ConversationSession> = snapshot()
        .sortedByDescending(ConversationSession::updatedAt)
        .take(limit.coerceAtLeast(0))

    fun snapshot(): List<ConversationSession> = preferences.getStringSet(KEY_SESSIONS, emptySet()).orEmpty()
        .mapNotNull(::decodeSession)

    @Synchronized
    fun prune(maxSessions: Int = 30, maxAgeMillis: Long = 30L * 24 * 60 * 60 * 1000) {
        require(maxSessions > 0)
        val cutoff = System.currentTimeMillis() - maxAgeMillis.coerceAtLeast(0)
        val retained = snapshot()
            .filter { it.state == ConversationState.ACTIVE || it.updatedAt >= cutoff }
            .sortedByDescending(ConversationSession::updatedAt)
            .take(maxSessions)
        save(retained)
    }

    private fun upsert(session: ConversationSession) = save(snapshot().filterNot { it.id == session.id } + session)

    private fun save(sessions: List<ConversationSession>) {
        preferences.edit().putStringSet(KEY_SESSIONS, sessions.map(::encodeSession).toSet()).apply()
    }

    private fun encodeSession(session: ConversationSession): String {
        val turns = session.turns.joinToString(TURN_SEPARATOR) { turn ->
            listOf(
                turn.id,
                turn.role.name,
                turn.text,
                turn.timestamp,
                turn.intent.orEmpty(),
                encodeMap(turn.attributes)
            ).joinToString(FIELD_SEPARATOR) { value -> sanitize(value.toString()) }
        }
        return listOf(
            session.id,
            session.title,
            session.createdAt,
            session.updatedAt,
            session.state.name,
            session.summary,
            session.pendingQuestion.orEmpty(),
            session.activeGoalId.orEmpty(),
            turns
        ).joinToString(SESSION_SEPARATOR) { value -> sanitizeSession(value.toString()) }
    }

    private fun decodeSession(raw: String): ConversationSession? {
        val p = raw.split(SESSION_SEPARATOR)
        if (p.size != 9) return null
        val turns = if (p[8].isBlank()) emptyList() else p[8].split(TURN_SEPARATOR).mapNotNull(::decodeTurn)
        return ConversationSession(
            id = p[0], title = p[1], createdAt = p[2].toLongOrNull() ?: return null,
            updatedAt = p[3].toLongOrNull() ?: return null,
            state = runCatching { ConversationState.valueOf(p[4]) }.getOrNull() ?: return null,
            summary = p[5], pendingQuestion = p[6].ifBlank { null },
            activeGoalId = p[7].ifBlank { null }, turns = turns
        )
    }

    private fun decodeTurn(raw: String): ConversationTurn? {
        val p = raw.split(FIELD_SEPARATOR)
        if (p.size != 6) return null
        return ConversationTurn(
            id = p[0], role = runCatching { ConversationRole.valueOf(p[1]) }.getOrNull() ?: return null,
            text = p[2], timestamp = p[3].toLongOrNull() ?: return null,
            intent = p[4].ifBlank { null }, attributes = decodeMap(p[5])
        )
    }

    private fun encodeMap(map: Map<String, String>) = map.entries.joinToString(MAP_ENTRY_SEPARATOR) {
        "${sanitize(it.key)}$MAP_VALUE_SEPARATOR${sanitize(it.value)}"
    }

    private fun decodeMap(raw: String): Map<String, String> = if (raw.isBlank()) emptyMap() else raw
        .split(MAP_ENTRY_SEPARATOR)
        .mapNotNull { entry ->
            val i = entry.indexOf(MAP_VALUE_SEPARATOR)
            if (i <= 0) null else entry.substring(0, i) to entry.substring(i + MAP_VALUE_SEPARATOR.length)
        }.toMap()

    private fun sanitize(value: String) = value
        .replace(SESSION_SEPARATOR, " ").replace(TURN_SEPARATOR, " ")
        .replace(FIELD_SEPARATOR, " ").replace(MAP_ENTRY_SEPARATOR, " ")
        .replace(MAP_VALUE_SEPARATOR, " ").take(2000)
    private fun sanitizeSession(value: String) = value.replace(SESSION_SEPARATOR, " ")

    private companion object {
        const val FILE_NAME = "mayra_conversations"
        const val KEY_SESSIONS = "sessions"
        const val SESSION_SEPARATOR = "\u0018"
        const val TURN_SEPARATOR = "\u0019"
        const val FIELD_SEPARATOR = "\u001A"
        const val MAP_ENTRY_SEPARATOR = "\u001B"
        const val MAP_VALUE_SEPARATOR = "\u001C"
    }
}

enum class GoalState { DRAFT, ACTIVE, PAUSED, BLOCKED, COMPLETED, FAILED, CANCELLED }
enum class GoalPriority { LOW, NORMAL, HIGH, CRITICAL }

data class GoalMilestone(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val completed: Boolean = false,
    val completedAt: Long? = null
)

data class MayraGoal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val state: GoalState = GoalState.DRAFT,
    val priority: GoalPriority = GoalPriority.NORMAL,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val dueAt: Long? = null,
    val parentGoalId: String? = null,
    val progressPercent: Int = 0,
    val milestones: List<GoalMilestone> = emptyList(),
    val tags: Set<String> = emptySet(),
    val lastError: String? = null
)

class GoalEngine(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun create(goal: MayraGoal): MayraGoal {
        require(goal.title.isNotBlank())
        require(goal.progressPercent in 0..100)
        require(goal.milestones.map { it.id }.distinct().size == goal.milestones.size)
        val normalized = goal.copy(title = goal.title.trim().take(160), state = if (goal.state == GoalState.DRAFT) GoalState.ACTIVE else goal.state)
        save(snapshot().filterNot { it.id == normalized.id } + normalized)
        return normalized
    }

    fun get(id: String): MayraGoal? = snapshot().firstOrNull { it.id == id }
    fun snapshot(): List<MayraGoal> = preferences.getStringSet(KEY_GOALS, emptySet()).orEmpty().mapNotNull(::decode)
    fun active(): List<MayraGoal> = snapshot().filter { it.state in setOf(GoalState.ACTIVE, GoalState.BLOCKED, GoalState.PAUSED) }
        .sortedWith(compareByDescending<MayraGoal> { it.priority }.thenBy { it.dueAt ?: Long.MAX_VALUE })

    @Synchronized
    fun pause(id: String): MayraGoal = transition(id, setOf(GoalState.ACTIVE, GoalState.BLOCKED), GoalState.PAUSED)
    @Synchronized
    fun resume(id: String): MayraGoal = transition(id, setOf(GoalState.PAUSED, GoalState.BLOCKED), GoalState.ACTIVE)
    @Synchronized
    fun cancel(id: String): MayraGoal = transition(id, GoalState.entries.toSet() - GoalState.COMPLETED, GoalState.CANCELLED)

    @Synchronized
    fun completeMilestone(goalId: String, milestoneId: String, now: Long = System.currentTimeMillis()): MayraGoal {
        val goal = get(goalId) ?: error("Unknown goal: $goalId")
        require(goal.state !in TERMINAL_STATES)
        require(goal.milestones.any { it.id == milestoneId })
        val milestones = goal.milestones.map { if (it.id == milestoneId) it.copy(completed = true, completedAt = now) else it }
        val completed = milestones.count(GoalMilestone::completed)
        val progress = if (milestones.isEmpty()) goal.progressPercent else completed * 100 / milestones.size
        val updated = goal.copy(
            milestones = milestones,
            progressPercent = progress,
            state = if (progress == 100) GoalState.COMPLETED else GoalState.ACTIVE,
            updatedAt = now,
            lastError = null
        )
        upsert(updated)
        return updated
    }

    @Synchronized
    fun reportProgress(id: String, percent: Int, now: Long = System.currentTimeMillis()): MayraGoal {
        require(percent in 0..100)
        val goal = get(id) ?: error("Unknown goal: $id")
        require(goal.state !in TERMINAL_STATES)
        return goal.copy(
            progressPercent = maxOf(goal.progressPercent, percent),
            state = if (percent == 100) GoalState.COMPLETED else GoalState.ACTIVE,
            updatedAt = now,
            lastError = null
        ).also(::upsert)
    }

    @Synchronized
    fun block(id: String, reason: String): MayraGoal {
        val goal = get(id) ?: error("Unknown goal: $id")
        return goal.copy(state = GoalState.BLOCKED, lastError = reason.take(300), updatedAt = System.currentTimeMillis()).also(::upsert)
    }

    fun diagnostics(now: Long = System.currentTimeMillis()): GoalDiagnostics {
        val all = snapshot()
        return GoalDiagnostics(
            total = all.size,
            active = all.count { it.state == GoalState.ACTIVE },
            paused = all.count { it.state == GoalState.PAUSED },
            blocked = all.count { it.state == GoalState.BLOCKED },
            completed = all.count { it.state == GoalState.COMPLETED },
            overdue = all.count { it.dueAt?.let { due -> due < now } == true && it.state !in TERMINAL_STATES },
            averageProgress = if (all.isEmpty()) 0 else all.sumOf(MayraGoal::progressPercent) / all.size
        )
    }

    private fun transition(id: String, allowed: Set<GoalState>, target: GoalState): MayraGoal {
        val goal = get(id) ?: error("Unknown goal: $id")
        require(goal.state in allowed) { "Cannot move ${goal.state} to $target" }
        return goal.copy(state = target, updatedAt = System.currentTimeMillis()).also(::upsert)
    }

    private fun upsert(goal: MayraGoal) = save(snapshot().filterNot { it.id == goal.id } + goal)
    private fun save(goals: List<MayraGoal>) = preferences.edit().putStringSet(KEY_GOALS, goals.map(::encode).toSet()).apply()

    private fun encode(goal: MayraGoal): String {
        val milestones = goal.milestones.joinToString(MILESTONE_SEPARATOR) {
            listOf(it.id, it.title, it.completed, it.completedAt ?: -1L)
                .joinToString(FIELD_SEPARATOR) { value -> sanitize(value.toString()) }
        }
        return listOf(
            goal.id,
            goal.title,
            goal.description,
            goal.state.name,
            goal.priority.name,
            goal.createdAt,
            goal.updatedAt,
            goal.dueAt ?: -1L,
            goal.parentGoalId.orEmpty(),
            goal.progressPercent,
            milestones,
            goal.tags.joinToString(TAG_SEPARATOR),
            goal.lastError.orEmpty()
        ).joinToString(GOAL_SEPARATOR) { value -> sanitizeGoal(value.toString()) }
    }

    private fun decode(raw: String): MayraGoal? {
        val p = raw.split(GOAL_SEPARATOR)
        if (p.size != 13) return null
        val milestones = if (p[10].isBlank()) emptyList() else p[10].split(MILESTONE_SEPARATOR).mapNotNull { m ->
            val x = m.split(FIELD_SEPARATOR)
            if (x.size != 4) null else GoalMilestone(x[0], x[1], x[2].toBooleanStrictOrNull() ?: return@mapNotNull null, x[3].toLongOrNull()?.takeIf { it >= 0 })
        }
        return MayraGoal(
            id = p[0], title = p[1], description = p[2],
            state = runCatching { GoalState.valueOf(p[3]) }.getOrNull() ?: return null,
            priority = runCatching { GoalPriority.valueOf(p[4]) }.getOrNull() ?: return null,
            createdAt = p[5].toLongOrNull() ?: return null, updatedAt = p[6].toLongOrNull() ?: return null,
            dueAt = p[7].toLongOrNull()?.takeIf { it >= 0 }, parentGoalId = p[8].ifBlank { null },
            progressPercent = p[9].toIntOrNull() ?: return null, milestones = milestones,
            tags = p[11].split(TAG_SEPARATOR).filter(String::isNotBlank).toSet(), lastError = p[12].ifBlank { null }
        )
    }

    private fun sanitize(value: String) = value.replace(GOAL_SEPARATOR, " ").replace(MILESTONE_SEPARATOR, " ")
        .replace(FIELD_SEPARATOR, " ").replace(TAG_SEPARATOR, " ").take(1500)
    private fun sanitizeGoal(value: String) = value.replace(GOAL_SEPARATOR, " ")

    private companion object {
        const val FILE_NAME = "mayra_goals"
        const val KEY_GOALS = "goals"
        const val GOAL_SEPARATOR = "\u0014"
        const val MILESTONE_SEPARATOR = "\u0015"
        const val FIELD_SEPARATOR = "\u0016"
        const val TAG_SEPARATOR = "\u0017"
        val TERMINAL_STATES = setOf(GoalState.COMPLETED, GoalState.FAILED, GoalState.CANCELLED)
    }
}

data class GoalDiagnostics(
    val total: Int,
    val active: Int,
    val paused: Int,
    val blocked: Int,
    val completed: Int,
    val overdue: Int,
    val averageProgress: Int
)
