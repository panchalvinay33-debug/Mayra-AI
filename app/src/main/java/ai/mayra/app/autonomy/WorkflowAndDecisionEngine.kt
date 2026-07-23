package ai.mayra.app.autonomy

import ai.mayra.app.brain.BrainContextSnapshot
import ai.mayra.app.brain.MayraSkillRegistry
import ai.mayra.app.brain.SkillRequest
import ai.mayra.app.brain.SkillResult
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class WorkflowState { DRAFT, READY, RUNNING, WAITING, BLOCKED, COMPLETED, FAILED, CANCELLED }
enum class WorkflowNodeState { WAITING, READY, RUNNING, COMPLETED, FAILED, SKIPPED, CANCELLED }
enum class WorkflowFailurePolicy { STOP, CONTINUE, RETRY }
enum class WorkflowNodeType { ACTION, CONDITION, DELAY, CHECKPOINT }

data class WorkflowCondition(
    val key: String,
    val operator: ConditionOperator,
    val expected: String
)

enum class ConditionOperator { EQUALS, NOT_EQUALS, CONTAINS, GREATER_THAN, LESS_THAN, EXISTS }

data class WorkflowNode(
    val id: String = UUID.randomUUID().toString(),
    val order: Int,
    val type: WorkflowNodeType,
    val title: String,
    val intent: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val dependsOn: Set<String> = emptySet(),
    val condition: WorkflowCondition? = null,
    val delayMillis: Long = 0,
    val requiresConfirmation: Boolean = false,
    val failurePolicy: WorkflowFailurePolicy = WorkflowFailurePolicy.STOP,
    val maxAttempts: Int = 3,
    val attempt: Int = 0,
    val state: WorkflowNodeState = WorkflowNodeState.WAITING,
    val availableAt: Long? = null,
    val lastError: String? = null
)

data class MayraWorkflow(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val goalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val state: WorkflowState = WorkflowState.DRAFT,
    val variables: Map<String, String> = emptyMap(),
    val nodes: List<WorkflowNode>,
    val executionCount: Int = 0,
    val maxExecutions: Int = 100
)

data class WorkflowStepResult(
    val workflow: MayraWorkflow,
    val nodeId: String?,
    val skillResult: SkillResult? = null,
    val waitingForConfirmation: Boolean = false,
    val waitingUntil: Long? = null,
    val idle: Boolean = false
)

class WorkflowValidator {
    fun validate(workflow: MayraWorkflow) {
        require(workflow.title.isNotBlank())
        require(workflow.nodes.isNotEmpty())
        require(workflow.maxExecutions in 1..500)
        require(workflow.nodes.map { it.id }.distinct().size == workflow.nodes.size) { "Duplicate workflow node ids" }
        val ids = workflow.nodes.map { it.id }.toSet()
        require(workflow.nodes.all { node -> node.dependsOn.all(ids::contains) }) { "Unknown node dependency" }
        require(workflow.nodes.all { it.maxAttempts in 1..10 }) { "maxAttempts must be between 1 and 10" }
        require(workflow.nodes.all { it.delayMillis in 0..MAX_DELAY_MILLIS }) { "Delay is outside the safe range" }
        require(!containsCycle(workflow.nodes)) { "Workflow contains a dependency cycle" }
        require(workflow.nodes.count { it.type == WorkflowNodeType.ACTION } <= 100) { "Workflow has too many action nodes" }
    }

    private fun containsCycle(nodes: List<WorkflowNode>): Boolean {
        val dependencies = nodes.associate { it.id to it.dependsOn }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(id: String): Boolean {
            if (id in visiting) return true
            if (id in visited) return false
            visiting += id
            val cycle = dependencies[id].orEmpty().any(::visit)
            visiting -= id
            visited += id
            return cycle
        }
        return dependencies.keys.any(::visit)
    }

