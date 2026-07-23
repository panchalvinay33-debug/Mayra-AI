package ai.mayra.app.execution

import ai.mayra.app.agent.AgentRun
import ai.mayra.app.agent.AgentRunState
import ai.mayra.app.agent.MayraAgentRuntime
import ai.mayra.app.device.DeviceWorkDecision
import ai.mayra.app.device.MayraDeviceRuntime

interface ExecutionCheckpointStore {
    fun save(checkpoint: ExecutionCheckpoint)
    fun load(): ExecutionCheckpoint?
    fun clear()
}

class InMemoryExecutionCheckpointStore : ExecutionCheckpointStore {
    private var checkpoint: ExecutionCheckpoint? = null
    override fun save(checkpoint: ExecutionCheckpoint) { this.checkpoint = checkpoint }
    override fun load(): ExecutionCheckpoint? = checkpoint
    override fun clear() { checkpoint = null }
}

data class ExecutionDispatchResult(
    val request: ExecutionRequest?,
    val run: AgentRun?,
    val leased: Boolean = false,
    val executed: Boolean = false,
    val waiting: Boolean = false,
    val blockedReason: String? = null
)

data class ExecutionCoordinatorDiagnostics(
    val controlPlane: ExecutionDiagnostics,
    val persistedCheckpoints: Long,
    val restoredRequests: Long,
    val dispatches: Long,
    val blockedByDevice: Long,
    val agentErrors: Long
)

