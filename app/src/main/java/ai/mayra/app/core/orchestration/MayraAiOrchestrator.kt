package ai.mayra.app.core.orchestration

import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.context.ConversationContextSnapshot
import ai.mayra.app.core.context.ConversationContextStore
import ai.mayra.app.core.context.ConversationRole
import ai.mayra.app.core.memory.LongTermMemoryEngine
import ai.mayra.app.core.memory.MemoryKind
import ai.mayra.app.core.memory.MemorySearchResult
import ai.mayra.app.core.planning.ExecutionPlan
import ai.mayra.app.core.planning.PlanExecutionReport
import ai.mayra.app.core.planning.PlanExecutor
import ai.mayra.app.core.planning.StepAction
import ai.mayra.app.core.planning.TaskPlanner

/**
 * Framework-independent coordinator for Mayra's local assistant brain.
 *
 * It normalizes input, stores bounded conversation context, recalls relevant durable memories,
 * delegates deterministic responses to [LocalCommandEngine], executes multi-step goals through
 * [TaskPlanner], and records goal outcomes in long-term memory.
 */
class MayraAiOrchestrator(
    private val commandEngine: LocalCommandEngine = LocalCommandEngine(),
    private val contextStore: ConversationContextStore = ConversationContextStore(),
    private val memoryEngine: LongTermMemoryEngine = LongTermMemoryEngine(),
    private val planner: TaskPlanner = TaskPlanner(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun processText(input: String): OrchestrationResult {
        val normalized = normalize(input)
        if (normalized.isBlank()) {
            return OrchestrationResult.Rejected("Please say or type a command.")
        }

        val timestamp = clock()
        contextStore.append(ConversationRole.USER, normalized, timestamp)
        val memories = memoryEngine.search(normalized, limit = MAX_RECALLED_MEMORIES)

        return try {
            val response = commandEngine.respond(normalized)
            contextStore.append(ConversationRole.ASSISTANT, response, clock())
            OrchestrationResult.Completed(
                input = normalized,
                response = response,
                recalledMemories = memories
            )
        } catch (error: Throwable) {
            OrchestrationResult.Failed(
                input = normalized,
                message = error.message ?: "Mayra could not process that request.",
                cause = error
            )
        }
    }

    suspend fun processGoal(goal: String): GoalOrchestrationResult {
        val normalized = normalize(goal)
        if (normalized.isBlank()) {
            return GoalOrchestrationResult.Rejected("Please provide a goal.")
        }

        val plan = planner.plan(normalized)
        val executor = PlanExecutor(
            StepAction { step ->
                when (val result = processText(step.command)) {
                    is OrchestrationResult.Completed -> result.response
                    is OrchestrationResult.Rejected -> error(result.reason)
                    is OrchestrationResult.Failed -> throw result.cause
                }
            }
        )
        val report = executor.execute(plan)
        recordGoalOutcome(plan, report)

        return if (report.isSuccessful) {
            GoalOrchestrationResult.Completed(report)
        } else {
            GoalOrchestrationResult.Failed(
                report = report,
                message = "Mayra completed ${report.completedSteps}/${plan.steps.size} steps."
            )
        }
    }

    fun remember(
        namespace: String,
        key: String,
        value: String,
        kind: MemoryKind = MemoryKind.OTHER,
        confidence: Double = 1.0,
        source: String? = null
    ) = memoryEngine.remember(
        namespace = namespace,
        key = key,
        value = value,
        kind = kind,
        confidence = confidence,
        timestamp = clock(),
        source = source
    )

    fun contextSnapshot(): ConversationContextSnapshot = contextStore.snapshot()

    fun memorySnapshot() = memoryEngine.snapshot()

    private fun recordGoalOutcome(plan: ExecutionPlan, report: PlanExecutionReport) {
        val status = if (report.isSuccessful) "completed" else "incomplete"
        memoryEngine.remember(
            namespace = GOAL_MEMORY_NAMESPACE,
            key = goalMemoryKey(plan.goal),
            value = buildString {
                append(plan.goal)
                append("; status=").append(status)
                append("; completed=").append(report.completedSteps).append('/').append(plan.steps.size)
                append("; failed=").append(report.failedSteps)
                append("; blocked=").append(report.blockedSteps)
            },
            kind = MemoryKind.PROJECT,
            confidence = 1.0,
            timestamp = clock(),
            source = "goal-execution"
        )
    }

    private fun goalMemoryKey(goal: String): String = goal
        .lowercase()
        .replace(NON_WORD_REGEX, "_")
        .trim('_')
        .take(MAX_GOAL_KEY_LENGTH)
        .ifBlank { "goal_${clock()}" }

    private fun normalize(value: String): String = value.trim().replace(WHITESPACE_REGEX, " ")

    private companion object {
        const val MAX_RECALLED_MEMORIES = 5
        const val MAX_GOAL_KEY_LENGTH = 80
        const val GOAL_MEMORY_NAMESPACE = "goal_history"
        val WHITESPACE_REGEX = Regex("\\s+")
        val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}]+")
    }
}

sealed interface OrchestrationResult {
    data class Completed(
        val input: String,
        val response: String,
        val recalledMemories: List<MemorySearchResult>
    ) : OrchestrationResult

    data class Rejected(val reason: String) : OrchestrationResult

    data class Failed(
        val input: String,
        val message: String,
        val cause: Throwable
    ) : OrchestrationResult
}

sealed interface GoalOrchestrationResult {
    data class Completed(val report: PlanExecutionReport) : GoalOrchestrationResult
    data class Failed(val report: PlanExecutionReport, val message: String) : GoalOrchestrationResult
    data class Rejected(val reason: String) : GoalOrchestrationResult
}
