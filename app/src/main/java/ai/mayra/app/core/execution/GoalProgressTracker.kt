package ai.mayra.app.core.execution

/** Immutable UI-friendly view of one goal's latest execution progress. */
data class GoalProgressSnapshot(
    val sessionId: String,
    val goal: String? = null,
    val state: GoalState,
    val completedSteps: Int = 0,
    val totalSteps: Int = 0,
    val percentComplete: Int = 0,
    val currentStepId: String? = null,
    val currentStepTitle: String? = null,
    val currentCommand: String? = null,
    val queuePosition: Int? = null,
    val failureMessage: String? = null
)

/**
 * Converts [GoalExecutionEvent] callbacks into stable snapshots suitable for UI, notifications,
 * workers, and process-independent persistence adapters.
 */
class GoalProgressTracker : GoalExecutionEventListener {
    private val snapshots = linkedMapOf<String, GoalProgressSnapshot>()

    @Synchronized
    override fun onEvent(event: GoalExecutionEvent) {
        val previous = snapshots[event.sessionId]
        snapshots[event.sessionId] = when (event) {
            is GoalExecutionEvent.Queued -> GoalProgressSnapshot(
                sessionId = event.sessionId,
                goal = event.goal,
                state = GoalState.QUEUED,
                queuePosition = event.queuePosition
            )

            is GoalExecutionEvent.Restored -> (previous ?: GoalProgressSnapshot(
                sessionId = event.sessionId,
                state = GoalState.QUEUED
            )).copy(
                state = GoalState.QUEUED,
                queuePosition = event.queuePosition
            )

            is GoalExecutionEvent.Started -> (previous ?: GoalProgressSnapshot(
                sessionId = event.sessionId,
                goal = event.goal,
                state = GoalState.RUNNING
            )).copy(
                goal = event.goal,
                state = GoalState.RUNNING,
                queuePosition = null,
                failureMessage = null
            )

            is GoalExecutionEvent.Planned -> requirePrevious(event.sessionId).copy(
                totalSteps = event.totalSteps,
                percentComplete = calculatePercent(
                    completedSteps = previous?.completedSteps ?: 0,
                    totalSteps = event.totalSteps
                )
            )

            is GoalExecutionEvent.Progress -> applyStepProgress(
                requirePrevious(event.sessionId),
                event.event
            )

            is GoalExecutionEvent.Cancelled -> (previous ?: GoalProgressSnapshot(
                sessionId = event.sessionId,
                state = GoalState.CANCELLED
            )).copy(
                state = GoalState.CANCELLED,
                currentStepId = null,
                currentStepTitle = null,
                currentCommand = null,
                queuePosition = null
            )

            is GoalExecutionEvent.Finished -> requirePrevious(event.sessionId).copy(
                state = event.state,
                completedSteps = event.completedSteps,
                totalSteps = event.totalSteps,
                percentComplete = calculatePercent(event.completedSteps, event.totalSteps),
                currentStepId = null,
                currentStepTitle = null,
                currentCommand = null,
                queuePosition = null,
                failureMessage = event.failureMessage
            )
        }
    }

    @Synchronized
    fun get(sessionId: String): GoalProgressSnapshot? = snapshots[sessionId]

    @Synchronized
    fun snapshot(): List<GoalProgressSnapshot> = snapshots.values.toList()

    @Synchronized
    fun clearTerminal(): Int {
        val terminalIds = snapshots.values
            .filter { it.state == GoalState.COMPLETED || it.state == GoalState.FAILED || it.state == GoalState.CANCELLED }
            .map { it.sessionId }
        terminalIds.forEach(snapshots::remove)
        return terminalIds.size
    }

    private fun applyStepProgress(
        previous: GoalProgressSnapshot,
        event: StepProgressEvent
    ): GoalProgressSnapshot = when (event) {
        is StepProgressEvent.Started -> previous.copy(
            state = GoalState.RUNNING,
            completedSteps = event.completedSteps,
            totalSteps = event.totalSteps,
            percentComplete = calculatePercent(event.completedSteps, event.totalSteps),
            currentStepId = event.step.id,
            currentStepTitle = event.step.title,
            currentCommand = event.step.command
        )

        is StepProgressEvent.Completed -> previous.copy(
            completedSteps = event.completedSteps,
            totalSteps = event.totalSteps,
            percentComplete = calculatePercent(event.completedSteps, event.totalSteps),
            currentStepId = null,
            currentStepTitle = null,
            currentCommand = null
        )

        is StepProgressEvent.Failed -> previous.copy(
            completedSteps = event.completedSteps,
            totalSteps = event.totalSteps,
            percentComplete = calculatePercent(event.completedSteps, event.totalSteps),
            currentStepId = event.step.id,
            currentStepTitle = event.step.title,
            currentCommand = event.step.command,
            failureMessage = event.error.message
        )

        is StepProgressEvent.Cancelled -> previous.copy(
            state = GoalState.CANCELLED,
            completedSteps = event.completedSteps,
            totalSteps = event.totalSteps,
            percentComplete = calculatePercent(event.completedSteps, event.totalSteps),
            currentStepId = null,
            currentStepTitle = null,
            currentCommand = null
        )
    }

    private fun requirePrevious(sessionId: String): GoalProgressSnapshot =
        requireNotNull(snapshots[sessionId]) { "No progress snapshot exists for session $sessionId." }

    private companion object {
        fun calculatePercent(completedSteps: Int, totalSteps: Int): Int {
            if (totalSteps <= 0) return 0
            return ((completedSteps.coerceIn(0, totalSteps) * 100) / totalSteps)
        }
    }
}
