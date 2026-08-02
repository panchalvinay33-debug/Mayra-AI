package ai.mayra.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRecoveryPolicyTest {
    private val now = 1_000_000L

    @Test fun `future scheduled reminder is rescheduled`() {
        assertEquals(
            ReminderRecoveryAction.SCHEDULE,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.SCHEDULED, now + 60_000), now)
        )
    }

    @Test fun `overdue scheduled reminder becomes missed notification once`() {
        assertEquals(
            ReminderRecoveryAction.MARK_MISSED_AND_NOTIFY,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.SCHEDULED, now - 1), now)
        )
    }

    @Test fun `due reminder gets follow up without replaying original alert`() {
        assertEquals(
            ReminderRecoveryAction.SCHEDULE_FOLLOW_UP,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.DUE, now - 60_000), now)
        )
    }

    @Test fun `existing missed reminder is left alone on reboot`() {
        assertEquals(
            ReminderRecoveryAction.LEAVE_MISSED,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.MISSED, now - 60_000), now)
        )
    }

    @Test fun `finished reminders are ignored`() {
        assertEquals(
            ReminderRecoveryAction.IGNORE,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.COMPLETED, now - 60_000), now)
        )
        assertEquals(
            ReminderRecoveryAction.IGNORE,
            ReminderRecoveryPolicy.decide(reminder(ReminderState.CANCELLED, now - 60_000), now)
        )
    }

    private fun reminder(state: ReminderState, dueAt: Long) = MayraReminder(
        id = "r-${state.name}",
        title = "Test",
        dueAt = dueAt,
        createdAt = 1L,
        updatedAt = 1L,
        state = state
    )
}
