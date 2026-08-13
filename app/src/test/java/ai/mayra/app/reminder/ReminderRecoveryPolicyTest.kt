package ai.mayra.app.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRecoveryPolicyTest {
    private val now = 10_000_000L

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

    @Test fun `follow up recovery uses only the remaining thirty minute window`() {
        val lastAlert = now - 25L * 60L * 1_000L
        val due = reminder(ReminderState.DUE, now - 30L * 60L * 1_000L).copy(lastNotifiedAt = lastAlert)

        assertEquals(5L * 60L * 1_000L, MayraReminderRuntime.followUpDelayMillis(due, now))
    }

    @Test fun `already overdue follow up is scheduled immediately`() {
        val lastAlert = now - 35L * 60L * 1_000L
        val due = reminder(ReminderState.DUE, now - 40L * 60L * 1_000L).copy(lastNotifiedAt = lastAlert)

        assertEquals(0L, MayraReminderRuntime.followUpDelayMillis(due, now))
    }

    @Test fun `fresh due reminder receives full follow up window`() {
        val due = reminder(ReminderState.DUE, now).copy(lastNotifiedAt = now)

        assertEquals(MayraReminderRuntime.FOLLOW_UP_DELAY_MILLIS, MayraReminderRuntime.followUpDelayMillis(due, now))
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
