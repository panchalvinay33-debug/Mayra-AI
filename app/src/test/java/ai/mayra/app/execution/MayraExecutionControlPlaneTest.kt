package ai.mayra.app.execution

import ai.mayra.app.agent.AgentPlan
import ai.mayra.app.agent.AgentRun
import ai.mayra.app.agent.AgentRunState
import ai.mayra.app.agent.AgentStep
import ai.mayra.app.agent.AgentStepKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MayraExecutionControlPlaneTest {
    private var clock = 1_000_000L

    @Test
    fun `urgent request is leased before normal request`() {
        val plane = plane()
        plane.enqueue(request("normal", ExecutionPriority.NORMAL))
        val urgent = plane.enqueue(request("urgent", ExecutionPriority.URGENT))

        val lease = assertNotNull(plane.acquireNext("worker"))

        assertEquals(urgent.id, lease.requestId)
    }

    @Test
    fun `conflicting request waits while resource is leased`() {
        val plane = plane()
        val first = plane.enqueue(request("camera-one", resources = setOf(ExecutionResource.CAMERA)))
        plane.enqueue(request("camera-two", resources = setOf(ExecutionResource.CAMERA)))
        assertEquals(first.id, plane.acquireNext("worker-a")?.requestId)

        val secondLease = plane.acquireNext("worker-b")

        assertNull(secondLease)
        assertEquals(1, plane.diagnostics().waiting)
        assertTrue(plane.diagnostics().conflictsDetected >= 1)
    }

    @Test
    fun `urgent work preempts lower priority conflicting lease`() {
        val plane = plane()
        val low = plane.enqueue(request("low", ExecutionPriority.LOW, setOf(ExecutionResource.MICROPHONE)))
        assertEquals(low.id, plane.acquireNext("worker-a")?.requestId)
        val urgent = plane.enqueue(
            request(
                title = "urgent",
                priority = ExecutionPriority.URGENT,
                resources = setOf(ExecutionResource.MICROPHONE),
                conflictPolicy = ExecutionConflictPolicy.PREEMPT_LOWER_PRIORITY
            )
        )

        val lease = assertNotNull(plane.acquireNext("worker-b"))

        assertEquals(urgent.id, lease.requestId)
        assertEquals(ExecutionRequestState.WAITING, plane.get(low.id)?.state)
        assertEquals(1, plane.diagnostics().preemptions)
    }

    @Test
    fun `expired lease is recovered and can be leased again`() {
        val plane = plane(leaseMillis = 5_000)
        val request = plane.enqueue(request("recover"))
        assertNotNull(plane.acquireNext("worker-a"))
        clock += 5_001

        val recovered = assertNotNull(plane.acquireNext("worker-b"))

        assertEquals(request.id, recovered.requestId)
        assertEquals(2, plane.get(request.id)?.attempts)
        assertEquals(1, plane.diagnostics().leaseRecoveries)
    }

    @Test
    fun `retryable failure waits with bounded backoff`() {
        val plane = plane()
        val request = plane.enqueue(request("retry"))
        assertNotNull(plane.acquireNext("worker"))

        val failed = assertNotNull(plane.fail(request.id, "worker", "temporary", retryable = true))

        assertEquals(ExecutionRequestState.WAITING, failed.state)
        assertTrue(failed.notBefore > clock)
        assertEquals("temporary", failed.lastError)
    }

    @Test
    fun `checkpoint restore converts running work to waiting`() {
        val source = plane()
        val request = source.enqueue(request("restart"))
        assertNotNull(source.acquireNext("worker"))
        source.markRunning(request.id, "worker")
        val checkpoint = source.checkpoint()
        val restored = plane()

        assertEquals(1, restored.restore(checkpoint))

        assertEquals(ExecutionRequestState.WAITING, restored.get(request.id)?.state)
        assertNull(restored.get(request.id)?.leaseOwner)
        assertEquals("Recovered after process restart", restored.get(request.id)?.lastError)
    }

    @Test
    fun `agent completion updates progress and terminal state`() {
        val plane = plane()
        val request = plane.enqueue(request("agent"))
        assertNotNull(plane.acquireNext("worker"))
        val plan = AgentPlan(
            title = "Plan",
            objective = "Complete work",
            createdAt = clock,
            expiresAt = clock + 60_000,
            steps = listOf(AgentStep(order = 0, title = "Checkpoint", kind = AgentStepKind.CHECKPOINT))
        )
        val run = AgentRun(plan = plan, state = AgentRunState.COMPLETED, steps = plan.steps, createdAt = clock, updatedAt = clock)

        val updated = plane.updateFromAgent(request.id, "worker", run)

        assertEquals(ExecutionRequestState.COMPLETED, updated?.state)
        assertEquals(100, plane.events(request.id).first().progressPercent)
    }

    @Test
    fun `expired request cannot be dispatched`() {
        val plane = plane()
        val request = plane.enqueue(request("short", expiresAt = clock + 1_000))
        clock += 1_001

        assertNull(plane.acquireNext("worker"))
        assertEquals(ExecutionRequestState.EXPIRED, plane.get(request.id)?.state)
    }

    private fun plane(leaseMillis: Long = 10_000) = MayraExecutionControlPlane(
        now = { clock },
        leaseDurationMillis = leaseMillis,
        maxStoredRequests = 50,
        maxStoredEvents = 100
    )

    private fun request(
        title: String,
        priority: ExecutionPriority = ExecutionPriority.NORMAL,
        resources: Set<ExecutionResource> = emptySet(),
        conflictPolicy: ExecutionConflictPolicy = ExecutionConflictPolicy.WAIT,
        expiresAt: Long = clock + 60_000
    ) = ExecutionRequest(
        runId = "run-$title",
        title = title,
        priority = priority,
        resources = resources,
        conflictPolicy = conflictPolicy,
        createdAt = clock,
        notBefore = clock,
        expiresAt = expiresAt
    )
}
