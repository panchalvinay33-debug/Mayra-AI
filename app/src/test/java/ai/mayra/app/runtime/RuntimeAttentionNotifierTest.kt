package ai.mayra.app.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RuntimeAttentionNotifierTest {
    @Test
    fun `healthy runtime produces no alert`() {
        assertNull(
            buildRuntimeAttentionAlert(
                runtimeFailures = 0,
                planFailures = 0,
                pendingActions = emptyList(),
                blockedPlans = 0,
                waitingConfirmationSteps = 0
            )
        )
    }

    @Test
    fun `runtime failure has highest priority`() {
        val alert = buildRuntimeAttentionAlert(
            runtimeFailures = 1,
            planFailures = 2,
            pendingActions = listOf("action-1" to "Call customer"),
            blockedPlans = 1,
            waitingConfirmationSteps = 1
        )

        assertEquals("Mayra runtime needs attention", alert?.title)
        assertEquals("3 runtime failures detected.", alert?.message)
        assertEquals("failure:3:1:2", alert?.fingerprint)
    }

    @Test
    fun `single pending approval uses action title`() {
        val alert = buildRuntimeAttentionAlert(
            runtimeFailures = 0,
            planFailures = 0,
            pendingActions = listOf("action-1" to "Send message to Mummy"),
            blockedPlans = 0,
            waitingConfirmationSteps = 0
        )

        assertEquals("Mayra is waiting for approval", alert?.title)
        assertEquals("Send message to Mummy", alert?.message)
        assertEquals("approval:action-1", alert?.fingerprint)
    }

    @Test
    fun `multiple approvals use stable sorted fingerprint`() {
        val alert = buildRuntimeAttentionAlert(
            runtimeFailures = 0,
            planFailures = 0,
            pendingActions = listOf(
                "action-b" to "Second",
                "action-a" to "First"
            ),
            blockedPlans = 0,
            waitingConfirmationSteps = 0
        )

        assertEquals("2 actions are waiting for your approval.", alert?.message)
        assertEquals("approval:action-a,action-b", alert?.fingerprint)
    }

    @Test
    fun `blocked workflow alert is used when no failures or approvals exist`() {
        val alert = buildRuntimeAttentionAlert(
            runtimeFailures = 0,
            planFailures = 0,
            pendingActions = emptyList(),
            blockedPlans = 1,
            waitingConfirmationSteps = 2
        )

        assertEquals("A Mayra workflow is paused", alert?.title)
        assertEquals("1 blocked workflow · 2 steps waiting.", alert?.message)
        assertEquals("workflow:1:2", alert?.fingerprint)
    }

    @Test
    fun `attention schedule enforces WorkManager minimum interval`() {
        assertEquals(15L, runtimeAttentionIntervalMinutes(1L))
        assertEquals(15L, runtimeAttentionIntervalMinutes(14L))
    }

    @Test
    fun `attention schedule preserves valid interval`() {
        assertEquals(30L, runtimeAttentionIntervalMinutes(30L))
    }

    @Test
    fun `enabled schedule reports normalized interval`() {
        assertEquals(
            "Background scans every 15 min",
            RuntimeAttentionScheduleState(enabled = true, intervalMinutes = 1L).status()
        )
    }

    @Test
    fun `disabled schedule reports off`() {
        assertEquals(
            "Background scans are off",
            RuntimeAttentionScheduleState(enabled = false, intervalMinutes = 15L).status()
        )
    }

    @Test
    fun `queued background scan has clear feedback`() {
        assertEquals("Background runtime scan queued.", backgroundScanQueuedMessage())
    }
}
