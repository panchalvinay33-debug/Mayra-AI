package ai.mayra.app.context

import ai.mayra.app.MayraEntryContract
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProactiveContextCardsTest {
    private val now = LocalDateTime.of(2026, 8, 6, 14, 30)

    @Test
    fun ranksUrgentDeterministicSignalsAndBoundsOutput() {
        val cards = rankProactiveContextCards(
            bundle(
                reminder = ReminderAggregate(4, 2, 15),
                calendar = CalendarAggregate(2, true, 30),
                notification = NotificationAggregate(5, 1, emptyMap()),
                power = PowerState(isCharging = false, batteryPercent = 12),
                connectivity = ConnectivityState.OFFLINE,
                session = SessionAggregate(MayraEntryContract.Source.VOICE_SESSION, 3)
            ),
            maxCards = 3
        )

        assertEquals(
            listOf(
                ProactiveCardKind.REMINDER_DUE,
                ProactiveCardKind.BUSY_NOW,
                ProactiveCardKind.NOTIFICATION_ATTENTION
            ),
            cards.map(ProactiveContextCard::kind)
        )
        assertTrue(cards.zipWithNext().all { (a, b) -> a.priority >= b.priority })
    }

    @Test
    fun reminderSoonAndCalendarSoonAreCoarseAndContainNoPrivateTextFields() {
        val cards = rankProactiveContextCards(
            bundle(
                reminder = ReminderAggregate(1, 0, 20),
                calendar = CalendarAggregate(1, false, 10)
            )
        )
        val joined = cards.joinToString("\n") { "${it.title} ${it.detail}" }

        assertEquals(ProactiveCardKind.CALENDAR_SOON, cards.first().kind)
        assertTrue(cards.any { it.kind == ProactiveCardKind.REMINDER_SOON })
        assertFalse(joined.contains("title=", ignoreCase = true))
        assertFalse(joined.contains("location", ignoreCase = true))
        assertFalse(joined.contains("sender", ignoreCase = true))
        assertFalse(joined.contains("phone", ignoreCase = true))
    }

    @Test
    fun staleSessionDoesNotCreateCard() {
        val cards = rankProactiveContextCards(
            bundle(session = SessionAggregate(MayraEntryContract.Source.VOICE_SESSION, 16))
        )
        assertFalse(cards.any { it.kind == ProactiveCardKind.RECENT_VOICE_SESSION })
    }

    @Test
    fun staleNotificationAggregateDoesNotDriveAttentionCard() {
        val cards = rankProactiveContextCards(
            bundle(
                notification = NotificationAggregate(5, 3, emptyMap()),
                notificationCapturedAt = now.minusMinutes(121)
            )
        )
        assertFalse(cards.any { it.kind == ProactiveCardKind.NOTIFICATION_ATTENTION })
    }

    @Test
    fun futureDatedSnapshotIsNeverFresh() {
        assertFalse(isFreshSnapshot(now.plusMinutes(1), now, 120))
        assertTrue(isFreshSnapshot(now.minusMinutes(120), now, 120))
    }

    @Test
    fun noSignalsProducesNoProactiveCard() {
        val cards = rankProactiveContextCards(bundle())
        assertTrue(cards.isEmpty())
    }

    private fun bundle(
        reminder: ReminderAggregate? = null,
        calendar: CalendarAggregate? = null,
        notification: NotificationAggregate? = null,
        notificationCapturedAt: LocalDateTime = now,
        power: PowerState? = null,
        connectivity: ConnectivityState? = null,
        session: SessionAggregate? = null
    ) = MayraContextBundle(
        capturedAt = now,
        device = MayraContextSnapshot(
            capturedAt = now,
            dayPart = DayPart.AFTERNOON,
            connectivity = connectivity?.let { ContextValue.Available(it, ContextSource.CONNECTIVITY_MANAGER) }
                ?: ContextValue.Unavailable,
            power = power?.let { ContextValue.Available(it, ContextSource.BATTERY_MANAGER) }
                ?: ContextValue.Unavailable
        ),
        calendar = CalendarContextSnapshot(
            now,
            calendar?.let { ContextValue.Available(it, ContextSource.CALENDAR_PROVIDER) }
                ?: ContextValue.Unavailable
        ),
        reminders = ReminderContextSnapshot(
            now,
            reminder?.let { ContextValue.Available(it, ContextSource.REMINDERS) }
                ?: ContextValue.Unavailable
        ),
        notifications = NotificationContextSnapshot(
            notificationCapturedAt,
            notification?.let { ContextValue.Available(it, ContextSource.NOTIFICATION_ACCESS) }
                ?: ContextValue.Unavailable
        ),
        contacts = ContactsContextSnapshot(now, ContextValue.Unavailable),
        session = SessionContextSnapshot(
            now,
            session?.let { ContextValue.Available(it, ContextSource.SESSION) }
                ?: ContextValue.Unavailable
        )
    )
}
