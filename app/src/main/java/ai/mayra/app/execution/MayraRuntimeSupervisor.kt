package ai.mayra.app.execution

import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.roundToInt

enum class SupervisorSeverity { INFO, LOW, MEDIUM, HIGH, CRITICAL }
enum class SupervisorAction { NONE, DISPATCH, RETRY, CANCEL_STALE, REDUCE_LOAD, WAIT_FOR_DEVICE, REVIEW_FAILURES, PRUNE_HISTORY }
enum class RuntimeHealthBand { HEALTHY, DEGRADED, UNHEALTHY, CRITICAL }

data class WorkflowArchiveEntry(
    val id: String = UUID.randomUUID().toString(),
    val requestId: String,
    val runId: String,
    val title: String,
    val state: ExecutionRequestState,
    val priority: ExecutionPriority,
    val attempts: Int,
    val createdAt: Long,
    val finishedAt: Long,
    val durationMillis: Long,
    val lastError: String? = null,
    val tags: Set<String> = emptySet()
)

data class RuntimeFinding(
    val code: String,
    val title: String,
    val message: String,
    val severity: SupervisorSeverity,
    val action: SupervisorAction = SupervisorAction.NONE,
    val requestId: String? = null,
    val createdAt: Long
)

data class ExecutionAnalytics(
    val total: Int,
    val active: Int,
    val terminal: Int,
    val successRatePercent: Int,
    val failureRatePercent: Int,
    val averageAttempts: Double,
    val averageCompletedDurationMillis: Long?,
    val queuedByPriority: Map<ExecutionPriority, Int>,
    val failuresByTag: Map<String, Int>,
    val resourceDemand: Map<ExecutionResource, Int>
)

data class RuntimeHealthReport(
    val score: Int,
    val band: RuntimeHealthBand,
    val analytics: ExecutionAnalytics,
    val findings: List<RuntimeFinding>,
    val generatedAt: Long
)

data class SupervisorPolicy(
    val staleRunningMillis: Long = 5 * 60_000L,
    val staleWaitingMillis: Long = 30 * 60_000L,
    val queueWarningSize: Int = 20,
    val queueCriticalSize: Int = 75,
    val failureWarningPercent: Int = 25,
    val retryWarningAverage: Double = 2.5,
    val maxArchiveEntries: Int = 500,
    val findingDedupeMillis: Long = 10 * 60_000L
) {
    init {
        require(staleRunningMillis in 30_000L..24 * 60 * 60_000L)
        require(staleWaitingMillis >= staleRunningMillis)
        require(queueWarningSize in 1 until queueCriticalSize)
        require(failureWarningPercent in 1..100)
        require(retryWarningAverage in 1.0..10.0)
        require(maxArchiveEntries in 25..5_000)
        require(findingDedupeMillis >= 1_000L)
    }
}

