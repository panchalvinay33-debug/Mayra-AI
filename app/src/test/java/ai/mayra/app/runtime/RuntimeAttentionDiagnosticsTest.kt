package ai.mayra.app.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeAttentionDiagnosticsTest {
    @Test
    fun `missing completion time reports scan has not run`() {
        val state = RuntimeAttentionScanState(
            completedAt = 0L,
            outcome = RuntimeAttentionScanOutcome.NO_NEW_ALERT
        )

        assertEquals("Background scan has not run yet", state.status(now = 10_000L))
    }

    @Test
    fun `recent posted alert is described clearly`() {
        val state = RuntimeAttentionScanState(
            completedAt = 1_000_000L,
            outcome = RuntimeAttentionScanOutcome.ALERT_POSTED
        )

        assertEquals("Last background scan: just now · alert posted", state.status(now = 1_020_000L))
    }

    @Test
    fun `one minute age uses singular label`() {
        val state = RuntimeAttentionScanState(
            completedAt = 1_000_000L,
            outcome = RuntimeAttentionScanOutcome.NO_NEW_ALERT
        )

        assertEquals("Last background scan: 1 min ago · no new alert", state.status(now = 1_060_000L))
    }

    @Test
    fun `runtime unavailable and failures remain distinguishable`() {
        val unavailable = RuntimeAttentionScanState(
            completedAt = 1_000_000L,
            outcome = RuntimeAttentionScanOutcome.RUNTIME_UNAVAILABLE
        )
        val failed = RuntimeAttentionScanState(
            completedAt = 1_000_000L,
            outcome = RuntimeAttentionScanOutcome.SNAPSHOT_FAILED
        )

        assertEquals("Last background scan: 2 min ago · runtime unavailable", unavailable.status(now = 1_120_000L))
        assertEquals("Last background scan: 2 min ago · scan failed", failed.status(now = 1_120_000L))
    }
}
