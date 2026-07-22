package ai.mayra.app.core.planning

import ai.mayra.app.core.runtime.TaskPriority
import java.util.UUID

/** A single executable unit produced by [TaskPlanner]. */
data class PlannedStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val command: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val dependsOn: Set<String> = emptySet(),
    val maxAttempts: Int = 1
) {
    init {
        require(title.isNotBlank()) { "Step title cannot be blank." }
        require(command.isNotBlank()) { "Step command cannot be blank." }
        require(maxAttempts > 0) { "Step maxAttempts must be greater than zero." }
        require(id !in dependsOn) { "A step cannot depend on itself." }
    }
}

data class ExecutionPlan(
    val goal: String,
    val steps: List<PlannedStep>
) {
    init {
        require(goal.isNotBlank()) { "Goal cannot be blank." }
        require(steps.isNotEmpty()) { "A plan must contain at least one step." }
        require(steps.map { it.id }.toSet().size == steps.size) { "Step IDs must be unique." }

        val knownIds = steps.mapTo(mutableSetOf()) { it.id }
        val missingDependencies = steps.flatMap { it.dependsOn }.filterNot { it in knownIds }
        require(missingDependencies.isEmpty()) {
            "Plan contains unknown dependencies: ${missingDependencies.distinct().joinToString()}"
        }
        require(!containsDependencyCycle(steps)) { "Plan dependencies must not contain a cycle." }
    }

    fun readySteps(completedStepIds: Set<String>): List<PlannedStep> = steps
        .filterNot { it.id in completedStepIds }
        .filter { completedStepIds.containsAll(it.dependsOn) }
        .sortedWith(
            compareByDescending<PlannedStep> { it.priority.weight }
                .thenBy { steps.indexOf(it) }
        )

    private companion object {
        fun containsDependencyCycle(steps: List<PlannedStep>): Boolean {
            val byId = steps.associateBy { it.id }
            val visiting = mutableSetOf<String>()
            val visited = mutableSetOf<String>()

            fun visit(id: String): Boolean {
                if (id in visiting) return true
                if (id in visited) return false

                visiting += id
                val cyclic = byId.getValue(id).dependsOn.any(::visit)
                visiting -= id
                visited += id
                return cyclic
            }

            return steps.any { visit(it.id) }
        }
    }
}

fun interface StepAction {
    suspend fun execute(step: PlannedStep): String
}

sealed interface StepExecutionResult {
    val step: PlannedStep
    val attempts: Int

    data class Completed(
        override val step: PlannedStep,
        override val attempts: Int,
        val output: String
    ) : StepExecutionResult

    data class Failed(
        override val step: PlannedStep,
        override val attempts: Int,
        val error: Throwable
    ) : StepExecutionResult

    data class Blocked(
        override val step: PlannedStep,
        override val attempts: Int = 0,
        val unmetDependencies: Set<String>
    ) : StepExecutionResult
}

data class PlanExecutionReport(
    val plan: ExecutionPlan,
    val results: List<StepExecutionResult>
) {
    val completedSteps: Int get() = results.count { it is StepExecutionResult.Completed }
    val failedSteps: Int get() = results.count { it is StepExecutionResult.Failed }
    val blockedSteps: Int get() = results.count { it is StepExecutionResult.Blocked }
    val isSuccessful: Boolean get() = completedSteps == plan.steps.size
}

/**
 * Executes dependency-aware plans deterministically and retries each step up to its configured limit.
 * Framework integrations can adapt Android actions or remote AI calls through [StepAction].
 */
class PlanExecutor(
    private val action: StepAction
) {
    suspend fun execute(plan: ExecutionPlan): PlanExecutionReport {
        val completed = mutableSetOf<String>()
        val results = mutableListOf<StepExecutionResult>()
        val pending = plan.steps.toMutableList()

        while (pending.isNotEmpty()) {
            val ready = plan.readySteps(completed).filter { it in pending }
            if (ready.isEmpty()) break

            ready.forEach { step ->
                val result = executeWithRetry(step)
                results += result
                pending -= step
                if (result is StepExecutionResult.Completed) completed += step.id
            }
        }

        pending.forEach { step ->
            results += StepExecutionResult.Blocked(
                step = step,
                unmetDependencies = step.dependsOn - completed
            )
        }

        return PlanExecutionReport(plan, results)
    }

    private suspend fun executeWithRetry(step: PlannedStep): StepExecutionResult {
        var lastFailure: Throwable? = null
        repeat(step.maxAttempts) { attemptIndex ->
            try {
                return StepExecutionResult.Completed(
                    step = step,
                    attempts = attemptIndex + 1,
                    output = action.execute(step)
                )
            } catch (error: Throwable) {
                lastFailure = error
            }
        }
        return StepExecutionResult.Failed(
            step = step,
            attempts = step.maxAttempts,
            error = requireNotNull(lastFailure)
        )
    }
}

/**
 * Creates conservative local plans without depending on an LLM. More advanced planners can implement
 * the same contract later while preserving validation and execution semantics.
 */
class TaskPlanner {
    fun plan(goal: String): ExecutionPlan {
        val normalizedGoal = goal.trim().replace(WHITESPACE_REGEX, " ")
        require(normalizedGoal.isNotBlank()) { "Goal cannot be blank." }

        val commands = normalizedGoal
            .split(SEPARATOR_REGEX)
            .map(String::trim)
            .filter(String::isNotBlank)

        val steps = if (commands.size <= 1) {
            listOf(
                PlannedStep(
                    title = normalizedGoal,
                    command = normalizedGoal
                )
            )
        } else {
            val generated = mutableListOf<PlannedStep>()
            commands.forEachIndexed { index, command ->
                generated += PlannedStep(
                    title = "Step ${index + 1}: $command",
                    command = command,
                    dependsOn = generated.lastOrNull()?.let { setOf(it.id) }.orEmpty()
                )
            }
            generated
        }

        return ExecutionPlan(goal = normalizedGoal, steps = steps)
    }

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
        val SEPARATOR_REGEX = Regex("\\s*(?:,?\\bthen\\b|;|\\n)\\s*", RegexOption.IGNORE_CASE)
    }
}
