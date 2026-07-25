package ai.mayra.app.reminder

import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderRecoveryPolicyTest {
    private val now = 10_000L

    @Test
    fun `future scheduled reminder is scheduled`() {
        assertEquals(
            ReminderRecoveryAction.SCHEDULE,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.SCHEDULED, now + 1_000L), now)
        )
    }

    @Test
    fun `overdue scheduled reminder is marked missed once`() {
        assertEquals(
            ReminderRecoveryAction.MARK_MISSED_AND_NOTIFY,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.SCHEDULED, now - 1L), now)
        )
    }

    @Test
    fun `already missed reminder remains quiet on reboot`() {
        assertEquals(
            ReminderRecoveryAction.LEAVE_MISSED,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.MISSED, now - 60_000L), now)
        )
    }

    @Test
    fun `due reminder restores only its follow up`() {
        assertEquals(
            ReminderRecoveryAction.SCHEDULE_FOLLOW_UP,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.DUE, now - 60_000L), now)
        )
    }

    @Test
    fun `terminal reminders are ignored`() {
        assertEquals(
            ReminderRecoveryAction.IGNORE,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.COMPLETED, now - 1L), now)
        )
        assertEquals(
            ReminderRecoveryAction.IGNORE,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.CANCELLED, now - 1L), now)
        )
    }

    private fun reminder(state: ReminderState, dueAt: Long) = MayraReminder(
        id = "recovery-${state.name}",
        title = "Recovery test",
        dueAt = dueAt,
        createdAt = 1_000L,
        updatedAt = 1_000L,
        state = state
    )
}
