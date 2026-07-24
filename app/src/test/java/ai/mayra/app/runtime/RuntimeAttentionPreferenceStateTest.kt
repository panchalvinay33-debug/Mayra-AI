package ai.mayra.app.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeAttentionPreferenceStateTest {
    @Test
    fun `enabled unsnoozed alerts can notify`() {
        val state = RuntimeAttentionPreferenceState(enabled = true, snoozedUntil = 0L)

        assertTrue(state.canNotify(now = 1_000L))
        assertEquals("Runtime alerts are on", state.status(now = 1_000L))
    }

    @Test
    fun `disabled alerts cannot notify after snooze expires`() {
        val state = RuntimeAttentionPreferenceState(enabled = false, snoozedUntil = 0L)

        assertFalse(state.canNotify(now = 10_000L))
        assertEquals("Runtime alerts are off", state.status(now = 10_000L))
    }

    @Test
    fun `snoozed alerts remain blocked until deadline`() {
        val state = RuntimeAttentionPreferenceState(enabled = true, snoozedUntil = 61_000L)

        assertFalse(state.canNotify(now = 1_000L))
        assertTrue(state.canNotify(now = 61_000L))
    }

    @Test
    fun `snooze status rounds remaining time up`() {
        val state = RuntimeAttentionPreferenceState(enabled = true, snoozedUntil = 61_001L)

        assertEquals("Runtime alerts snoozed for 2 min", state.status(now = 1_000L))
    }
}
