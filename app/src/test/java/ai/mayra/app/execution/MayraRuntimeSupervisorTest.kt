package ai.mayra.app.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRuntimeSupervisorTest {
    @Test
    fun queuePressureProducesFindingAndDegradesHealth() {
        var now = 1_000_000L
        val plane = MayraExecutionControlPlane(now = { now })
        repeat(3) { index ->
            plane.enqueue(request("run-$index", "Task $index", now, ExecutionPriority.NORMAL))
        }
        val supervisor = MayraRuntimeSupervisor(
            controlPlane = plane,
            policy = SupervisorPolicy(queueWarningSize = 2, queueCriticalSize = 5),
            now = { now }
        )

        val report = supervisor.inspect()

        assertTrue(report.findings.any { it.code == "queue.warning" })
        assertTrue(report.score < 100)
        assertEquals(3, report.analytics.active)
    }

    @Test
    fun waitingWorkflowBecomesStaleFinding() {
        var now = 2_000_000L
        val plane = MayraExecutionControlPlane(now = { now }, leaseDurationMillis = 5_000L)
        val first = plane.enqueue(
            request("run-owner", "Owner", now, ExecutionPriority.HIGH, setOf(ExecutionResource.MICROPHONE))
        )
        plane.acquireNext("worker")
        val waiting = plane.enqueue(
            request("run-wait", "Waiting voice task", now, ExecutionPriority.NORMAL, setOf(ExecutionResource.MICROPHONE))
        )
        plane.acquireNext("worker-2")
        now += 31 * 60_000L
        val supervisor = MayraRuntimeSupervisor(
            plane,
            SupervisorPolicy(staleRunningMillis = 60_000L, staleWaitingMillis = 30 * 60_000L),
            now = { now }
        )

        val report = supervisor.inspect()

        assertTrue(report.findings.any { it.code == "waiting.stale" && it.requestId == waiting.id })
        assertFalse(report.findings.any { it.requestId == first.id && it.code == "waiting.stale" })
    }

    @Test
    fun terminalWorkflowIsArchivedAndSearchable() {
        val now = 3_000_000L
        val plane = MayraExecutionControlPlane(now = { now })
        val request = plane.enqueue(
            ExecutionRequest(
                runId = "trip-run",
                title = "Prepare Jaipur trip",
                createdAt = now,
                notBefore = now,
                expiresAt = now + 60_000L,
                tags = setOf("travel", "packing")
            )
        )
        plane.cancel(request.id, "User changed plans")
        val supervisor = MayraRuntimeSupervisor(plane, now = { now + 1_000L })

        supervisor.inspect()
        val results = supervisor.searchArchive("travel")

        assertEquals(1, results.size)
        assertEquals(ExecutionRequestState.CANCELLED, results.first().state)
        assertEquals("Prepare Jaipur trip", results.first().title)
    }

    @Test
    fun repeatedInspectionDeduplicatesRecentFinding() {
        val now = 4_000_000L
        val plane = MayraExecutionControlPlane(now = { now })
        repeat(3) { plane.enqueue(request("run-$it", "Task $it", now, ExecutionPriority.NORMAL)) }
        val supervisor = MayraRuntimeSupervisor(
            plane,
            SupervisorPolicy(queueWarningSize = 2, queueCriticalSize = 5, findingDedupeMillis = 60_000L),
            now = { now }
        )

        val first = supervisor.inspect()
        val second = supervisor.inspect()

        assertTrue(first.findings.any { it.code == "queue.warning" })
        assertTrue(second.findings.none { it.code == "queue.warning" })
    }

    @Test
    fun oldQueuedRequestReceivesBoundedPromotion() {
        var now = 5_000_000L
        val created = now
        val request = request("run-old", "Old task", created, ExecutionPriority.LOW)
        now += 25 * 60_000L
        val scheduler = MayraAdaptiveScheduler(
            policy = AdaptiveSchedulerPolicy(agingPromotionMillis = 20 * 60_000L),
            now = { now }
        )
        val analytics = emptyAnalytics()

        val recommendation = scheduler.recommend(request, null, analytics)

        assertEquals(SchedulingDecision.PROMOTE, recommendation.decision)
        assertEquals(ExecutionPriority.NORMAL, recommendation.recommendedPriority)
        assertTrue(recommendation.reasons.any { it.contains("promotion") })
    }

    @Test
    fun highRetryRequestRequiresReview() {
        val now = 6_000_000L
        val request = request("run-retry", "Retry task", now, ExecutionPriority.HIGH).copy(attempts = 4)
        val scheduler = MayraAdaptiveScheduler(now = { now })

        val recommendation = scheduler.recommend(request, null, emptyAnalytics())

        assertEquals(SchedulingDecision.REQUIRE_REVIEW, recommendation.decision)
        assertTrue(recommendation.score < ExecutionPriority.HIGH.weight)
    }

    @Test
    fun eventBusKeepsBoundedHistoryAndSurvivesListenerFailure() {
        val bus = RuntimeSupervisorEventBus(maxEvents = 50)
        bus.subscribe(RuntimeEventListener { error("listener failure") })
        repeat(70) { index ->
            bus.publish(
                RuntimeSupervisorEvent(
                    type = RuntimeEventType.INSPECTION,
                    message = "event $index",
                    timestamp = index.toLong()
                )
            )
        }

        val events = bus.recent(100)

        assertEquals(50, events.size)
        assertEquals("event 69", events.first().message)
    }

    private fun request(
        runId: String,
        title: String,
        now: Long,
        priority: ExecutionPriority,
        resources: Set<ExecutionResource> = emptySet()
    ) = ExecutionRequest(
        runId = runId,
        title = title,
        priority = priority,
        resources = resources,
        createdAt = now,
        notBefore = now,
        expiresAt = now + 24 * 60 * 60_000L
    )

    private fun emptyAnalytics() = ExecutionAnalytics(
        total = 0,
        active = 0,
        terminal = 0,
        successRatePercent = 100,
        failureRatePercent = 0,
        averageAttempts = 0.0,
        averageCompletedDurationMillis = null,
        queuedByPriority = ExecutionPriority.entries.associateWith { 0 },
        failuresByTag = emptyMap(),
        resourceDemand = emptyMap()
    )
}
