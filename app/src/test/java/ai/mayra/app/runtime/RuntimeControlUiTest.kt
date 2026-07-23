package ai.mayra.app.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeControlUiTest {
    @Test
    fun `healthy runtime has no failures or waiting work`() {
        assertEquals(
            RuntimeHealth.HEALTHY,
            classifyRuntimeHealth(
                failedCount = 0,
                pendingActionCount = 0,
                blockedPlanCount = 0,
                waitingConfirmationSteps = 0
            )
        )
    }

    @Test
    fun `waiting approval requires attention`() {
        assertEquals(
            RuntimeHealth.ATTENTION,
            classifyRuntimeHealth(
                failedCount = 0,
                pendingActionCount = 1,
                blockedPlanCount = 0,
                waitingConfirmationSteps = 0
            )
        )
    }

    @Test
    fun `blocked workflow requires attention`() {
        assertEquals(
            RuntimeHealth.ATTENTION,
            classifyRuntimeHealth(
                failedCount = 0,
                pendingActionCount = 0,
                blockedPlanCount = 1,
                waitingConfirmationSteps = 1
            )
        )
    }

    @Test
    fun `failure takes priority over waiting work`() {
        assertEquals(
            RuntimeHealth.DEGRADED,
            classifyRuntimeHealth(
                failedCount = 1,
                pendingActionCount = 2,
                blockedPlanCount = 3,
                waitingConfirmationSteps = 4
            )
        )
    }

    @Test
    fun `freshness waits for first snapshot`() {
        assertEquals("Waiting for first snapshot", runtimeSnapshotFreshness(capturedAt = 0L, now = 10_000L))
    }

    @Test
    fun `freshness reports recent snapshot`() {
        assertEquals("Updated just now", runtimeSnapshotFreshness(capturedAt = 9_000L, now = 10_000L))
        assertEquals("Updated 12s ago", runtimeSnapshotFreshness(capturedAt = 8_000L, now = 20_000L))
    }

    @Test
    fun `freshness warns when snapshot is old`() {
        assertEquals(
            "Snapshot may be stale · updated 2m ago",
            runtimeSnapshotFreshness(capturedAt = 1_000L, now = 121_000L)
        )
    }

    @Test
    fun `workflow progress reports partial completion`() {
        assertEquals(
            50 to "2/4 completed · 1 failed · 1 waiting",
            workflowProgress(totalSteps = 4, completedSteps = 2, failedSteps = 1, waitingSteps = 1)
        )
    }

    @Test
    fun `workflow progress handles empty plan`() {
        assertEquals(
            0 to "0/0 completed · 0 failed · 0 waiting",
            workflowProgress(totalSteps = 0, completedSteps = 0, failedSteps = 0, waitingSteps = 0)
        )
    }

    @Test
    fun `workflow progress clamps invalid completed count`() {
        assertEquals(
            100 to "5/2 completed · 0 failed · 0 waiting",
            workflowProgress(totalSteps = 2, completedSteps = 5, failedSteps = 0, waitingSteps = 0)
        )
    }
}
