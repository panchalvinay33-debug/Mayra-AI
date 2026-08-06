package ai.mayra.app.context

/**
 * Explicit allow-list for context that may accompany a remote conversational request.
 *
 * Contacts and notifications are intentionally excluded even though their J6 snapshots are already
 * aggregate-only. This keeps the remote boundary narrower than the local Context Fabric.
 */
object MayraRemoteContextPolicy {
    fun lines(bundle: MayraContextBundle): List<String> = buildList {
        add("day_part=${bundle.device.dayPart.name.lowercase()}")

        when (val connectivity = bundle.device.connectivity) {
            is ContextValue.Available -> add("connectivity=${connectivity.value.name.lowercase()}")
            ContextValue.NotGranted, ContextValue.Unavailable -> Unit
        }

        when (val power = bundle.device.power) {
            is ContextValue.Available -> {
                add("charging=${power.value.isCharging}")
                power.value.batteryPercent?.let { add("battery_percent=$it") }
            }
            ContextValue.NotGranted, ContextValue.Unavailable -> Unit
        }

        when (val calendar = bundle.calendar.access) {
            is ContextValue.Available -> {
                add("calendar_remaining_today=${calendar.value.remainingEventsToday}")
                add("calendar_busy_now=${calendar.value.busyNow}")
                calendar.value.minutesUntilNextEvent?.let { add("calendar_next_minutes=$it") }
            }
            ContextValue.NotGranted, ContextValue.Unavailable -> Unit
        }

        when (val reminders = bundle.reminders.access) {
            is ContextValue.Available -> {
                add("reminders_active=${reminders.value.activeCount}")
                add("reminders_due_or_overdue=${reminders.value.dueOrOverdueCount}")
                reminders.value.minutesUntilNextReminder?.let { add("reminder_next_minutes=$it") }
            }
            ContextValue.NotGranted, ContextValue.Unavailable -> Unit
        }
    }.take(MAX_REMOTE_CONTEXT_LINES)

    private const val MAX_REMOTE_CONTEXT_LINES = 12
}
