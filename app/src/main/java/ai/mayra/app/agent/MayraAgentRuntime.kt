package ai.mayra.app.agent

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/** Framework-neutral tool contract used to connect voice, automation, search, memory and vision. */
interface MayraAgentTool {
    val descriptor: AgentToolDescriptor
    suspend fun execute(call: AgentToolCall, context: AgentExecutionContext): AgentToolResult
    suspend fun compensate(call: AgentToolCall, result: AgentToolResult.Success, context: AgentExecutionContext): AgentToolResult =
        AgentToolResult.NotSupported("Rollback is not supported by ${descriptor.id}")
}

data class AgentToolDescriptor(
    val id: String,
    val displayName: String,
    val operations: Set<String>,
    val risk: AgentRisk = AgentRisk.LOW,
    val supportsCompensation: Boolean = false,
    val requiresNetwork: Boolean = false,
    val timeoutMillis: Long = 30_000L,
    val maxPayloadEntries: Int = 40
) {
    init {
        require(id.isNotBlank())
        require(displayName.isNotBlank())
        require(operations.isNotEmpty())
        require(timeoutMillis in 100L..120_000L)
        require(maxPayloadEntries in 1..100)
    }
}

enum class AgentRisk { LOW, MEDIUM, HIGH, CRITICAL }
enum class AgentRunState { DRAFT, READY, RUNNING, WAITING, BLOCKED, PAUSED, COMPLETED, FAILED, CANCELLED, COMPENSATING }
enum class AgentStepState { WAITING, READY, RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED, COMPENSATED }
enum class AgentFailurePolicy { STOP, CONTINUE, RETRY, ROLLBACK }
enum class AgentStepKind { TOOL, CONDITION, DELAY, CHECKPOINT, USER_INPUT }

data class AgentToolCall(
    val toolId: String,
    val operation: String,
    val arguments: Map<String, String> = emptyMap()
) {
    init {
        require(toolId.isNotBlank())
        require(operation.isNotBlank())
        require(arguments.size <= 100)
    }
}

sealed interface AgentToolResult {
    data class Success(
        val message: String,
        val outputs: Map<String, String> = emptyMap(),
        val compensationToken: String? = null
    ) : AgentToolResult
    data class NeedsConfirmation(val prompt: String) : AgentToolResult
    data class NeedsPermission(val permissions: Set<String>, val explanation: String) : AgentToolResult
    data class RetryableFailure(val reason: String) : AgentToolResult
    data class Failure(val reason: String) : AgentToolResult
    data class NotSupported(val reason: String) : AgentToolResult
}

data class AgentCondition(
    val variable: String,
    val operator: AgentConditionOperator,
    val expected: String = ""
)

enum class AgentConditionOperator { EXISTS, EQUALS, NOT_EQUALS, CONTAINS, GREATER_THAN, LESS_THAN }

data class AgentStep(
    val id: String = UUID.randomUUID().toString(),
    val order: Int,
    val title: String,
    val kind: AgentStepKind,
    val call: AgentToolCall? = null,
    val condition: AgentCondition? = null,
    val delayMillis: Long = 0L,
    val dependencies: Set<String> = emptySet(),
    val outputBindings: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
    val failurePolicy: AgentFailurePolicy = AgentFailurePolicy.STOP,
    val maxAttempts: Int = 3,
    val attempt: Int = 0,
    val state: AgentStepState = AgentStepState.WAITING,
    val availableAt: Long? = null,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val lastError: String? = null
) {
    init {
        require(title.isNotBlank())
        require(order >= 0)
        require(delayMillis in 0L..MAX_DELAY_MILLIS)
        require(maxAttempts in 1..10)
        require(attempt in 0..maxAttempts)
        require(outputBindings.size <= 30)
        if (kind == AgentStepKind.TOOL) require(call != null)
        if (kind == AgentStepKind.CONDITION) require(condition != null)
    }

    companion object { const val MAX_DELAY_MILLIS = 7L * 24 * 60 * 60 * 1000 }
}