class MayraRuntimeSupervisor(
    private val controlPlane: MayraExecutionControlPlane,
    private val policy: SupervisorPolicy = SupervisorPolicy(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val archive = ArrayDeque<WorkflowArchiveEntry>()
    private val findingHistory = ArrayDeque<RuntimeFinding>()
    private val archivedRequestIds = mutableSetOf<String>()

    @Synchronized
    fun inspect(): RuntimeHealthReport {
        val timestamp = now()
        val requests = controlPlane.snapshot()
        archiveTerminalRequests(requests, timestamp)
        val analytics = analytics(requests)
        val findings = buildFindings(requests, analytics, timestamp)
            .filterNot { duplicateFinding(it, timestamp) }
        findings.forEach(findingHistory::addLast)
        while (findingHistory.size > MAX_FINDINGS) findingHistory.removeFirst()
        val score = score(analytics, findings)
        return RuntimeHealthReport(
            score = score,
            band = when {
                score >= 85 -> RuntimeHealthBand.HEALTHY
                score >= 65 -> RuntimeHealthBand.DEGRADED
                score >= 40 -> RuntimeHealthBand.UNHEALTHY
                else -> RuntimeHealthBand.CRITICAL
            },
            analytics = analytics,
            findings = findings,
            generatedAt = timestamp
        )
    }

    @Synchronized
    fun archive(limit: Int = 100): List<WorkflowArchiveEntry> =
        archive.toList().takeLast(limit.coerceIn(1, policy.maxArchiveEntries)).asReversed()

    @Synchronized
    fun searchArchive(query: String, limit: Int = 50): List<WorkflowArchiveEntry> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return archive(limit)
        return archive.asSequence()
            .filter { entry ->
                entry.title.lowercase().contains(normalized) ||
                    entry.state.name.lowercase().contains(normalized) ||
                    entry.tags.any { it.lowercase().contains(normalized) } ||
                    entry.lastError.orEmpty().lowercase().contains(normalized)
            }
            .takeLast(limit.coerceIn(1, policy.maxArchiveEntries))
            .toList()
            .asReversed()
    }

    @Synchronized
    fun recentFindings(limit: Int = 50): List<RuntimeFinding> =
        findingHistory.toList().takeLast(limit.coerceIn(1, MAX_FINDINGS)).asReversed()

    @Synchronized
    fun pruneArchive(olderThan: Long): Int {
        val retained = archive.filter { it.finishedAt >= olderThan }
        val removed = archive.size - retained.size
        archive.clear()
        retained.takeLast(policy.maxArchiveEntries).forEach(archive::addLast)
        archivedRequestIds.retainAll(archive.mapTo(mutableSetOf()) { it.requestId })
        return removed
    }

    private fun analytics(requests: List<ExecutionRequest>): ExecutionAnalytics {
        val terminalStates = setOf(
            ExecutionRequestState.COMPLETED,
            ExecutionRequestState.FAILED,
            ExecutionRequestState.CANCELLED,
            ExecutionRequestState.EXPIRED
        )
        val terminal = requests.filter { it.state in terminalStates }
        val completed = terminal.count { it.state == ExecutionRequestState.COMPLETED }
        val failed = terminal.count { it.state == ExecutionRequestState.FAILED || it.state == ExecutionRequestState.EXPIRED }
        val durations = archive.filter { it.state == ExecutionRequestState.COMPLETED }.map { it.durationMillis }
        val failuresByTag = terminal.asSequence()
            .filter { it.state == ExecutionRequestState.FAILED || it.state == ExecutionRequestState.EXPIRED }
            .flatMap { request -> (request.tags.ifEmpty { setOf("untagged") }).asSequence() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(20)
            .toMap()
        return ExecutionAnalytics(
            total = requests.size,
            active = requests.count { it.state !in terminalStates },
            terminal = terminal.size,
            successRatePercent = if (terminal.isEmpty()) 100 else (completed * 100.0 / terminal.size).roundToInt(),
            failureRatePercent = if (terminal.isEmpty()) 0 else (failed * 100.0 / terminal.size).roundToInt(),
            averageAttempts = if (requests.isEmpty()) 0.0 else requests.map { it.attempts }.average(),
            averageCompletedDurationMillis = durations.takeIf { it.isNotEmpty() }?.average()?.roundToInt()?.toLong(),
            queuedByPriority = ExecutionPriority.entries.associateWith { priority ->
                requests.count { it.priority == priority && it.state in setOf(ExecutionRequestState.QUEUED, ExecutionRequestState.WAITING) }
            },
            failuresByTag = failuresByTag,
            resourceDemand = ExecutionResource.entries.associateWith { resource ->
                requests.count { it.state !in terminalStates && resource in it.resources }
            }.filterValues { it > 0 }
        )
    }

    private fun buildFindings(
        requests: List<ExecutionRequest>,
        analytics: ExecutionAnalytics,
        timestamp: Long
    ): List<RuntimeFinding> {
        val findings = mutableListOf<RuntimeFinding>()
        val queued = requests.count { it.state in setOf(ExecutionRequestState.QUEUED, ExecutionRequestState.WAITING) }
        if (queued >= policy.queueCriticalSize) findings += RuntimeFinding(
            "queue.critical", "Execution queue is critically large",
            "$queued workflows are waiting. Pause non-essential work and reduce load.",
            SupervisorSeverity.CRITICAL, SupervisorAction.REDUCE_LOAD, createdAt = timestamp
        ) else if (queued >= policy.queueWarningSize) findings += RuntimeFinding(
            "queue.warning", "Execution queue is growing",
            "$queued workflows are queued or waiting.",
            SupervisorSeverity.MEDIUM, SupervisorAction.DISPATCH, createdAt = timestamp
        )
        if (analytics.terminal >= 4 && analytics.failureRatePercent >= policy.failureWarningPercent) findings += RuntimeFinding(
            "failure.rate", "Workflow failure rate is elevated",
            "${analytics.failureRatePercent}% of terminal workflows failed or expired.",
            if (analytics.failureRatePercent >= 50) SupervisorSeverity.HIGH else SupervisorSeverity.MEDIUM,
            SupervisorAction.REVIEW_FAILURES,
            createdAt = timestamp
        )
        if (analytics.averageAttempts >= policy.retryWarningAverage) findings += RuntimeFinding(
            "retry.pressure", "Workflow retry pressure is high",
            "Average attempts are ${"%.1f".format(analytics.averageAttempts)} per request.",
            SupervisorSeverity.MEDIUM, SupervisorAction.REVIEW_FAILURES, createdAt = timestamp
        )
        requests.filter { it.state == ExecutionRequestState.RUNNING && timestamp - it.createdAt >= policy.staleRunningMillis }
            .forEach { findings += RuntimeFinding(
                "running.stale", "Possible stalled workflow", "${it.title} has remained running longer than expected.",
                SupervisorSeverity.HIGH, SupervisorAction.CANCEL_STALE, it.id, timestamp
            ) }
        requests.filter { it.state in setOf(ExecutionRequestState.WAITING, ExecutionRequestState.BLOCKED) && timestamp - it.createdAt >= policy.staleWaitingMillis }
            .forEach { findings += RuntimeFinding(
                "waiting.stale", "Workflow has been waiting too long", "${it.title} may need input, connectivity or manual review.",
                SupervisorSeverity.MEDIUM, SupervisorAction.RETRY, it.id, timestamp
            ) }
        val busiestResource = analytics.resourceDemand.maxByOrNull { it.value }
        if (busiestResource != null && busiestResource.value >= policy.queueWarningSize) findings += RuntimeFinding(
            "resource.hotspot.${busiestResource.key.name.lowercase()}",
            "Execution resource hotspot detected",
            "${busiestResource.value} active workflows require ${busiestResource.key.name.lowercase()}.",
            SupervisorSeverity.MEDIUM, SupervisorAction.REDUCE_LOAD, createdAt = timestamp
        )
        return findings
    }

    private fun score(analytics: ExecutionAnalytics, findings: List<RuntimeFinding>): Int {
        var score = 100
        score -= (analytics.failureRatePercent * 0.35).roundToInt()
        score -= ((analytics.averageAttempts - 1.0).coerceAtLeast(0.0) * 6).roundToInt()
        findings.forEach { finding ->
            score -= when (finding.severity) {
                SupervisorSeverity.INFO -> 0
                SupervisorSeverity.LOW -> 2
                SupervisorSeverity.MEDIUM -> 6
                SupervisorSeverity.HIGH -> 12
                SupervisorSeverity.CRITICAL -> 22
            }
        }
        return score.coerceIn(0, 100)
    }

    private fun archiveTerminalRequests(requests: List<ExecutionRequest>, timestamp: Long) {
        val terminalStates = setOf(
            ExecutionRequestState.COMPLETED,
            ExecutionRequestState.FAILED,
            ExecutionRequestState.CANCELLED,
            ExecutionRequestState.EXPIRED
        )
        requests.filter { it.state in terminalStates && it.id !in archivedRequestIds }.forEach { request ->
            val finishedAt = controlPlane.events(request.id, 1).firstOrNull()?.timestamp ?: timestamp
            archive.addLast(
                WorkflowArchiveEntry(
                    requestId = request.id,
                    runId = request.runId,
                    title = request.title,
                    state = request.state,
                    priority = request.priority,
                    attempts = request.attempts,
                    createdAt = request.createdAt,
                    finishedAt = finishedAt,
                    durationMillis = (finishedAt - request.createdAt).coerceAtLeast(0L),
                    lastError = request.lastError,
                    tags = request.tags
                )
            )
            archivedRequestIds += request.id
        }
        while (archive.size > policy.maxArchiveEntries) {
            val removed = archive.removeFirst()
            archivedRequestIds -= removed.requestId
        }
    }

    private fun duplicateFinding(finding: RuntimeFinding, timestamp: Long): Boolean = findingHistory.any {
        it.code == finding.code && it.requestId == finding.requestId && timestamp - it.createdAt < policy.findingDedupeMillis
    }

    companion object { private const val MAX_FINDINGS = 300 }
}
