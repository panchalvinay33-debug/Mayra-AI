package ai.mayra.app.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class RuntimeControlViewModelTest {
    @Test
    fun `snapshot failure is exposed as degraded state`() {
        val viewModel = RuntimeControlViewModel(
            snapshotProvider = { error("snapshot unavailable") }
        )

        val state = viewModel.uiState.value
        assertEquals(RuntimeHealth.DEGRADED, state.health)
        assertEquals("snapshot unavailable", state.error)
        assertFalse(state.isBusy)
        assertNull(state.busyLabel)
    }

    @Test
    fun `invalid approval clears busy state and shows message`() {
        val viewModel = RuntimeControlViewModel(
            snapshotProvider = { error("snapshot unavailable") },
            approveAction = { RuntimeControlResult.InvalidState("Action already resolved.") }
        )

        viewModel.approve("action-1")

        val state = viewModel.uiState.value
        assertEquals("Action already resolved.", state.error)
        assertFalse(state.isBusy)
        assertNull(state.busyLabel)
    }

    @Test
    fun `thrown cancel action clears busy state`() {
        val viewModel = RuntimeControlViewModel(
            snapshotProvider = { error("snapshot unavailable") },
            cancelPlanAction = { error("cancel failed") }
        )

        viewModel.cancelPlan("plan-1")

        val state = viewModel.uiState.value
        assertEquals("cancel failed", state.error)
        assertFalse(state.isBusy)
        assertNull(state.busyLabel)
    }

    @Test
    fun `history cleanup feedback handles zero one and many records`() {
        assertEquals("No completed workflow history to clear.", workflowHistoryCleanupMessage(0))
        assertEquals("Cleared 1 completed workflow.", workflowHistoryCleanupMessage(1))
        assertEquals("Cleared 4 completed workflows.", workflowHistoryCleanupMessage(4))
    }

    @Test
    fun `history cleanup failure clears busy state`() {
        val viewModel = RuntimeControlViewModel(
            snapshotProvider = { error("snapshot unavailable") },
            clearHistoryAction = { error("cleanup failed") }
        )

        viewModel.clearCompletedHistory()

        val state = viewModel.uiState.value
        assertEquals("cleanup failed", state.error)
        assertFalse(state.isBusy)
        assertNull(state.busyLabel)
    }

    @Test
    fun `successful cleanup notice survives snapshot refresh failure`() {
        val viewModel = RuntimeControlViewModel(
            snapshotProvider = { error("snapshot unavailable") },
            clearHistoryAction = { 3 }
        )

        viewModel.clearCompletedHistory()

        val state = viewModel.uiState.value
        assertEquals("Cleared 3 completed workflows.", state.notice)
        assertFalse(state.isBusy)
        assertNull(state.busyLabel)
    }
}