data class AgentPlan(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val objective: String,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_PLAN_TTL,
    val maxExecutions: Int = 100,
    val maxDepth: Int = 12,
    val allowParallel: Boolean = true,
    val steps: List<AgentStep>
) {
    init {
        require(title.isNotBlank())
        require(objective.isNotBlank())
        require(expiresAt > createdAt)
        require(maxExecutions in 1..500)
        require(maxDepth in 1..30)
        require(steps.isNotEmpty())
        require(steps.size <= 150)
    }

    companion object { const val DEFAULT_PLAN_TTL = 7L * 24 * 60 * 60 * 1000 }
}

data class AgentWorkspace(
    val variables: Map<String, String> = emptyMap(),
    val outputs: Map<String, Map<String, String>> = emptyMap(),
    val messages: List<String> = emptyList(),
    val pendingQuestion: String? = null
) {
    init {
        require(variables.size <= 300)
        require(outputs.size <= 150)
        require(messages.size <= 200)
    }
}

data class AgentRun(
    val id: String = UUID.randomUUID().toString(),
    val plan: AgentPlan,
    val state: AgentRunState = AgentRunState.DRAFT,
    val steps: List<AgentStep> = plan.steps,
    val workspace: AgentWorkspace = AgentWorkspace(),
    val executionCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val nextWakeAt: Long? = null,
    val cancellationReason: String? = null,
    val lastError: String? = null
)

data class AgentExecutionContext(
    val runId: String,
    val stepId: String,
    val workspace: AgentWorkspace,
    val confirmed: Boolean,
    val now: Long
)

data class AgentTickResult(
    val run: AgentRun,
    val executedStepIds: List<String> = emptyList(),
    val waitingForConfirmationStepId: String? = null,
    val waitingForInputStepId: String? = null,
    val waitingUntil: Long? = null,
    val idle: Boolean = false
)

data class AgentCheckpoint(
    val runId: String,
    val planId: String,
    val state: AgentRunState,
    val executionCount: Int,
    val stepStates: Map<String, AgentStepState>,
    val variables: Map<String, String>,
    val updatedAt: Long
)

data class AgentDiagnostics(
    val runs: Int,
    val active: Int,
    val completed: Int,
    val failed: Int,
    val cancelled: Int,
    val blocked: Int,
    val executedSteps: Long,
    val retries: Long,
    val compensations: Long,
    val toolFailures: Map<String, Long>
)

class AgentPlanValidator {
    fun validate(plan: AgentPlan, tools: Map<String, MayraAgentTool>) {
        val ids = plan.steps.map(AgentStep::id)
        require(ids.distinct().size == ids.size) { "Duplicate agent step ids" }
        val idSet = ids.toSet()
        require(plan.steps.all { step -> step.dependencies.all(idSet::contains) }) { "Unknown step dependency" }
        require(!containsCycle(plan.steps)) { "Agent plan contains a dependency cycle" }
        require(maxDepth(plan.steps) <= plan.maxDepth) { "Agent plan exceeds maximum dependency depth" }
        plan.steps.filter { it.kind == AgentStepKind.TOOL }.forEach { step ->
            val call = requireNotNull(step.call)
            val tool = tools[call.toolId] ?: error("Unknown tool: ${call.toolId}")
            require(call.operation in tool.descriptor.operations) { "Unsupported operation ${call.operation} for ${call.toolId}" }
            require(call.arguments.size <= tool.descriptor.maxPayloadEntries)
            if (tool.descriptor.risk >= AgentRisk.HIGH) require(step.requiresConfirmation) {
                "High-risk tool step ${step.id} must require confirmation"
            }
        }
    }

    private fun containsCycle(steps: List<AgentStep>): Boolean {
        val graph = steps.associate { it.id to it.dependencies }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String): Boolean {
            if (id in visiting) return true
            if (id in visited) return false
            visiting += id
            val cycle = graph[id].orEmpty().any(::visit)
            visiting -= id
            visited += id
            return cycle
        }
        return graph.keys.any(::visit)
    }

    private fun maxDepth(steps: List<AgentStep>): Int {
        val graph = steps.associate { it.id to it.dependencies }
        val memo = mutableMapOf<String, Int>()
        fun depth(id: String): Int = memo.getOrPut(id) {
            1 + (graph[id].orEmpty().maxOfOrNull(::depth) ?: 0)
        }
        return graph.keys.maxOfOrNull(::depth) ?: 0
    }
}

