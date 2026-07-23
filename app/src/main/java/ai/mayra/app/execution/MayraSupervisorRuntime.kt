package ai.mayra.app.execution

import ai.mayra.app.MayraRuntime

data class SupervisorRuntimeSnapshot(
    val health: RuntimeHealthReport,
    val recommendations: List<SchedulingRecommendation>,
    val archive: List<WorkflowArchiveEntry>,
    val events: List<RuntimeSupervisorEvent>
)

/**
 * Shared supervisor facade for UI, voice, diagnostics and future workers.
 * It remains advisory: it does not silently cancel, promote or execute workflows.
 */
object MayraSupervisorRuntime {
    private val eventBus by lazy { RuntimeSupervisorEventBus() }
    private val supervisor by lazy {
        check(MayraRuntime.installed) { "Mayra runtime is not installed" }
        MayraRuntimeSupervisor(MayraRuntime.executionControlPlane)
    }
    private val scheduler by lazy { MayraAdaptiveScheduler(eventBus = eventBus) }

    @Synchronized
    fun inspect(recommendationLimit: Int = 25): SupervisorRuntimeSnapshot {
        require(recommendationLimit in 1..100)
        val health = supervisor.inspect()
        eventBus.publish(
            RuntimeSupervisorEvent(
                type = RuntimeEventType.INSPECTION,
                message = "Runtime inspection completed with ${health.band.name.lowercase()} health.",
                timestamp = health.generatedAt,
                attributes = mapOf(
                    "score" to health.score.toString(),
                    "active" to health.analytics.active.toString(),
                    "terminal" to health.analytics.terminal.toString()
                )
            )
        )
        health.findings.forEach { finding ->
            eventBus.publish(
                RuntimeSupervisorEvent(
                    type = RuntimeEventType.FINDING,
                    message = finding.message,
                    requestId = finding.requestId,
                    timestamp = finding.createdAt,
                    attributes = mapOf(
                        "code" to finding.code,
                        "severity" to finding.severity.name,
                        "action" to finding.action.name
                    )
                )
            )
        }
        val recommendations = scheduler.recommendAll(
            requests = MayraRuntime.executionControlPlane.snapshot(),
            device = MayraRuntime.deviceRuntime.latest(),
            analytics = health.analytics,
            limit = recommendationLimit
        )
        return SupervisorRuntimeSnapshot(
            health = health,
            recommendations = recommendations,
            archive = supervisor.archive(100),
            events = eventBus.recent(100)
        )
    }

    fun archive(limit: Int = 100): List<WorkflowArchiveEntry> = supervisor.archive(limit)

    fun searchArchive(query: String, limit: Int = 50): List<WorkflowArchiveEntry> =
        supervisor.searchArchive(query, limit)

    fun recentFindings(limit: Int = 50): List<RuntimeFinding> = supervisor.recentFindings(limit)

    fun recentEvents(limit: Int = 100): List<RuntimeSupervisorEvent> = eventBus.recent(limit)

    fun recommendation(requestId: String): SchedulingRecommendation? {
        val health = supervisor.inspect()
        val request = MayraRuntime.executionControlPlane.get(requestId) ?: return null
        return scheduler.recommend(request, MayraRuntime.deviceRuntime.latest(), health.analytics)
    }

    @Synchronized
    fun maintenance(archiveOlderThan: Long): Int {
        val removed = supervisor.pruneArchive(archiveOlderThan)
        eventBus.publish(
            RuntimeSupervisorEvent(
                type = RuntimeEventType.MAINTENANCE,
                message = "Supervisor maintenance removed $removed archived workflows.",
                timestamp = System.currentTimeMillis(),
                attributes = mapOf("removed" to removed.toString())
            )
        )
        return removed
    }
}
