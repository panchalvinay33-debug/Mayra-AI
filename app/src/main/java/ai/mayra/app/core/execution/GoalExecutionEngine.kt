package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.ExecutionPlan
import ai.mayra.app.core.planning.PlanExecutionReport
import ai.mayra.app.core.planning.PlanExecutor
import ai.mayra.app.core.planning.StepAction
import ai.mayra.app.core.planning.StepExecutionResult
import ai.mayra.app.core.planning.TaskPlanner
import java.util.UUID

/** Lifecycle state for a goal submitted to [GoalExecutionEngine]. */
enum class GoalState {
    QUEUED,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class GoalSession(
    val id: String,
    val goal: String,
    val state: GoalState,
    val createdAt: Long,
    val startedAt: Long? = null,
    val finishedAt: Long? = null,
    val plan: ExecutionPlan? = null,
    val report: PlanExecutionReport? = null,
    val failureMessage: String? = null
) {
    val isTerminal: Boolean
        get() = state == GoalState.COMPLETED ||
            state == GoalState.FAILED ||
            state == GoalState.CANCELLED
}

data class GoalExecutionSnapshot(
    val sessions: List<GoalSession>,
    val queuedCount: Int,
    val runningCount: Int,
    val completedCount: Int,
    val failedCount: Int,
    val cancelledCount: Int
)

/**
 * Coordinates planning and dependency-aware execution for user goals.
 *
 * The engine is deliberately framework-independent. Android services, workers, and UI layers can
 * submit goals and decide when to drain the queue. Execution remains deterministic and serial,
 * preventing two plans from competing for device actions at the same time.
 */
class GoalExecutionEngine(
    private val planner: TaskPlanner = TaskPlanner(),
    private val action: StepAction,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val historyLimit: Int = DEFAULT_HISTORY_LIMIT
) {
    init {
        require(historyLimit > 0) { "historyLimit must be greater than zero." }
    }

    private val sessions = linkedMapOf<String, GoalSession>()
    private val queue = ArrayDeque<String>()
    private var runningSessionId: String? = null

    @Synchronized
    fun submit(goal: String): GoalSession {
        val normalizedGoal = goal.trim().replace(WHITESPACE_REGEX, " ")
        require(normalizedGoal.isNotBlank()) { "Goal cannot be blank." }

        val session = GoalSession(
            id = idFactory(),
            goal = normalizedGoal,
            state = GoalState.QUEUED,
            createdAt = clock()
        )
        require(session.id.isNotBlank()) { "Generated goal ID cannot be blank." }
        require(session.id !in sessions) { "Generated goal ID must be unique." }

        sessions[session.id] = session
        queue.addLast(session.id)
        trimHistory()
        return session
    }

    @Synchronized
    fun get(sessionId: String): GoalSession? = sessions[sessionId]

    @Synchronized
    fun queued(): List<GoalSession> = queue.mapNotNull(sessions::get)

    @Synchronized
    fun cancel(sessionId: String): Boolean {
        val current = sessions[sessionId] ?: return false
        if (current.state != GoalState.QUEUED) return false

        queue.remove(sessionId)
        sessions[sessionId] = current.copy(
            state = GoalState.CANCELLED,
            finishedAt = clock()
        )
        trimHistory()
        return true
    }

    /** Executes the next queued goal, or returns null when the queue is empty. */
    suspend fun runNext(): GoalSession? {
        val running = beginNext() ?: return null
        return try {
            val plan = planner.plan(running.goal)
            updateRunning(running.id) { it.copy(plan = plan) }

            val report = PlanExecutor(action).execute(plan)
            val failed = report.results.filterIsInstance<StepExecutionResult.Failed>().firstOrNull()
            finish(
                sessionId = running.id,
                state = if (report.isSuccessful) GoalState.COMPLETED else GoalState.FAILED,
                report = report,
                failureMessage = failed?.error?.message
                    ?: report.results.filterIsInstance<StepExecutionResult.Blocked>()
                        .firstOrNull()
                        ?.let { "Execution blocked by: ${it.unmetDependencies.joinToString()}" }
            )
        } catch (error: Throwable) {
            finish(
                sessionId = running.id,
                state = GoalState.FAILED,
                failureMessage = error.message ?: "Goal execution failed."
            )
        }
    }

    /** Drains all goals that were queued when execution began, including newly queued goals. */
    suspend fun runUntilIdle(): List<GoalSession> {
        val completed = mutableListOf<GoalSession>()
        while (true) {
            completed += runNext() ?: break
        }
        return completed
    }

    @Synchronized
    fun snapshot(): GoalExecutionSnapshot {
        val values = sessions.values.toList()
        return GoalExecutionSnapshot(
            sessions = values,
            queuedCount = values.count { it.state == GoalState.QUEUED },
            runningCount = values.count { it.state == GoalState.RUNNING },
            completedCount = values.count { it.state == GoalState.COMPLETED },
            failedCount = values.count { it.state == GoalState.FAILED },
            cancelledCount = values.count { it.state == GoalState.CANCELLED }
        )
    }

    @Synchronized
    private fun beginNext(): GoalSession? {
        check(runningSessionId == null) { "A goal is already running." }
        val sessionId = queue.removeFirstOrNull() ?: return null
        val queued = requireNotNull(sessions[sessionId])
        val running = queued.copy(state = GoalState.RUNNING, startedAt = clock())
        sessions[sessionId] = running
        runningSessionId = sessionId
        return running
    }

    @Synchronized
    private fun updateRunning(sessionId: String, transform: (GoalSession) -> GoalSession) {
        check(runningSessionId == sessionId) { "Goal is not the active session." }
        sessions[sessionId] = transform(requireNotNull(sessions[sessionId]))
    }

    @Synchronized
    private fun finish(
        sessionId: String,
        state: GoalState,
        report: PlanExecutionReport? = null,
        failureMessage: String? = null
    ): GoalSession {
        check(state == GoalState.COMPLETED || state == GoalState.FAILED)
        check(runningSessionId == sessionId) { "Goal is not the active session." }

        val finished = requireNotNull(sessions[sessionId]).copy(
            state = state,
            finishedAt = clock(),
            report = report,
            failureMessage = failureMessage
        )
        sessions[sessionId] = finished
        runningSessionId = null
        trimHistory()
        return finished
    }

    private fun trimHistory() {
        if (sessions.size <= historyLimit) return

        val removableIds = sessions.values
            .filter(GoalSession::isTerminal)
            .sortedBy { it.finishedAt ?: Long.MAX_VALUE }
            .map { it.id }
            .iterator()

        while (sessions.size > historyLimit && removableIds.hasNext()) {
            sessions.remove(removableIds.next())
        }
    }

    private companion object {
        const val DEFAULT_HISTORY_LIMIT = 100
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}
