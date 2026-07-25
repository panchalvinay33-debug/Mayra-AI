package ai.mayra.app.reminder

enum class ReminderRecoveryAction {
    SCHEDULE,
    SCHEDULE_FOLLOW_UP,
    MARK_MISSED_AND_NOTIFY,
    LEAVE_MISSED,
    IGNORE
}

/** Pure reboot/update recovery policy. Existing missed reminders must not alert repeatedly. */
object ReminderRecoveryPolicy {
    fun decide(reminder: MayraReminder, now: Long): ReminderRecoveryAction = when {
        reminder.state in setOf(ReminderState.COMPLETED, ReminderState.CANCELLED) -> ReminderRecoveryAction.IGNORE
        reminder.state == ReminderState.DUE -> ReminderRecoveryAction.SCHEDULE_FOLLOW_UP
        reminder.state == ReminderState.MISSED -> ReminderRecoveryAction.LEAVE_MISSED
        reminder.dueAt <= now && reminder.state in setOf(ReminderState.SCHEDULED, ReminderState.SNOOZED) ->
            ReminderRecoveryAction.MARK_MISSED_AND_NOTIFY
        reminder.state in setOf(ReminderState.SCHEDULED, ReminderState.SNOOZED) -> ReminderRecoveryAction.SCHEDULE
        else -> ReminderRecoveryAction.IGNORE
    }
}
