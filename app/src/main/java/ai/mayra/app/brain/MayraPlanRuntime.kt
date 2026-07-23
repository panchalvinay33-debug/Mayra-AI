package ai.mayra.app.brain

import android.content.Context
import java.util.UUID

class MayraPlanStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun upsert(plan: MayraPlan): MayraPlan {
        save(snapshot().filterNot { it.id == plan.id } + plan)
        return plan
    }

    @Synchronized
    fun remove(planId: String): Boolean {
        val current = snapshot()
        val updated = current.filterNot { it.id == planId }
        if (updated.size == current.size) return false
        save(updated)
        return true
    }

    fun get(planId: String): MayraPlan? = snapshot().firstOrNull { it.id == planId }

    fun snapshot(): List<MayraPlan> = preferences.getStringSet(KEY_PLANS, emptySet()).orEmpty()
        .mapNotNull(::decodePlan)
        .sortedByDescending(MayraPlan::createdAt)

    fun active(now: Long = System.currentTimeMillis()): List<MayraPlan> = snapshot().filter {
        it.state !in TERMINAL_PLAN_STATES && (it.scheduledFor == null || it.scheduledFor <= now)
    }

    @Synchronized
    fun prune(maxEntries: Int = 100) {
        require(maxEntries > 0)
        val retained = snapshot().sortedWith(
            compareByDescending<MayraPlan> { it.state !in TERMINAL_PLAN_STATES }
                .thenByDescending { it.createdAt }
        ).take(maxEntries)
        save(retained)
    }

    private fun save(plans: List<MayraPlan>) {
        preferences.edit().putStringSet(KEY_PLANS, plans.map(::encodePlan).toSet()).apply()
    }

    private fun encodePlan(plan: MayraPlan): String {
        val steps = plan.steps.joinToString(STEP_SEPARATOR) { step ->
            listOf(
                step.id,
                step.order,
                step.intent,
                step.description,
                encodeMap(step.parameters),
                step.dependsOn.joinToString(SET_SEPARATOR),
                step.requiresConfirmation,
                step.failurePolicy.name,
                step.maxAttempts,
                step.attempt,
                step.state.name,
                step.lastError.orEmpty()
            ).joinToString(FIELD_SEPARATOR) { sanitize(it.toString()) }
        }
        return listOf(
            plan.id,
            plan.title,
            plan.originalCommand,
            plan.createdAt,
            plan.scheduledFor ?: -1L,
            plan.state.name,
            steps
        ).joinToString(PLAN_SEPARATOR) { sanitizePlan(it.toString()) }
    }

    private fun decodePlan(raw: String): MayraPlan? {
        val parts = raw.split(PLAN_SEPARATOR)
        if (parts.size != 7) return null
        val steps = if (parts[6].isBlank()) emptyList() else parts[6].split(STEP_SEPARATOR).mapNotNull(::decodeStep)
        if (steps.isEmpty()) return null
        return MayraPlan(
            id = parts[0],
            title = parts[1],
            originalCommand = parts[2],
            createdAt = parts[3].toLongOrNull() ?: return null,
            scheduledFor = parts[4].toLongOrNull()?.takeIf { it >= 0L },
            state = enumValueOrNull<PlanState>(parts[5]) ?: return null,
            steps = steps
        )
    }

    private fun decodeStep(raw: String): PlanStep? {
        val p = raw.split(FIELD_SEPARATOR)
        if (p.size != 12) return null
        return PlanStep(
            id = p[0],
            order = p[1].toIntOrNull() ?: return null,
            intent = p[2],
            description = p[3],
            parameters = decodeMap(p[4]),
            dependsOn = p[5].split(SET_SEPARATOR).filter(String::isNotBlank).toSet(),
            requiresConfirmation = p[6].toBooleanStrictOrNull() ?: return null,
            failurePolicy = enumValueOrNull<FailurePolicy>(p[7]) ?: return null,
            maxAttempts = p[8].toIntOrNull() ?: return null,
            attempt = p[9].toIntOrNull() ?: return null,
            state = enumValueOrNull<PlanStepState>(p[10]) ?: return null,
            lastError = p[11].ifBlank { null }
        )
    }

    private fun encodeMap(values: Map<String, String>): String = values.entries.joinToString(MAP_ENTRY_SEPARATOR) {
        "${sanitize(it.key)}$MAP_VALUE_SEPARATOR${sanitize(it.value)}"
    }

    private fun decodeMap(raw: String): Map<String, String> = if (raw.isBlank()) emptyMap() else raw
        .split(MAP_ENTRY_SEPARATOR)
        .mapNotNull { entry ->
            val index = entry.indexOf(MAP_VALUE_SEPARATOR)
            if (index <= 0) null else entry.substring(0, index) to entry.substring(index + MAP_VALUE_SEPARATOR.length)
        }.toMap()

    private fun sanitize(value: String): String = value
        .replace(PLAN_SEPARATOR, " ")
        .replace(STEP_SEPARATOR, " ")
        .replace(FIELD_SEPARATOR, " ")
        .replace(MAP_ENTRY_SEPARATOR, " ")
        .replace(MAP_VALUE_SEPARATOR, " ")
        .replace(SET_SEPARATOR, " ")
        .take(MAX_FIELD_LENGTH)

    private fun sanitizePlan(value: String): String = value.replace(PLAN_SEPARATOR, " ")

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value) }.getOrNull()

    private companion object {
        const val FILE_NAME = "mayra_plans"
        const val KEY_PLANS = "plans"
        const val PLAN_SEPARATOR = "\u001A"
        const val STEP_SEPARATOR = "\u001B"
        const val FIELD_SEPARATOR = "\u001C"
        const val MAP_ENTRY_SEPARATOR = "\u001D"
        const val MAP_VALUE_SEPARATOR = "\u001E"
        const val SET_SEPARATOR = "\u001F"
        const val MAX_FIELD_LENGTH = 1200
        val TERMINAL_PLAN_STATES = setOf(PlanState.COMPLETED, PlanState.FAILED, PlanState.CANCELLED)
    }
}