class MayraExecutionCoordinator(
    private val controlPlane: MayraExecutionControlPlane,
    private val agentRuntime: MayraAgentRuntime,
    private val deviceRuntime: MayraDeviceRuntime,
    private val checkpointStore: ExecutionCheckpointStore = InMemoryExecutionCheckpointStore(),
    private val ownerId: String = "mayra-execution",
    private val maxTicksPerDispatch: Int = 10
) {
    private var persistedCheckpoints = 0L
    private var restoredRequests = 0L
    private var dispatches = 0L
    private var blockedByDevice = 0L
    private var agentErrors = 0L

    init {
        require(ownerId.isNotBlank())
        require(maxTicksPerDispatch in 1..50)
    }

    @Synchronized
    fun enqueueRun(
        runId: String,
        title: String,
        priority: ExecutionPriority = ExecutionPriority.NORMAL,
        resources: Set<ExecutionResource> = emptySet(),
        conflictPolicy: ExecutionConflictPolicy = ExecutionConflictPolicy.WAIT,
        notBefore: Long = System.currentTimeMillis(),
        expiresAt: Long = System.currentTimeMillis() + ExecutionRequest.DEFAULT_TTL
    ): ExecutionRequest = controlPlane.enqueue(
        ExecutionRequest(
            runId = runId,
            title = title,
            priority = priority,
            resources = resources,
            conflictPolicy = conflictPolicy,
            notBefore = notBefore,
            expiresAt = expiresAt
        )
    ).also { persist() }

    suspend fun dispatchNext(): ExecutionDispatchResult {
        val available = availableResources()
        val lease = synchronized(this) { controlPlane.acquireNext(ownerId, available) }
            ?: return ExecutionDispatchResult(request = null, run = null, waiting = true)
        dispatches++

        val request = controlPlane.get(lease.requestId)
            ?: return ExecutionDispatchResult(request = null, run = null, leased = true, blockedReason = "Execution request disappeared")
        val run = agentRuntime.get(lease.runId)
        if (run == null) {
            agentErrors++
            val failed = controlPlane.fail(lease.requestId, ownerId, "Agent run was not found", retryable = false)
            persist()
            return ExecutionDispatchResult(failed, null, leased = true, blockedReason = "Agent run was not found")
        }

        val gate = deviceGate(request)
        if (!gate.allowed) {
            blockedByDevice++
            val failed = controlPlane.fail(lease.requestId, ownerId, gate.reason ?: "Device conditions are not suitable", retryable = gate.retryable)
            persist()
            return ExecutionDispatchResult(failed, run, leased = true, waiting = gate.retryable, blockedReason = gate.reason)
        }

        controlPlane.markRunning(lease.requestId, ownerId)
        var current = run
        return try {
            repeat(maxTicksPerDispatch) {
                controlPlane.heartbeat(lease.requestId, ownerId)
                val tick = agentRuntime.tick(current.id)
                current = tick.run
                controlPlane.updateFromAgent(lease.requestId, ownerId, current)
                if (current.state in TERMINAL_AGENT_STATES || tick.waitingForConfirmationStepId != null ||
                    tick.waitingForInputStepId != null || tick.waitingUntil != null || tick.idle) {
                    persist()
                    return ExecutionDispatchResult(
                        request = controlPlane.get(lease.requestId),
                        run = current,
                        leased = true,
                        executed = true,
                        waiting = current.state in setOf(AgentRunState.WAITING, AgentRunState.BLOCKED, AgentRunState.PAUSED)
                    )
                }
            }
            controlPlane.updateFromAgent(lease.requestId, ownerId, current)
            persist()
            ExecutionDispatchResult(controlPlane.get(lease.requestId), current, leased = true, executed = true)
        } catch (error: Throwable) {
            agentErrors++
            val failed = controlPlane.fail(
                lease.requestId,
                ownerId,
                error.message ?: "Agent execution failed",
                retryable = true
            )
            persist()
            ExecutionDispatchResult(failed, current, leased = true, blockedReason = error.message)
        }
    }

    suspend fun drain(maxDispatches: Int = 20): List<ExecutionDispatchResult> {
        require(maxDispatches in 1..100)
        val results = mutableListOf<ExecutionDispatchResult>()
        repeat(maxDispatches) {
            val result = dispatchNext()
            results += result
            if (!result.leased) return results
        }
        return results
    }

    @Synchronized
    fun cancel(requestId: String, reason: String = "Cancelled by user"): ExecutionRequest? {
        val request = controlPlane.cancel(requestId, reason) ?: return null
        agentRuntime.cancel(request.runId, reason)
        persist()
        return request
    }

    @Synchronized
    fun persist() {
        checkpointStore.save(controlPlane.checkpoint())
        persistedCheckpoints++
    }

    @Synchronized
    fun restore(): Int {
        val checkpoint = checkpointStore.load() ?: return 0
        val restored = controlPlane.restore(checkpoint)
        restoredRequests += restored
        return restored
    }

    @Synchronized
    fun clearCheckpoint() = checkpointStore.clear()

    @Synchronized
    fun diagnostics(): ExecutionCoordinatorDiagnostics = ExecutionCoordinatorDiagnostics(
        controlPlane = controlPlane.diagnostics(),
        persistedCheckpoints = persistedCheckpoints,
        restoredRequests = restoredRequests,
        dispatches = dispatches,
        blockedByDevice = blockedByDevice,
        agentErrors = agentErrors
    )

    private fun availableResources(): Set<ExecutionResource> {
        val analysis = deviceRuntime.latest()
        if (analysis == null) return ExecutionResource.entries.toSet()
        val blocked = mutableSetOf<ExecutionResource>()
        if (!analysis.snapshot.network.validated) blocked += ExecutionResource.NETWORK
        if (analysis.snapshot.thermal.name in setOf("SEVERE", "CRITICAL", "EMERGENCY", "SHUTDOWN")) blocked += ExecutionResource.CPU_HEAVY
        if (analysis.snapshot.storage.freePercent <= 2) blocked += ExecutionResource.STORAGE
        return ExecutionResource.entries.toSet() - blocked
    }

    private fun deviceGate(request: ExecutionRequest): DeviceWorkDecision {
        val needsNetwork = ExecutionResource.NETWORK in request.resources
        val heavy = ExecutionResource.CPU_HEAVY in request.resources
        return deviceRuntime.canRunWork(
            requiresNetwork = needsNetwork,
            allowMetered = request.priority >= ExecutionPriority.HIGH,
            heavy = heavy
        )
    }

    private companion object {
        val TERMINAL_AGENT_STATES = setOf(
            AgentRunState.COMPLETED,
            AgentRunState.FAILED,
            AgentRunState.CANCELLED
        )
    }
}
