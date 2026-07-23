package ai.mayra.app.execution

import ai.mayra.app.agent.AgentRun
import ai.mayra.app.agent.AgentRunState
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

enum class ExecutionPriority(val weight: Int) { LOW(10), NORMAL(30), HIGH(60), URGENT(100) }
enum class ExecutionRequestState { QUEUED, LEASED, RUNNING, WAITING, BLOCKED, COMPLETED, FAILED, CANCELLED, EXPIRED }
enum class ExecutionConflictPolicy { WAIT, REJECT, PREEMPT_LOWER_PRIORITY }
enum class ExecutionResource { NETWORK, MICROPHONE, CAMERA, LOCATION, CALENDAR, COMMUNICATION, STORAGE, CPU_HEAVY }

data class ExecutionRequest(
    val id: String = UUID.randomUUID().toString(),
    val runId: String,
    val title: String,
    val priority: ExecutionPriority = ExecutionPriority.NORMAL,
    val resources: Set<ExecutionResource> = emptySet(),
    val conflictPolicy: ExecutionConflictPolicy = ExecutionConflictPolicy.WAIT,
    val createdAt: Long = System.currentTimeMillis(),
    val notBefore: Long = createdAt,
    val expiresAt: Long = createdAt + DEFAULT_TTL,
    val state: ExecutionRequestState = ExecutionRequestState.QUEUED,
    val attempts: Int = 0,
    val maxAttempts: Int = 5,
    val leaseOwner: String? = null,
    val leaseExpiresAt: Long? = null,
    val lastError: String? = null,
    val tags: Set<String> = emptySet()
) {
    init {
        require(runId.isNotBlank())
        require(title.isNotBlank())
        require(expiresAt > createdAt)
        require(notBefore in createdAt..expiresAt)
        require(maxAttempts in 1..10)
        require(attempts in 0..maxAttempts)
        require(tags.size <= 20)
    }

    companion object { const val DEFAULT_TTL = 7L * 24 * 60 * 60 * 1000 }
}

data class ExecutionLease(
    val requestId: String,
    val runId: String,
    val owner: String,
    val acquiredAt: Long,
    val expiresAt: Long,
    val resources: Set<ExecutionResource>
)

data class ExecutionProgressEvent(
    val id: String = UUID.randomUUID().toString(),
    val requestId: String,
    val runId: String,
    val state: ExecutionRequestState,
    val message: String,
    val timestamp: Long,
    val progressPercent: Int? = null
) {
    init {
        require(message.isNotBlank())
        require(progressPercent == null || progressPercent in 0..100)
    }
}

data class ExecutionCheckpoint(
    val version: Int = 1,
    val requests: List<ExecutionRequest>,
    val events: List<ExecutionProgressEvent>,
    val createdAt: Long
)

data class ExecutionDiagnostics(
    val queued: Int,
    val leased: Int,
    val running: Int,
    val waiting: Int,
    val blocked: Int,
    val completed: Int,
    val failed: Int,
    val cancelled: Int,
    val expired: Int,
    val leasesGranted: Long,
    val leaseRecoveries: Long,
    val conflictsDetected: Long,
    val preemptions: Long
)