    companion object {
        const val MAX_DELAY_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}

class WorkflowRuntime(
    private val skills: MayraSkillRegistry,
    private val contextProvider: () -> BrainContextSnapshot,
    private val validator: WorkflowValidator = WorkflowValidator(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val workflows = linkedMapOf<String, MayraWorkflow>()
    private val confirmedNodes = mutableSetOf<String>()
    private val executedNodes = AtomicLong(0)
    private val failures = AtomicLong(0)

    @Synchronized
    fun submit(workflow: MayraWorkflow): MayraWorkflow {
        validator.validate(workflow)
        val ready = workflow.copy(state = WorkflowState.READY, updatedAt = now())
        workflows[ready.id] = ready
        return ready
    }

    @Synchronized
    fun get(id: String): MayraWorkflow? = workflows[id]

    @Synchronized
    fun snapshot(): List<MayraWorkflow> = workflows.values.sortedByDescending(MayraWorkflow::updatedAt)

    @Synchronized
    fun confirmNode(workflowId: String, nodeId: String): Boolean {
        val workflow = workflows[workflowId] ?: return false
        val node = workflow.nodes.firstOrNull { it.id == nodeId } ?: return false
        if (!node.requiresConfirmation || node.state !in setOf(WorkflowNodeState.WAITING, WorkflowNodeState.READY)) return false
        confirmedNodes += nodeId
        return true
    }

    @Synchronized
    fun cancel(workflowId: String): MayraWorkflow? {
        val workflow = workflows[workflowId] ?: return null
        confirmedNodes.removeAll(workflow.nodes.map { it.id }.toSet())
        val cancelled = workflow.copy(
            state = WorkflowState.CANCELLED,
            updatedAt = now(),
            nodes = workflow.nodes.map { node ->
                if (node.state in setOf(WorkflowNodeState.COMPLETED, WorkflowNodeState.FAILED, WorkflowNodeState.SKIPPED)) node
                else node.copy(state = WorkflowNodeState.CANCELLED)
            }
        )
        workflows[workflowId] = cancelled
        return cancelled
    }

    suspend fun executeNext(workflowId: String): WorkflowStepResult {
        val current = synchronized(this) { workflows[workflowId] } ?: error("Unknown workflow: $workflowId")
        if (current.state in TERMINAL_STATES) return WorkflowStepResult(current, null, idle = true)
        if (current.executionCount >= current.maxExecutions) {
            return failWorkflow(current, "Execution limit reached; workflow stopped to prevent a loop")
        }

        val timestamp = now()
        val completedIds = current.nodes.filter { it.state in setOf(WorkflowNodeState.COMPLETED, WorkflowNodeState.SKIPPED) }.map { it.id }.toSet()
        val node = current.nodes
            .asSequence()
            .filter { it.state in setOf(WorkflowNodeState.WAITING, WorkflowNodeState.READY) }
            .filter { it.dependsOn.all(completedIds::contains) }
            .sortedBy(WorkflowNode::order)
            .firstOrNull()
            ?: return settleIdle(current)

        if (node.requiresConfirmation && node.id !in synchronized(this) { confirmedNodes.toSet() }) {
            val waiting = current.copy(state = WorkflowState.BLOCKED, updatedAt = timestamp)
            synchronized(this) { workflows[workflowId] = waiting }
            return WorkflowStepResult(waiting, node.id, waitingForConfirmation = true)
        }

        if (node.availableAt?.let { it > timestamp } == true) {
            val waiting = current.copy(state = WorkflowState.WAITING, updatedAt = timestamp)
            synchronized(this) { workflows[workflowId] = waiting }
            return WorkflowStepResult(waiting, node.id, waitingUntil = node.availableAt)
        }

        val running = updateNode(current, node.id) { it.copy(state = WorkflowNodeState.RUNNING) }
            .copy(state = WorkflowState.RUNNING, executionCount = current.executionCount + 1, updatedAt = timestamp)
        synchronized(this) { workflows[workflowId] = running }

        val result = when (node.type) {
            WorkflowNodeType.CONDITION -> executeCondition(running, node)
            WorkflowNodeType.DELAY -> executeDelay(running, node, timestamp)
            WorkflowNodeType.CHECKPOINT -> completeNode(running, node.id)
            WorkflowNodeType.ACTION -> executeAction(running, node)
        }
        executedNodes.incrementAndGet()
        return result
    }

    suspend fun drain(workflowId: String, maxSteps: Int = 30): MayraWorkflow {
        require(maxSteps in 1..100)
        var workflow = get(workflowId) ?: error("Unknown workflow: $workflowId")
        repeat(maxSteps) {
            val result = executeNext(workflowId)
            workflow = result.workflow
            if (result.idle || result.waitingForConfirmation || result.waitingUntil != null || workflow.state in TERMINAL_STATES) return workflow
        }
        return workflow
    }

    fun diagnostics(): WorkflowDiagnostics {
        val all = snapshot()
        return WorkflowDiagnostics(
            stored = all.size,
            active = all.count { it.state !in TERMINAL_STATES },
            running = all.count { it.state == WorkflowState.RUNNING },
            blocked = all.count { it.state == WorkflowState.BLOCKED },
            completed = all.count { it.state == WorkflowState.COMPLETED },
            failed = all.count { it.state == WorkflowState.FAILED },
            executedNodes = executedNodes.get(),
            nodeFailures = failures.get()
        )
    }

    private fun executeCondition(workflow: MayraWorkflow, node: WorkflowNode): WorkflowStepResult {
        val condition = node.condition ?: return failNode(workflow, node, "Condition node has no condition")
        val actual = workflow.variables[condition.key]
        val matched = when (condition.operator) {
            ConditionOperator.EXISTS -> actual != null
            ConditionOperator.EQUALS -> actual == condition.expected
            ConditionOperator.NOT_EQUALS -> actual != condition.expected
            ConditionOperator.CONTAINS -> actual?.contains(condition.expected, ignoreCase = true) == true
            ConditionOperator.GREATER_THAN -> actual?.toDoubleOrNull()?.let { it > (condition.expected.toDoubleOrNull() ?: Double.MAX_VALUE) } == true
            ConditionOperator.LESS_THAN -> actual?.toDoubleOrNull()?.let { it < (condition.expected.toDoubleOrNull() ?: Double.MIN_VALUE) } == true
        }
        val updated = if (matched) completeNode(workflow, node.id).workflow else {
            updateNode(workflow, node.id) { it.copy(state = WorkflowNodeState.SKIPPED, lastError = "Condition was false") }
                .let(::recalculate)
        }
        synchronized(this) { workflows[workflow.id] = updated }
        return WorkflowStepResult(updated, node.id)
    }

    private fun executeDelay(workflow: MayraWorkflow, node: WorkflowNode, timestamp: Long): WorkflowStepResult {
        if (node.delayMillis == 0L) return completeNode(workflow, node.id)
        val availableAt = node.availableAt ?: timestamp + node.delayMillis
        if (availableAt > timestamp) {
            val waiting = updateNode(workflow, node.id) { it.copy(state = WorkflowNodeState.WAITING, availableAt = availableAt) }
                .copy(state = WorkflowState.WAITING, updatedAt = timestamp)
            synchronized(this) { workflows[workflow.id] = waiting }
            return WorkflowStepResult(waiting, node.id, waitingUntil = availableAt)
        }
        return completeNode(workflow, node.id)
    }

    private suspend fun executeAction(workflow: MayraWorkflow, node: WorkflowNode): WorkflowStepResult {
        val intent = node.intent ?: return failNode(workflow, node, "Action node has no intent")
        val skillResult = skills.executeBest(
            SkillRequest(
                intent = intent,
                utterance = node.title,
                parameters = node.parameters,
                context = contextProvider(),
                confirmed = !node.requiresConfirmation || node.id in synchronized(this) { confirmedNodes.toSet() }
            )
        )
        return when (skillResult) {
            is SkillResult.Success -> completeNode(workflow, node.id, skillResult)
            is SkillResult.NeedsConfirmation -> {
                synchronized(this) { confirmedNodes.remove(node.id) }
                val blocked = updateNode(workflow, node.id) { it.copy(state = WorkflowNodeState.WAITING, lastError = "Confirmation required") }
                    .copy(state = WorkflowState.BLOCKED, updatedAt = now())
                synchronized(this) { workflows[workflow.id] = blocked }
                WorkflowStepResult(blocked, node.id, skillResult, waitingForConfirmation = true)
            }
            is SkillResult.MissingPermission -> failNode(workflow, node, skillResult.explanation, skillResult)
            is SkillResult.Failure -> failNode(workflow, node, skillResult.reason, skillResult)
            SkillResult.NotHandled -> failNode(workflow, node, "No skill handled $intent", skillResult)
        }
    }

    private fun completeNode(workflow: MayraWorkflow, nodeId: String, skillResult: SkillResult? = null): WorkflowStepResult {
        synchronized(this) { confirmedNodes.remove(nodeId) }
        val completed = updateNode(workflow, nodeId) { it.copy(state = WorkflowNodeState.COMPLETED, lastError = null) }
            .let(::recalculate)
        synchronized(this) { workflows[workflow.id] = completed }
        return WorkflowStepResult(completed, nodeId, skillResult)
    }

    private fun failNode(workflow: MayraWorkflow, node: WorkflowNode, error: String, skillResult: SkillResult? = null): WorkflowStepResult {
        failures.incrementAndGet()
        val nextAttempt = node.attempt + 1
        val updatedNode = when {
            node.failurePolicy == WorkflowFailurePolicy.RETRY && nextAttempt < node.maxAttempts ->
                node.copy(state = WorkflowNodeState.READY, attempt = nextAttempt, lastError = error.take(300))
            node.failurePolicy == WorkflowFailurePolicy.CONTINUE ->
                node.copy(state = WorkflowNodeState.SKIPPED, attempt = nextAttempt, lastError = error.take(300))
            else -> node.copy(state = WorkflowNodeState.FAILED, attempt = nextAttempt, lastError = error.take(300))
        }
        val updated = updateNode(workflow, node.id) { updatedNode }.let(::recalculate)
        synchronized(this) { workflows[workflow.id] = updated }
        return WorkflowStepResult(updated, node.id, skillResult)
    }

    private fun failWorkflow(workflow: MayraWorkflow, error: String): WorkflowStepResult {
        failures.incrementAndGet()
        val failed = workflow.copy(
            state = WorkflowState.FAILED,
            updatedAt = now(),
            nodes = workflow.nodes.map { node ->
                if (node.state in setOf(WorkflowNodeState.COMPLETED, WorkflowNodeState.FAILED, WorkflowNodeState.SKIPPED)) node
                else node.copy(state = WorkflowNodeState.CANCELLED, lastError = error.take(300))
            }
        )
        synchronized(this) { workflows[workflow.id] = failed }
        return WorkflowStepResult(failed, null)
    }

    private fun settleIdle(workflow: MayraWorkflow): WorkflowStepResult {
        val settled = recalculate(workflow)
        synchronized(this) { workflows[workflow.id] = settled }
        return WorkflowStepResult(settled, null, idle = true)
    }

    private fun recalculate(workflow: MayraWorkflow): MayraWorkflow {
        val states = workflow.nodes.map(WorkflowNode::state)
        val state = when {
            states.all { it in setOf(WorkflowNodeState.COMPLETED, WorkflowNodeState.SKIPPED) } -> WorkflowState.COMPLETED
            states.any { it == WorkflowNodeState.FAILED } -> WorkflowState.FAILED
            states.any { it == WorkflowNodeState.RUNNING } -> WorkflowState.RUNNING
            states.any { it == WorkflowNodeState.WAITING } -> WorkflowState.READY
            else -> WorkflowState.BLOCKED
        }
        return workflow.copy(state = state, updatedAt = now())
    }

    private fun updateNode(workflow: MayraWorkflow, nodeId: String, transform: (WorkflowNode) -> WorkflowNode): MayraWorkflow =
        workflow.copy(nodes = workflow.nodes.map { if (it.id == nodeId) transform(it) else it })

    private companion object {
        val TERMINAL_STATES = setOf(WorkflowState.COMPLETED, WorkflowState.FAILED, WorkflowState.CANCELLED)
    }
}

data class WorkflowDiagnostics(
    val stored: Int,
    val active: Int,
    val running: Int,
    val blocked: Int,
    val completed: Int,
    val failed: Int,
    val executedNodes: Long,
    val nodeFailures: Long
)

enum class AutonomyDecision { EXECUTE, ASK, SCHEDULE, IGNORE, RETRY }

data class DecisionInput(
    val confidence: Double,
    val risk: Double,
    val urgency: Double,
    val contextRelevance: Double,
    val permissionReady: Boolean,
    val userAvailable: Boolean,
    val retryCount: Int = 0,
    val retryable: Boolean = false,
    val scheduledFor: Long? = null,
    val now: Long = System.currentTimeMillis()
)

data class DecisionScore(
    val decision: AutonomyDecision,
    val executeScore: Double,
    val askScore: Double,
    val scheduleScore: Double,
    val ignoreScore: Double,
    val retryScore: Double,
    val reason: String
)

class AutonomousDecisionEngine {
    fun decide(input: DecisionInput): DecisionScore {
        require(input.confidence in 0.0..1.0)
        require(input.risk in 0.0..1.0)
        require(input.urgency in 0.0..1.0)
        require(input.contextRelevance in 0.0..1.0)
        require(input.retryCount >= 0)

        val scheduled = input.scheduledFor?.let { it > input.now } == true
        val execute = weighted(
            input.confidence to 0.35,
            (1 - input.risk) to 0.30,
            input.contextRelevance to 0.20,
            input.urgency to 0.15
        ) * if (input.permissionReady) 1.0 else 0.15

        val ask = weighted(
            input.risk to 0.45,
            (1 - input.confidence) to 0.25,
            input.contextRelevance to 0.10,
            if (input.userAvailable) 1.0 else 0.2 to 0.20
        )
        val schedule = (if (scheduled) 0.85 else (1 - input.urgency) * 0.45) * input.contextRelevance
        val retry = if (input.retryable && input.retryCount < MAX_RETRIES) {
            (input.confidence * 0.40) + ((1 - input.risk) * 0.35) + ((1.0 / (input.retryCount + 1)) * 0.25)
        } else 0.0
        val ignore = ((1 - input.confidence) * 0.45) + ((1 - input.contextRelevance) * 0.40) + (input.risk * 0.15)

        val forced = when {
            scheduled -> AutonomyDecision.SCHEDULE
            !input.permissionReady && input.userAvailable -> AutonomyDecision.ASK
            input.risk >= 0.70 -> AutonomyDecision.ASK
            input.retryable && input.retryCount in 1 until MAX_RETRIES && retry >= execute -> AutonomyDecision.RETRY
            input.confidence < 0.20 || input.contextRelevance < 0.15 -> AutonomyDecision.IGNORE
            execute >= 0.62 && input.risk <= 0.45 -> AutonomyDecision.EXECUTE
            input.userAvailable -> AutonomyDecision.ASK
            else -> AutonomyDecision.SCHEDULE
        }

        return DecisionScore(
            decision = forced,
            executeScore = execute.coerceIn(0.0, 1.0),
            askScore = ask.coerceIn(0.0, 1.0),
            scheduleScore = schedule.coerceIn(0.0, 1.0),
            ignoreScore = ignore.coerceIn(0.0, 1.0),
            retryScore = retry.coerceIn(0.0, 1.0),
            reason = reason(forced, input)
        )
    }

    private fun weighted(vararg values: Pair<Double, Double>): Double = values.sumOf { (value, weight) -> value * weight }

    private fun reason(decision: AutonomyDecision, input: DecisionInput): String = when (decision) {
        AutonomyDecision.EXECUTE -> "Confidence, safety and context are strong enough for immediate execution"
        AutonomyDecision.ASK -> when {
            !input.permissionReady -> "Required permission is not ready"
            input.risk >= 0.70 -> "Risk is high, so explicit user confirmation is required"
            else -> "The decision is ambiguous and the user is available"
        }
        AutonomyDecision.SCHEDULE -> "The action is intended for later or the user is unavailable"
        AutonomyDecision.IGNORE -> "Confidence or context relevance is too low"
        AutonomyDecision.RETRY -> "The failure is retryable and the bounded retry budget remains"
    }

    companion object { const val MAX_RETRIES = 3 }
}
