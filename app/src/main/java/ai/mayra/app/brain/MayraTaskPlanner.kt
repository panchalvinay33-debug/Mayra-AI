package ai.mayra.app.brain

import java.util.UUID

enum class PlanState { DRAFT, READY, RUNNING, BLOCKED, COMPLETED, FAILED, CANCELLED }
enum class PlanStepState { WAITING, READY, RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED }
enum class FailurePolicy { STOP_PLAN, CONTINUE, RETRY }

data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val order: Int,
    val intent: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap(),
    val dependsOn: Set<String> = emptySet(),
    val requiresConfirmation: Boolean = false,
    val failurePolicy: FailurePolicy = FailurePolicy.STOP_PLAN,
    val maxAttempts: Int = 3,
    val attempt: Int = 0,
    val state: PlanStepState = PlanStepState.WAITING,
    val lastError: String? = null
)

data class MayraPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val originalCommand: String,
    val createdAt: Long = System.currentTimeMillis(),
    val scheduledFor: Long? = null,
    val state: PlanState = PlanState.DRAFT,
    val steps: List<PlanStep>
)

data class PlanProgress(
    val total: Int,
    val completed: Int,
    val failed: Int,
    val blocked: Int,
    val percent: Int
)

class MayraTaskPlanner {
    fun createPlan(
        title: String,
        originalCommand: String,
        steps: List<PlanStep>,
        scheduledFor: Long? = null
    ): MayraPlan {
        require(title.isNotBlank())
        require(originalCommand.isNotBlank())
        require(steps.isNotEmpty())
        require(steps.map { it.id }.distinct().size == steps.size) { "Duplicate step ids" }
        val ids = steps.map { it.id }.toSet()
        require(steps.all { it.dependsOn.all(ids::contains) }) { "Unknown dependency" }
        require(!containsCycle(steps)) { "Plan dependencies contain a cycle" }

        return MayraPlan(
            title = title,
            originalCommand = originalCommand,
            scheduledFor = scheduledFor,
            state = PlanState.READY,
            steps = steps.sortedBy(PlanStep::order)
        )
    }

    fun readySteps(plan: MayraPlan, confirmedStepIds: Set<String> = emptySet()): List<PlanStep> {
        val completed = plan.steps.filter { it.state == PlanStepState.COMPLETED }.map { it.id }.toSet()
        return plan.steps.filter { step ->
            step.state in setOf(PlanStepState.WAITING, PlanStepState.READY) &&
                step.dependsOn.all(completed::contains) &&
                (!step.requiresConfirmation || step.id in confirmedStepIds)
        }.map { it.copy(state = PlanStepState.READY) }
    }

    fun markRunning(plan: MayraPlan, stepId: String): MayraPlan = updateStep(plan, stepId) {
        require(it.state in setOf(PlanStepState.WAITING, PlanStepState.READY))
        it.copy(state = PlanStepState.RUNNING)
    }.copy(state = PlanState.RUNNING)

    fun markCompleted(plan: MayraPlan, stepId: String): MayraPlan {
        val updated = updateStep(plan, stepId) {
            require(it.state == PlanStepState.RUNNING)
            it.copy(state = PlanStepState.COMPLETED, lastError = null)
        }
        return recalculateState(updated)
    }

    fun markFailed(plan: MayraPlan, stepId: String, error: String): MayraPlan {
        val updated = updateStep(plan, stepId) { step ->
            val nextAttempt = step.attempt + 1
            when {
                step.failurePolicy == FailurePolicy.RETRY && nextAttempt < step.maxAttempts ->
                    step.copy(state = PlanStepState.READY, attempt = nextAttempt, lastError = error.take(250))
                else -> step.copy(state = PlanStepState.FAILED, attempt = nextAttempt, lastError = error.take(250))
            }
        }
        return recalculateState(updated)
    }

    fun cancel(plan: MayraPlan): MayraPlan = plan.copy(
        state = PlanState.CANCELLED,
        steps = plan.steps.map {
            if (it.state in setOf(PlanStepState.COMPLETED, PlanStepState.FAILED)) it
            else it.copy(state = PlanStepState.CANCELLED)
        }
    )

    fun progress(plan: MayraPlan): PlanProgress {
        val total = plan.steps.size
        val completed = plan.steps.count { it.state == PlanStepState.COMPLETED }
        val failed = plan.steps.count { it.state == PlanStepState.FAILED }
        val blocked = plan.steps.count { step ->
            step.state == PlanStepState.WAITING && step.dependsOn.any { dependency ->
                plan.steps.firstOrNull { it.id == dependency }?.state == PlanStepState.FAILED
            }
        }
        return PlanProgress(total, completed, failed, blocked, if (total == 0) 0 else completed * 100 / total)
    }

    private fun updateStep(plan: MayraPlan, stepId: String, transform: (PlanStep) -> PlanStep): MayraPlan {
        require(plan.steps.any { it.id == stepId }) { "Unknown step: $stepId" }
        return plan.copy(steps = plan.steps.map { if (it.id == stepId) transform(it) else it })
    }

    private fun recalculateState(plan: MayraPlan): MayraPlan {
        val states = plan.steps.map(PlanStep::state)
        val state = when {
            states.all { it == PlanStepState.COMPLETED } -> PlanState.COMPLETED
            states.any { it == PlanStepState.FAILED } && plan.steps
                .filter { it.state == PlanStepState.FAILED }
                .any { it.failurePolicy == FailurePolicy.STOP_PLAN } -> PlanState.FAILED
            readySteps(plan).isEmpty() && states.any { it == PlanStepState.WAITING } -> PlanState.BLOCKED
            else -> PlanState.RUNNING
        }
        return plan.copy(state = state)
    }

    private fun containsCycle(steps: List<PlanStep>): Boolean {
        val dependencies = steps.associate { it.id to it.dependsOn }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun visit(id: String): Boolean {
            if (id in visiting) return true
            if (id in visited) return false
            visiting += id
            val cyclic = dependencies[id].orEmpty().any(::visit)
            visiting -= id
            visited += id
            return cyclic
        }

        return dependencies.keys.any(::visit)
    }
}
