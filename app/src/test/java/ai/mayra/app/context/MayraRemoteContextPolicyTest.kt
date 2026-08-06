package ai.mayra.app.context

import ai.mayra.app.MayraEntryContract
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRemoteContextPolicyTest {
    private val now = LocalDateTime.of(2026, 8, 6, 14, 30)

    @Test
    fun remotePolicyIncludesOnlyLowRiskDeviceAndAgendaFacts() {
        val bundle = MayraContextBundle(
            capturedAt = now,
            device = MayraContextSnapshot(
                capturedAt = now,
                dayPart = DayPart.AFTERNOON,
                connectivity = ContextValue.Available(ConnectivityState.ONLINE, ContextSource.CONNECTIVITY_MANAGER),
                power = ContextValue.Available(PowerState(isCharging = true, batteryPercent = 77), ContextSource.BATTERY_MANAGER)
            ),
            calendar = CalendarContextSnapshot(
                capturedAt = now,
                access = ContextValue.Available(
                    CalendarAggregate(remainingEventsToday = 2, busyNow = false, minutesUntilNextEvent = 25),
                    ContextSource.CALENDAR_PROVIDER
                )
            ),
            reminders = ReminderContextSnapshot(
                capturedAt = now,
                access = ContextValue.Available(
                    ReminderAggregate(activeCount = 3, dueOrOverdueCount = 1, minutesUntilNextReminder = 10),
                    ContextSource.REMINDERS
                )
            ),
            notifications = NotificationContextSnapshot(
                capturedAt = now,
                access = ContextValue.Available(
                    NotificationAggregate(activeCount = 99, attentionCount = 88, categoryCounts = emptyMap()),
                    ContextSource.NOTIFICATION_ACCESS
                )
            ),
            contacts = ContactsContextSnapshot(
                capturedAt = now,
                access = ContextValue.Available(
                    ContactsAggregate(totalContacts = 456, phoneCapableContacts = 400),
                    ContextSource.CONTACTS
                )
            ),
            session = SessionContextSnapshot(
                capturedAt = now,
                access = ContextValue.Available(
                    SessionAggregate(MayraEntryContract.Source.VOICE_SESSION, 2),
                    ContextSource.SESSION
                )
            )
        )

        val lines = MayraRemoteContextPolicy.lines(bundle)
        val joined = lines.joinToString("\n")

        assertTrue("day_part=afternoon" in lines)
        assertTrue("connectivity=online" in lines)
        assertTrue("battery_percent=77" in lines)
        assertTrue("calendar_next_minutes=25" in lines)
        assertTrue("reminders_due_or_overdue=1" in lines)
        assertFalse(joined.contains("notification", ignoreCase = true))
        assertFalse(joined.contains("contact", ignoreCase = true))
        assertFalse(joined.contains("session", ignoreCase = true))
        assertFalse(joined.contains("voice", ignoreCase = true))
        assertFalse(joined.contains("99"))
        assertFalse(joined.contains("456"))
        assertTrue(lines.size <= 12)
    }

    @Test
    fun unavailableOrNotGrantedSourcesDoNotLeakPermissionStateToRemote() {
        val bundle = MayraContextBundle(
            capturedAt = now,
            device = MayraContextSnapshot(
                capturedAt = now,
                dayPart = DayPart.AFTERNOON,
                connectivity = ContextValue.NotGranted,
                power = ContextValue.Unavailable
            ),
            calendar = CalendarContextSnapshot(now, ContextValue.NotGranted),
            reminders = ReminderContextSnapshot(now, ContextValue.Unavailable),
            notifications = NotificationContextSnapshot(now, ContextValue.NotGranted),
            contacts = ContactsContextSnapshot(now, ContextValue.NotGranted),
            session = SessionContextSnapshot(now, ContextValue.Unavailable)
        )

        val lines = MayraRemoteContextPolicy.lines(bundle)

        assertTrue(lines == listOf("day_part=afternoon"))
    }
}