class MayraExecutionControlPlane(
    private val now: () -> Long = System::currentTimeMillis,
    private val leaseDurationMillis: Long = 60_000L,
    private val maxStoredRequests: Int = 300,
    private val maxStoredEvents: Int = 1_000
) {
    private val requests = linkedMapOf<String, ExecutionRequest>()
    private val events = ArrayDeque<ExecutionProgressEvent>()
    private val leasesGranted = AtomicLong(0)
    private val leaseRecoveries = AtomicLong(0)
    private val conflictsDetected = AtomicLong(0)
    private val preemptions = AtomicLong(0)

    init {
        require(leaseDurationMillis in 5_000L..10 * 60_000L)
        require(maxStoredRequests in 10..2_000)
        require(maxStoredEvents in 50..10_000)
    }

    @Synchronized
    fun enqueue(request: ExecutionRequest): ExecutionRequest {
        require(request.id !in requests) { "Duplicate execution request id" }
        val timestamp = now()
        require(request.expiresAt > timestamp) { "Execution request has expired" }
        val queued = request.copy(state = ExecutionRequestState.QUEUED, leaseOwner = null, leaseExpiresAt = null)
        requests[queued.id] = queued
        appendEvent(queued, ExecutionRequestState.QUEUED, "Execution queued.", timestamp, 0)
        prune()
        return queued
    }

    @Synchronized
    fun get(requestId: String): ExecutionRequest? = requests[requestId]

    @Synchronized
    fun snapshot(): List<ExecutionRequest> = requests.values.sortedWith(
        compareByDescending<ExecutionRequest> { it.priority.weight }.thenBy { it.createdAt }
    )

    @Synchronized
    fun acquireNext(owner: String, availableResources: Set<ExecutionResource> = ExecutionResource.entries.toSet()): ExecutionLease? {
        require(owner.isNotBlank())
        val timestamp = now()
        recoverExpiredLeases(timestamp)
        expireDue(timestamp)
        val candidates = requests.values
            .filter { it.state in setOf(ExecutionRequestState.QUEUED, ExecutionRequestState.WAITING) }
            .filter { it.notBefore <= timestamp && it.expiresAt > timestamp }
            .filter { it.resources.all(availableResources::contains) }
            .sortedWith(compareByDescending<ExecutionRequest> { it.priority.weight }.thenBy { it.createdAt })

        for (candidate in candidates) {
            val conflicts = activeRequests().filter { it.id != candidate.id && it.resources.any(candidate.resources::contains) }
            if (conflicts.isNotEmpty()) {
                conflictsDetected.incrementAndGet()
                when (candidate.conflictPolicy) {
                    ExecutionConflictPolicy.REJECT -> {
                        val rejected = candidate.copy(state = ExecutionRequestState.FAILED, lastError = "Execution resource conflict")
                        requests[candidate.id] = rejected
                        appendEvent(rejected, rejected.state, "Execution rejected due to a resource conflict.", timestamp)
                        continue
                    }
                    ExecutionConflictPolicy.WAIT -> {
                        val waiting = candidate.copy(state = ExecutionRequestState.WAITING, lastError = "Waiting for execution resources")
                        requests[candidate.id] = waiting
                        continue
                    }
                    ExecutionConflictPolicy.PREEMPT_LOWER_PRIORITY -> {
                        val canPreempt = conflicts.all { it.priority.weight < candidate.priority.weight }
                        if (!canPreempt) {
                            val waiting = candidate.copy(state = ExecutionRequestState.WAITING, lastError = "Higher or equal priority work owns required resources")
                            requests[candidate.id] = waiting
                            continue
                        }
                        conflicts.forEach { conflict ->
                            val paused = conflict.copy(
                                state = ExecutionRequestState.WAITING,
                                leaseOwner = null,
                                leaseExpiresAt = null,
                                lastError = "Preempted by higher-priority execution"
                            )
                            requests[conflict.id] = paused
                            appendEvent(paused, paused.state, "Execution preempted by higher-priority work.", timestamp)
                            preemptions.incrementAndGet()
                        }
                    }
                }
            }

            val leased = candidate.copy(
                state = ExecutionRequestState.LEASED,
                leaseOwner = owner,
                leaseExpiresAt = timestamp + leaseDurationMillis,
                attempts = candidate.attempts + 1,
                lastError = null
            )
            requests[candidate.id] = leased
            leasesGranted.incrementAndGet()
            appendEvent(leased, leased.state, "Execution lease acquired.", timestamp)
            return ExecutionLease(leased.id, leased.runId, owner, timestamp, requireNotNull(leased.leaseExpiresAt), leased.resources)
        }
        return null
    }

    @Synchronized
    fun markRunning(requestId: String, owner: String): ExecutionRequest? = mutateOwned(requestId, owner) { request, timestamp ->
        request.copy(state = ExecutionRequestState.RUNNING, leaseExpiresAt = timestamp + leaseDurationMillis, lastError = null)
            .also { appendEvent(it, it.state, "Execution started.", timestamp, 1) }
    }

    @Synchronized
    fun heartbeat(requestId: String, owner: String): Boolean {
        val request = requests[requestId] ?: return false
        if (request.leaseOwner != owner || request.state !in setOf(ExecutionRequestState.LEASED, ExecutionRequestState.RUNNING)) return false
        requests[requestId] = request.copy(leaseExpiresAt = now() + leaseDurationMillis)
        return true
    }

    @Synchronized
    fun updateFromAgent(requestId: String, owner: String, run: AgentRun): ExecutionRequest? = mutateOwned(requestId, owner) { request, timestamp ->
        val mapped = when (run.state) {
            AgentRunState.DRAFT, AgentRunState.READY -> ExecutionRequestState.QUEUED
            AgentRunState.RUNNING, AgentRunState.COMPENSATING -> ExecutionRequestState.RUNNING
            AgentRunState.WAITING, AgentRunState.PAUSED -> ExecutionRequestState.WAITING
            AgentRunState.BLOCKED -> ExecutionRequestState.BLOCKED
            AgentRunState.COMPLETED -> ExecutionRequestState.COMPLETED
            AgentRunState.FAILED -> ExecutionRequestState.FAILED
            AgentRunState.CANCELLED -> ExecutionRequestState.CANCELLED
        }
        val terminal = mapped in TERMINAL_STATES
        request.copy(
            state = mapped,
            leaseOwner = if (terminal || mapped in setOf(ExecutionRequestState.WAITING, ExecutionRequestState.BLOCKED)) null else owner,
            leaseExpiresAt = if (terminal || mapped in setOf(ExecutionRequestState.WAITING, ExecutionRequestState.BLOCKED)) null else timestamp + leaseDurationMillis,
            lastError = run.lastError
        ).also {
            val progress = progressOf(run)
            appendEvent(it, mapped, "Agent run is ${mapped.name.lowercase()}.", timestamp, progress)
        }
    }

    @Synchronized
    fun fail(requestId: String, owner: String, error: String, retryable: Boolean): ExecutionRequest? = mutateOwned(requestId, owner) { request, timestamp ->
        val retry = retryable && request.attempts < request.maxAttempts && request.expiresAt > timestamp
        request.copy(
            state = if (retry) ExecutionRequestState.WAITING else ExecutionRequestState.FAILED,
            notBefore = if (retry) (timestamp + retryDelay(request.attempts)) else request.notBefore,
            leaseOwner = null,
            leaseExpiresAt = null,
            lastError = error.take(500)
        ).also { appendEvent(it, it.state, if (retry) "Execution will retry." else "Execution failed.", timestamp) }
    }

    @Synchronized
    fun cancel(requestId: String, reason: String = "Cancelled by user"): ExecutionRequest? {
        val request = requests[requestId] ?: return null
        if (request.state in TERMINAL_STATES) return request
        val cancelled = request.copy(
            state = ExecutionRequestState.CANCELLED,
            leaseOwner = null,
            leaseExpiresAt = null,
            lastError = reason.take(500)
        )
        requests[requestId] = cancelled
        appendEvent(cancelled, cancelled.state, "Execution cancelled.", now())
        return cancelled
    }

    @Synchronized
    fun events(requestId: String? = null, limit: Int = 100): List<ExecutionProgressEvent> = events
        .asSequence()
        .filter { requestId == null || it.requestId == requestId }
        .takeLast(limit.coerceIn(1, 500))
        .toList()
        .asReversed()

    @Synchronized
    fun checkpoint(): ExecutionCheckpoint = ExecutionCheckpoint(
        requests = requests.values.toList(),
        events = events.toList(),
        createdAt = now()
    )

    @Synchronized
    fun restore(checkpoint: ExecutionCheckpoint): Int {
        require(checkpoint.version == 1)
        requests.clear()
        events.clear()
        checkpoint.requests.takeLast(maxStoredRequests).forEach { request ->
            val restored = if (request.state in setOf(ExecutionRequestState.LEASED, ExecutionRequestState.RUNNING)) {
                request.copy(state = ExecutionRequestState.WAITING, leaseOwner = null, leaseExpiresAt = null, lastError = "Recovered after process restart")
            } else request
            requests[restored.id] = restored
        }
        checkpoint.events.takeLast(maxStoredEvents).forEach(events::addLast)
        expireDue(now())
        return requests.size
    }

    @Synchronized
    fun diagnostics(): ExecutionDiagnostics {
        val all = requests.values
        return ExecutionDiagnostics(
            queued = all.count { it.state == ExecutionRequestState.QUEUED },
            leased = all.count { it.state == ExecutionRequestState.LEASED },
            running = all.count { it.state == ExecutionRequestState.RUNNING },
            waiting = all.count { it.state == ExecutionRequestState.WAITING },
            blocked = all.count { it.state == ExecutionRequestState.BLOCKED },
            completed = all.count { it.state == ExecutionRequestState.COMPLETED },
            failed = all.count { it.state == ExecutionRequestState.FAILED },
            cancelled = all.count { it.state == ExecutionRequestState.CANCELLED },
            expired = all.count { it.state == ExecutionRequestState.EXPIRED },
            leasesGranted = leasesGranted.get(),
            leaseRecoveries = leaseRecoveries.get(),
            conflictsDetected = conflictsDetected.get(),
            preemptions = preemptions.get()
        )
    }

    private fun activeRequests(): List<ExecutionRequest> = requests.values.filter {
        it.state in setOf(ExecutionRequestState.LEASED, ExecutionRequestState.RUNNING)
    }

    private fun mutateOwned(
        requestId: String,
        owner: String,
        transform: (ExecutionRequest, Long) -> ExecutionRequest
    ): ExecutionRequest? {
        val request = requests[requestId] ?: return null
        if (request.leaseOwner != owner) return null
        val updated = transform(request, now())
        requests[requestId] = updated
        return updated
    }

    private fun recoverExpiredLeases(timestamp: Long) {
        requests.values.toList().forEach { request ->
            if (request.state in setOf(ExecutionRequestState.LEASED, ExecutionRequestState.RUNNING) && request.leaseExpiresAt?.let { it <= timestamp } == true) {
                val recovered = request.copy(
                    state = if (request.attempts < request.maxAttempts) ExecutionRequestState.WAITING else ExecutionRequestState.FAILED,
                    leaseOwner = null,
                    leaseExpiresAt = null,
                    notBefore = timestamp,
                    lastError = "Execution lease expired"
                )
                requests[request.id] = recovered
                appendEvent(recovered, recovered.state, "Expired execution lease recovered.", timestamp)
                leaseRecoveries.incrementAndGet()
            }
        }
    }

    private fun expireDue(timestamp: Long) {
        requests.values.toList().forEach { request ->
            if (request.state !in TERMINAL_STATES && request.expiresAt <= timestamp) {
                val expired = request.copy(state = ExecutionRequestState.EXPIRED, leaseOwner = null, leaseExpiresAt = null, lastError = "Execution request expired")
                requests[request.id] = expired
                appendEvent(expired, expired.state, "Execution expired.", timestamp)
            }
        }
    }

    private fun appendEvent(request: ExecutionRequest, state: ExecutionRequestState, message: String, timestamp: Long, progress: Int? = null) {
        events.addLast(ExecutionProgressEvent(requestId = request.id, runId = request.runId, state = state, message = message, timestamp = timestamp, progressPercent = progress))
        while (events.size > maxStoredEvents) events.removeFirst()
    }

    private fun progressOf(run: AgentRun): Int {
        if (run.steps.isEmpty()) return 0
        val done = run.steps.count { it.state.name in setOf("COMPLETED", "SKIPPED", "COMPENSATED") }
        return ((done * 100.0) / run.steps.size).toInt().coerceIn(0, 100)
    }

    private fun retryDelay(attempts: Int): Long = (1_000L shl attempts.coerceIn(0, 6)).coerceAtMost(60_000L)

    private fun prune() {
        if (requests.size <= maxStoredRequests) return
        requests.values
            .filter { it.state in TERMINAL_STATES }
            .sortedBy { it.createdAt }
            .take(requests.size - maxStoredRequests)
            .forEach { requests.remove(it.id) }
    }

    private companion object {
        val TERMINAL_STATES = setOf(
            ExecutionRequestState.COMPLETED,
            ExecutionRequestState.FAILED,
            ExecutionRequestState.CANCELLED,
            ExecutionRequestState.EXPIRED
        )
    }
}
