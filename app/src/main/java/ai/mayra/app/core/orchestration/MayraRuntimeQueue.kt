package ai.mayra.app.core.orchestration

import ai.mayra.app.core.runtime.RuntimeKernel
import ai.mayra.app.core.runtime.RuntimeSnapshot
import ai.mayra.app.core.runtime.RuntimeTask
import ai.mayra.app.core.runtime.TaskPriority

/**
 * Queues assistant turns and multi-step goals through [RuntimeKernel].
 *
 * Submission is separated from execution so UI, voice, and background surfaces can enqueue work,
 * cancel work that has not started, and drive one task or the entire queue without duplicating
 * scheduling logic.
 */
class MayraRuntimeQueue(
    private val orchestrator: MayraAiOrchestrator = MayraAiOrchestrator(),
    private val runtime: RuntimeKernel = RuntimeKernel(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val results = linkedMapOf<String, QueuedOrchestrationResult>()

    suspend fun submitText(
        input: String,
        priority: TaskPriority = TaskPriority.NORMAL
    ): OrchestrationTicket {
        val normalized = normalize(input)
        require(normalized.isNotBlank()) { "Input cannot be blank." }

        val task = RuntimeTask(
            name = TEXT_TASK_NAME,
            priority = priority,
            createdAt = clock()
        ) {
            storeResult(
                taskId = currentTaskId.getValue(),
                result = QueuedOrchestrationResult.Text(orchestrator.processText(normalized))
            )
        }
        currentTaskId.setValue(task.id)
        runtime.submit(task)
        storeResult(task.id, QueuedOrchestrationResult.Pending)
        return OrchestrationTicket(task.id, TEXT_TASK_NAME, priority, task.createdAt)
    }

    suspend fun submitGoal(
        goal: String,
        priority: TaskPriority = TaskPriority.NORMAL
    ): OrchestrationTicket {
        val normalized = normalize(goal)
        require(normalized.isNotBlank()) { "Goal cannot be blank." }

        val task = RuntimeTask(
            name = GOAL_TASK_NAME,
            priority = priority,
            createdAt = clock()
        ) {
            storeResult(
                taskId = currentTaskId.getValue(),
                result = QueuedOrchestrationResult.Goal(orchestrator.processGoal(normalized))
            )
        }
        currentTaskId.setValue(task.id)
        runtime.submit(task)
        storeResult(task.id, QueuedOrchestrationResult.Pending)
        return OrchestrationTicket(task.id, GOAL_TASK_NAME, priority, task.createdAt)
    }

    suspend fun cancel(taskId: String): Boolean {
        require(taskId.isNotBlank()) { "Task id cannot be blank." }
        val cancelled = runtime.cancel(taskId)
        if (cancelled) storeResult(taskId, QueuedOrchestrationResult.Cancelled)
        return cancelled
    }

    suspend fun runNext(): Boolean = runtime.runNext()

    suspend fun drain(): Int = runtime.drain()

    suspend fun runtimeSnapshot(): RuntimeSnapshot = runtime.snapshot()

    @Synchronized
    fun result(taskId: String): QueuedOrchestrationResult? = results[taskId]

    @Synchronized
    fun resultSnapshot(): Map<String, QueuedOrchestrationResult> = results.toMap()

    @Synchronized
    fun clearFinishedResults(): Int {
        val finishedIds = results
            .filterValues { it !is QueuedOrchestrationResult.Pending }
            .keys
            .toList()
        finishedIds.forEach(results::remove)
        return finishedIds.size
    }

    @Synchronized
    private fun storeResult(taskId: String, result: QueuedOrchestrationResult) {
        results[taskId] = result
    }

    private fun normalize(value: String): String = value.trim().replace(WHITESPACE_REGEX, " ")

    /**
     * RuntimeKernel invokes one task at a time. This holder lets the task block reference its own
     * immutable id without exposing mutable variables to callers.
     */
    private class TaskIdHolder {
        private var value: String? = null
        fun setValue(taskId: String) { value = taskId }
        fun getValue(): String = requireNotNull(value)
    }

    private val currentTaskId = TaskIdHolder()

    private companion object {
        const val TEXT_TASK_NAME = "assistant-turn"
        const val GOAL_TASK_NAME = "assistant-goal"
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}

data class OrchestrationTicket(
    val taskId: String,
    val taskName: String,
    val priority: TaskPriority,
    val createdAt: Long
)

sealed interface QueuedOrchestrationResult {
    data object Pending : QueuedOrchestrationResult
    data object Cancelled : QueuedOrchestrationResult
    data class Text(val result: OrchestrationResult) : QueuedOrchestrationResult
    data class Goal(val result: GoalOrchestrationResult) : QueuedOrchestrationResult
}