class MayraAgentRuntime(
    tools: Collection<MayraAgentTool>,
    private val validator: AgentPlanValidator = AgentPlanValidator(),
    private val now: () -> Long = System::currentTimeMillis,
    private val maxParallelSteps: Int = 4
) {
    private val tools = tools.associateBy { it.descriptor.id }
    private val runs = linkedMapOf<String, AgentRun>()
    private val confirmedSteps = mutableSetOf<String>()
    private val cancelledRuns = mutableSetOf<String>()
    private val executedSteps = AtomicLong(0)
    private val retries = AtomicLong(0)
    private val compensations = AtomicLong(0)
    private val toolFailures = linkedMapOf<String, Long>()

    init {
        require(this.tools.size == tools.size) { "Duplicate agent tool ids" }
        require(maxParallelSteps in 1..8)
    }

    @Synchronized
    fun submit(plan: AgentPlan, initialVariables: Map<String, String> = emptyMap()): AgentRun {
        validator.validate(plan, tools)
        require(initialVariables.size <= 100)
        val timestamp = now()
        require(plan.expiresAt > timestamp) { "Agent plan has expired" }
        val run = AgentRun(
            plan = plan,
            state = AgentRunState.READY,
            workspace = AgentWorkspace(variables = sanitizeMap(initialVariables)),
            createdAt = timestamp,
            updatedAt = timestamp
        )
        runs[run.id] = run
        return run
    }

    @Synchronized
    fun get(runId: String): AgentRun? = runs[runId]

    @Synchronized
    fun snapshot(): List<AgentRun> = runs.values.sortedByDescending(AgentRun::updatedAt)

    @Synchronized
    fun confirm(runId: String, stepId: String): Boolean {
        val run = runs[runId] ?: return false
        val step = run.steps.firstOrNull { it.id == stepId } ?: return false
        if (!step.requiresConfirmation || step.state !in setOf(AgentStepState.WAITING, AgentStepState.READY)) return false
        confirmedSteps += confirmationKey(runId, stepId)
        runs[runId] = run.copy(state = AgentRunState.READY, updatedAt = now(), lastError = null)
        return true
    }

    @Synchronized
    fun provideInput(runId: String, stepId: String, value: String): AgentRun? {
        val run = runs[runId] ?: return null
        val step = run.steps.firstOrNull { it.id == stepId && it.kind == AgentStepKind.USER_INPUT } ?: return null
        if (step.state !in setOf(AgentStepState.WAITING, AgentStepState.READY)) return null
        val variable = step.outputBindings.keys.firstOrNull() ?: "input.$stepId"
        val updated = completeStep(
            run.copy(workspace = run.workspace.copy(
                variables = run.workspace.variables + (variable to value.trim().take(2_000)),
                pendingQuestion = null
            )),
            step.id,
            emptyMap()
        )
        runs[runId] = updated
        return updated
    }

    @Synchronized
    fun pause(runId: String): AgentRun? = runs[runId]?.takeIf { it.state !in TERMINAL_STATES }?.let {
        it.copy(state = AgentRunState.PAUSED, updatedAt = now()).also { updated -> runs[runId] = updated }
    }

    @Synchronized
    fun resume(runId: String): AgentRun? = runs[runId]?.takeIf { it.state == AgentRunState.PAUSED }?.let {
        it.copy(state = AgentRunState.READY, updatedAt = now()).also { updated -> runs[runId] = updated }
    }

    @Synchronized
    fun cancel(runId: String, reason: String = "Cancelled by user"): AgentRun? {
        val run = runs[runId] ?: return null
        if (run.state in TERMINAL_STATES) return run
        cancelledRuns += runId
        confirmedSteps.removeAll(run.steps.map { confirmationKey(runId, it.id) }.toSet())
        val cancelled = run.copy(
            state = AgentRunState.CANCELLED,
            steps = run.steps.map { if (it.state in FINAL_STEP_STATES) it else it.copy(state = AgentStepState.CANCELLED) },
            cancellationReason = reason.take(300),
            updatedAt = now()
        )
        runs[runId] = cancelled
        return cancelled
    }

    suspend fun tick(runId: String): AgentTickResult {
        val run = synchronized(this) { runs[runId] } ?: error("Unknown agent run: $runId")
        if (run.state in TERMINAL_STATES || run.state == AgentRunState.PAUSED) return AgentTickResult(run, idle = true)
        if (runId in synchronized(this) { cancelledRuns.toSet() }) return AgentTickResult(get(runId) ?: run, idle = true)
        val timestamp = now()
        if (run.plan.expiresAt <= timestamp) return failRun(run, "Agent plan expired")
        if (run.executionCount >= run.plan.maxExecutions) return failRun(run, "Execution limit reached; possible loop prevented")

        val ready = readySteps(run, timestamp)
        if (ready.isEmpty()) return settle(run, timestamp)

        val confirmation = ready.firstOrNull { it.requiresConfirmation && confirmationKey(run.id, it.id) !in synchronized(this) { confirmedSteps.toSet() } }
        if (confirmation != null) {
            val blocked = run.copy(state = AgentRunState.BLOCKED, updatedAt = timestamp, lastError = "Confirmation required for ${confirmation.title}")
            synchronized(this) { runs[run.id] = blocked }
            return AgentTickResult(blocked, waitingForConfirmationStepId = confirmation.id)
        }

        val input = ready.firstOrNull { it.kind == AgentStepKind.USER_INPUT }
        if (input != null) {
            val blocked = run.copy(
                state = AgentRunState.BLOCKED,
                workspace = run.workspace.copy(pendingQuestion = input.title),
                updatedAt = timestamp
            )
            synchronized(this) { runs[run.id] = blocked }
            return AgentTickResult(blocked, waitingForInputStepId = input.id)
        }

        val executable = if (run.plan.allowParallel) ready.take(maxParallelSteps) else ready.take(1)
        val base = run.copy(
            state = AgentRunState.RUNNING,
            executionCount = run.executionCount + executable.size,
            steps = run.steps.map { step ->
                if (step.id in executable.map(AgentStep::id)) step.copy(state = AgentStepState.RUNNING, startedAt = timestamp) else step
            },
            updatedAt = timestamp
        )
        synchronized(this) { runs[run.id] = base }

        val outcomes = coroutineScope {
            executable.map { step -> async { step to executeStep(base, step, timestamp) } }.map { it.await() }
        }
        var updated = base
        val executed = mutableListOf<String>()
        for ((step, outcome) in outcomes.sortedBy { it.first.order }) {
            updated = applyOutcome(updated, step, outcome)
            executed += step.id
            executedSteps.incrementAndGet()
            if (updated.state in setOf(AgentRunState.FAILED, AgentRunState.CANCELLED, AgentRunState.COMPENSATING)) break
        }
        updated = recalculate(updated)
        synchronized(this) { runs[run.id] = updated }
        return AgentTickResult(updated, executedStepIds = executed, waitingUntil = updated.nextWakeAt)
    }

    suspend fun drain(runId: String, maxTicks: Int = 50): AgentRun {
        require(maxTicks in 1..200)
        repeat(maxTicks) {
            val result = tick(runId)
            if (result.idle || result.waitingForConfirmationStepId != null || result.waitingForInputStepId != null ||
                result.waitingUntil?.let { it > now() } == true || result.run.state in TERMINAL_STATES || result.run.state == AgentRunState.PAUSED
            ) return result.run
        }
        return get(runId) ?: error("Unknown agent run: $runId")
    }

    @Synchronized
    fun checkpoint(runId: String): AgentCheckpoint? = runs[runId]?.let { run ->
        AgentCheckpoint(
            runId = run.id,
            planId = run.plan.id,
            state = run.state,
            executionCount = run.executionCount,
            stepStates = run.steps.associate { it.id to it.state },
            variables = run.workspace.variables,
            updatedAt = run.updatedAt
        )
    }

    fun diagnostics(): AgentDiagnostics {
        val all = snapshot()
        return AgentDiagnostics(
            runs = all.size,
            active = all.count { it.state !in TERMINAL_STATES },
            completed = all.count { it.state == AgentRunState.COMPLETED },
            failed = all.count { it.state == AgentRunState.FAILED },
            cancelled = all.count { it.state == AgentRunState.CANCELLED },
            blocked = all.count { it.state == AgentRunState.BLOCKED },
            executedSteps = executedSteps.get(),
            retries = retries.get(),
            compensations = compensations.get(),
            toolFailures = synchronized(this) { toolFailures.toMap() }
        )
    }

    private fun readySteps(run: AgentRun, timestamp: Long): List<AgentStep> {
        val completed = run.steps.filter { it.state in setOf(AgentStepState.COMPLETED, AgentStepState.SKIPPED, AgentStepState.COMPENSATED) }.map(AgentStep::id).toSet()
        return run.steps.asSequence()
            .filter { it.state in setOf(AgentStepState.WAITING, AgentStepState.READY) }
            .filter { it.dependencies.all(completed::contains) }
            .filter { it.availableAt?.let { at -> at <= timestamp } != false }
            .sortedBy(AgentStep::order)
            .toList()
    }

    private suspend fun executeStep(run: AgentRun, step: AgentStep, timestamp: Long): StepOutcome = when (step.kind) {
        AgentStepKind.CHECKPOINT -> StepOutcome.Completed(emptyMap(), "Checkpoint reached")
        AgentStepKind.CONDITION -> {
            val condition = requireNotNull(step.condition)
            if (evaluate(condition, run.workspace.variables)) StepOutcome.Completed(emptyMap(), "Condition matched")
            else StepOutcome.Skipped("Condition was false")
        }
        AgentStepKind.DELAY -> {
            val available = step.availableAt ?: timestamp + step.delayMillis
            if (available > timestamp) StepOutcome.Waiting(available) else StepOutcome.Completed(emptyMap(), "Delay completed")
        }
        AgentStepKind.USER_INPUT -> StepOutcome.WaitingForInput
        AgentStepKind.TOOL -> {
            val call = requireNotNull(step.call)
            val tool = tools.getValue(call.toolId)
            val confirmed = !step.requiresConfirmation || confirmationKey(run.id, step.id) in synchronized(this) { confirmedSteps.toSet() }
            val context = AgentExecutionContext(run.id, step.id, run.workspace, confirmed, timestamp)
            val result = withTimeoutOrNull(tool.descriptor.timeoutMillis) { tool.execute(call, context) }
                ?: AgentToolResult.RetryableFailure("Tool timed out")
            StepOutcome.Tool(result)
        }
    }

    private suspend fun applyOutcome(run: AgentRun, step: AgentStep, outcome: StepOutcome): AgentRun = when (outcome) {
        is StepOutcome.Completed -> completeStep(run, step.id, outcome.outputs, outcome.message)
        is StepOutcome.Skipped -> updateStep(run, step.id) { it.copy(state = AgentStepState.SKIPPED, completedAt = now(), lastError = outcome.reason) }
        is StepOutcome.Waiting -> updateStep(run, step.id) { it.copy(state = AgentStepState.WAITING, availableAt = outcome.until) }
            .copy(state = AgentRunState.WAITING, nextWakeAt = outcome.until, updatedAt = now())
        StepOutcome.WaitingForInput -> updateStep(run, step.id) { it.copy(state = AgentStepState.WAITING) }
            .copy(state = AgentRunState.BLOCKED, workspace = run.workspace.copy(pendingQuestion = step.title), updatedAt = now())
        is StepOutcome.Tool -> applyToolResult(run, step, outcome.result)
    }

    private suspend fun applyToolResult(run: AgentRun, step: AgentStep, result: AgentToolResult): AgentRun = when (result) {
        is AgentToolResult.Success -> {
            synchronized(this) { confirmedSteps.remove(confirmationKey(run.id, step.id)) }
            completeStep(run, step.id, result.outputs, result.message)
        }
        is AgentToolResult.NeedsConfirmation -> updateStep(run, step.id) { it.copy(state = AgentStepState.WAITING, lastError = result.prompt) }
            .copy(state = AgentRunState.BLOCKED, lastError = result.prompt, updatedAt = now())
        is AgentToolResult.NeedsPermission -> failOrContinue(run, step, result.explanation, retryable = false)
        is AgentToolResult.RetryableFailure -> failOrContinue(run, step, result.reason, retryable = true)
        is AgentToolResult.Failure -> failOrContinue(run, step, result.reason, retryable = false)
        is AgentToolResult.NotSupported -> failOrContinue(run, step, result.reason, retryable = false)
    }

    private suspend fun failOrContinue(run: AgentRun, step: AgentStep, reason: String, retryable: Boolean): AgentRun {
        synchronized(this) { toolFailures[step.call?.toolId.orEmpty()] = (toolFailures[step.call?.toolId.orEmpty()] ?: 0L) + 1 }
        val attempt = step.attempt + 1
        return when {
            retryable && step.failurePolicy == AgentFailurePolicy.RETRY && attempt < step.maxAttempts -> {
                retries.incrementAndGet()
                updateStep(run, step.id) { it.copy(state = AgentStepState.READY, attempt = attempt, lastError = reason.take(300)) }
                    .copy(state = AgentRunState.READY, updatedAt = now())
            }
            step.failurePolicy == AgentFailurePolicy.CONTINUE -> updateStep(run, step.id) {
                it.copy(state = AgentStepState.SKIPPED, attempt = attempt, completedAt = now(), lastError = reason.take(300))
            }
            step.failurePolicy == AgentFailurePolicy.ROLLBACK -> compensate(run, step, reason)
            else -> updateStep(run, step.id) {
                it.copy(state = AgentStepState.FAILED, attempt = attempt, completedAt = now(), lastError = reason.take(300))
            }.copy(state = AgentRunState.FAILED, lastError = reason.take(500), updatedAt = now())
        }
    }

    private suspend fun compensate(run: AgentRun, failedStep: AgentStep, reason: String): AgentRun {
        var current = run.copy(state = AgentRunState.COMPENSATING, lastError = reason.take(500), updatedAt = now())
        val completed = current.steps.filter { it.state == AgentStepState.COMPLETED && it.kind == AgentStepKind.TOOL }.sortedByDescending(AgentStep::order)
        for (step in completed) {
            val call = step.call ?: continue
            val tool = tools[call.toolId] ?: continue
            if (!tool.descriptor.supportsCompensation) continue
            val prior = AgentToolResult.Success(
                message = current.workspace.messages.lastOrNull().orEmpty(),
                outputs = current.workspace.outputs[step.id].orEmpty()
            )
            val context = AgentExecutionContext(current.id, step.id, current.workspace, true, now())
            val outcome = withTimeoutOrNull(tool.descriptor.timeoutMillis) { tool.compensate(call, prior, context) }
            if (outcome is AgentToolResult.Success) {
                compensations.incrementAndGet()
                current = updateStep(current, step.id) { it.copy(state = AgentStepState.COMPENSATED, completedAt = now()) }
            }
        }
        return updateStep(current, failedStep.id) {
            it.copy(state = AgentStepState.FAILED, attempt = it.attempt + 1, completedAt = now(), lastError = reason.take(300))
        }.copy(state = AgentRunState.FAILED, updatedAt = now())
    }

    private fun completeStep(run: AgentRun, stepId: String, outputs: Map<String, String>, message: String = ""): AgentRun {
        val step = run.steps.first { it.id == stepId }
        val cleanOutputs = sanitizeMap(outputs)
        val bound = step.outputBindings.mapNotNull { (variable, outputKey) -> cleanOutputs[outputKey]?.let { variable to it } }.toMap()
        val workspace = run.workspace.copy(
            variables = (run.workspace.variables + bound).entries.takeLast(300).associate { it.toPair() },
            outputs = (run.workspace.outputs + (stepId to cleanOutputs)).entries.takeLast(150).associate { it.toPair() },
            messages = (run.workspace.messages + message.takeIf(String::isNotBlank).orEmpty()).filter(String::isNotBlank).takeLast(200),
            pendingQuestion = null
        )
        return updateStep(run.copy(workspace = workspace), stepId) {
            it.copy(state = AgentStepState.COMPLETED, completedAt = now(), lastError = null)
        }.copy(state = AgentRunState.RUNNING, nextWakeAt = null, updatedAt = now())
    }

    private fun settle(run: AgentRun, timestamp: Long): AgentTickResult {
        val recalculated = recalculate(run.copy(updatedAt = timestamp))
        synchronized(this) { runs[run.id] = recalculated }
        val waitingUntil = recalculated.steps.filter { it.state == AgentStepState.WAITING }.mapNotNull(AgentStep::availableAt).minOrNull()
        return AgentTickResult(recalculated, waitingUntil = waitingUntil, idle = waitingUntil == null)
    }

    private fun recalculate(run: AgentRun): AgentRun {
        if (run.state in TERMINAL_STATES) return run
        val states = run.steps.map(AgentStep::state)
        val state = when {
            states.all { it in setOf(AgentStepState.COMPLETED, AgentStepState.SKIPPED, AgentStepState.COMPENSATED) } -> AgentRunState.COMPLETED
            states.any { it == AgentStepState.FAILED } -> AgentRunState.FAILED
            run.workspace.pendingQuestion != null -> AgentRunState.BLOCKED
            states.any { it == AgentStepState.RUNNING } -> AgentRunState.RUNNING
            states.any { it == AgentStepState.WAITING && run.steps.first { step -> step.state == AgentStepState.WAITING }.availableAt != null } -> AgentRunState.WAITING
            else -> AgentRunState.READY
        }
        return run.copy(state = state, updatedAt = now())
    }

    private fun failRun(run: AgentRun, reason: String): AgentTickResult {
        val failed = run.copy(
            state = AgentRunState.FAILED,
            lastError = reason.take(500),
            steps = run.steps.map { if (it.state in FINAL_STEP_STATES) it else it.copy(state = AgentStepState.CANCELLED, lastError = reason.take(300)) },
            updatedAt = now()
        )
        synchronized(this) { runs[run.id] = failed }
        return AgentTickResult(failed)
    }

    private fun evaluate(condition: AgentCondition, variables: Map<String, String>): Boolean {
        val actual = variables[condition.variable]
        return when (condition.operator) {
            AgentConditionOperator.EXISTS -> actual != null
            AgentConditionOperator.EQUALS -> actual == condition.expected
            AgentConditionOperator.NOT_EQUALS -> actual != condition.expected
            AgentConditionOperator.CONTAINS -> actual?.contains(condition.expected, ignoreCase = true) == true
            AgentConditionOperator.GREATER_THAN -> actual?.toDoubleOrNull()?.let { it > (condition.expected.toDoubleOrNull() ?: Double.MAX_VALUE) } == true
            AgentConditionOperator.LESS_THAN -> actual?.toDoubleOrNull()?.let { it < (condition.expected.toDoubleOrNull() ?: Double.MIN_VALUE) } == true
        }
    }

    private fun updateStep(run: AgentRun, stepId: String, transform: (AgentStep) -> AgentStep): AgentRun =
        run.copy(steps = run.steps.map { if (it.id == stepId) transform(it) else it })

    private fun sanitizeMap(values: Map<String, String>): Map<String, String> = values.entries.take(300)
        .associate { it.key.trim().take(120) to it.value.trim().take(4_000) }

    private fun confirmationKey(runId: String, stepId: String) = "$runId:$stepId"

    private sealed interface StepOutcome {
        data class Completed(val outputs: Map<String, String>, val message: String) : StepOutcome
        data class Skipped(val reason: String) : StepOutcome
        data class Waiting(val until: Long) : StepOutcome
        data object WaitingForInput : StepOutcome
        data class Tool(val result: AgentToolResult) : StepOutcome
    }

    private companion object {
        val TERMINAL_STATES = setOf(AgentRunState.COMPLETED, AgentRunState.FAILED, AgentRunState.CANCELLED)
        val FINAL_STEP_STATES = setOf(AgentStepState.COMPLETED, AgentStepState.FAILED, AgentStepState.SKIPPED, AgentStepState.CANCELLED, AgentStepState.COMPENSATED)
    }
}