data class PlanExecutionResult(
    val plan: MayraPlan,
    val executedStepId: String?,
    val skillResult: SkillResult?,
    val waitingForConfirmation: Boolean,
    val idle: Boolean
)

data class PlanRuntimeDiagnostics(
    val storedPlans: Int,
    val activePlans: Int,
    val runningPlans: Int,
    val blockedPlans: Int,
    val completedPlans: Int,
    val failedPlans: Int,
    val waitingConfirmationSteps: Int
)

class MayraPlanRuntime(
    private val planner: MayraTaskPlanner,
    private val store: MayraPlanStore,
    private val skills: MayraSkillRegistry,
    private val contextProvider: () -> BrainContextSnapshot
) {
    private val confirmedSteps = mutableSetOf<String>()

    @Synchronized
    fun submit(plan: MayraPlan): MayraPlan = store.upsert(plan)

    @Synchronized
    fun confirmStep(planId: String, stepId: String): Boolean {
        val plan = store.get(planId) ?: return false
        val step = plan.steps.firstOrNull { it.id == stepId } ?: return false
        if (!step.requiresConfirmation || step.state !in setOf(PlanStepState.WAITING, PlanStepState.READY)) return false
        confirmedSteps += stepId
        return true
    }

    @Synchronized
    fun cancel(planId: String): MayraPlan? {
        val plan = store.get(planId) ?: return null
        confirmedSteps.removeAll(plan.steps.map { it.id }.toSet())
        return store.upsert(planner.cancel(plan))
    }

    suspend fun executeNext(planId: String, now: Long = System.currentTimeMillis()): PlanExecutionResult {
        val original = store.get(planId) ?: throw IllegalArgumentException("Unknown plan: $planId")
        if (original.scheduledFor?.let { it > now } == true || original.state in TERMINAL_STATES) {
            return PlanExecutionResult(original, null, null, waitingForConfirmation = false, idle = true)
        }

        val ready = planner.readySteps(original, confirmedSteps).firstOrNull()
        if (ready == null) {
            val waitingForConfirmation = original.steps.any {
                it.requiresConfirmation && it.state in setOf(PlanStepState.WAITING, PlanStepState.READY) && it.id !in confirmedSteps
            }
            return PlanExecutionResult(original, null, null, waitingForConfirmation, idle = !waitingForConfirmation)
        }

        var running = planner.markRunning(original, ready.id)
        store.upsert(running)
        val request = SkillRequest(
            intent = ready.intent,
            utterance = ready.description,
            parameters = ready.parameters,
            context = contextProvider(),
            confirmed = !ready.requiresConfirmation || ready.id in confirmedSteps
        )
        val result = skills.executeBest(request)
        running = when (result) {
            is SkillResult.Success -> planner.markCompleted(running, ready.id)
            is SkillResult.Failure -> planner.markFailed(running, ready.id, result.reason)
            is SkillResult.MissingPermission -> planner.markFailed(running, ready.id, result.explanation)
            is SkillResult.NeedsConfirmation -> {
                confirmedSteps.remove(ready.id)
                running.copy(
                    state = PlanState.BLOCKED,
                    steps = running.steps.map {
                        if (it.id == ready.id) it.copy(state = PlanStepState.WAITING, lastError = "Confirmation required") else it
                    }
                )
            }
            SkillResult.NotHandled -> planner.markFailed(running, ready.id, "No registered skill handled ${ready.intent}")
        }
        if (running.steps.firstOrNull { it.id == ready.id }?.state == PlanStepState.COMPLETED) confirmedSteps.remove(ready.id)
        store.upsert(running)
        return PlanExecutionResult(
            plan = running,
            executedStepId = ready.id,
            skillResult = result,
            waitingForConfirmation = result is SkillResult.NeedsConfirmation,
            idle = false
        )
    }

    suspend fun drain(planId: String, maxSteps: Int = 20): MayraPlan {
        require(maxSteps > 0)
        var plan = store.get(planId) ?: throw IllegalArgumentException("Unknown plan: $planId")
        repeat(maxSteps) {
            val result = executeNext(planId)
            plan = result.plan
            if (result.idle || result.waitingForConfirmation || plan.state in TERMINAL_STATES) return plan
        }
        return plan
    }

    fun diagnostics(now: Long = System.currentTimeMillis()): PlanRuntimeDiagnostics {
        val all = store.snapshot()
        return PlanRuntimeDiagnostics(
            storedPlans = all.size,
            activePlans = store.active(now).size,
            runningPlans = all.count { it.state == PlanState.RUNNING },
            blockedPlans = all.count { it.state == PlanState.BLOCKED },
            completedPlans = all.count { it.state == PlanState.COMPLETED },
            failedPlans = all.count { it.state == PlanState.FAILED },
            waitingConfirmationSteps = all.sumOf { plan ->
                plan.steps.count { it.requiresConfirmation && it.state in setOf(PlanStepState.WAITING, PlanStepState.READY) && it.id !in confirmedSteps }
            }
        )
    }

    private companion object {
        val TERMINAL_STATES = setOf(PlanState.COMPLETED, PlanState.FAILED, PlanState.CANCELLED)
    }
}
