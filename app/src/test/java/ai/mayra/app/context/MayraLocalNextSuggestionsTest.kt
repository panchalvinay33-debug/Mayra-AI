package ai.mayra.app.context

import ai.mayra.app.MayraEntryContract
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraLocalNextSuggestionsTest {
    private val now = LocalDateTime.of(2026, 8, 8, 13, 0)

    @Test
    fun ranksUrgentSuggestionsAndBoundsOutput() {
        val answer = MayraLocalNextSuggestions.answer(
            "ab kya karu",
            bundle(
                dueReminders = 2,
                busyNow = true,
                notificationAttention = 3,
                batteryPercent = 12,
                offline = true,
                libraryNeedsAttention = 2
            )
        ).orEmpty()

        val bullets = answer.lineSequence().count { it.startsWith("• ") }
        assertTrue(answer.startsWith("Next suggestions:"))
        assertTrue(answer.contains("Review your due reminders first"))
        assertTrue(answer.contains("You are busy now"))
        assertTrue(answer.contains("Check 3 notifications"))
        assertFalse(answer.contains("Charge the phone"))
        assertFalse(answer.contains("Document Library"))
        assertTrue(bullets == 3)
    }

    @Test
    fun staleNotificationsNeverDriveSuggestions() {
        val answer = MayraLocalNextSuggestions.answer(
            "what next",
            bundle(
                notificationAttention = 4,
                notificationCapturedAt = now.minusMinutes(121),
                libraryNeedsAttention = 1
            )
        ).orEmpty()

        assertFalse(answer.contains("notification", ignoreCase = true))
        assertTrue(answer.contains("Refresh the Document Library"))
    }

    @Test
    fun calmFallbackWhenNoActionableSignalExists() {
        val answer = MayraLocalNextSuggestions.answer(
            "what should i do now",
            bundle(
                dueReminders = 0,
                reminderNextMinutes = 180,
                calendarRemaining = 0,
                calendarNextMinutes = null,
                notificationAttention = 0,
                batteryPercent = 80,
                offline = false,
                libraryNeedsAttention = 0
            )
        ).orEmpty()

        assertTrue(answer.contains("Nothing urgent", ignoreCase = true))
        assertFalse(answer.contains("• "))
    }

    @Test
    fun actionCommandsAreDelegatedInsteadOfIntercepted() {
        assertNull(MayraLocalNextSuggestions.answer("ab reminder set karo 5.20 ka", bundle()))
        assertNull(MayraLocalNextSuggestions.answer("what next open WhatsApp", bundle()))
        assertNull(MayraLocalNextSuggestions.answer("ab kya karu call mummy", bundle()))
    }

    @Test
    fun outputContainsNoRawPrivateContent() {
        val answer = MayraLocalNextSuggestions.answer("next kya", bundle()).orEmpty()
        listOf(
            "Secret Meeting",
            "OTP 123456",
            "Rahul",
            "+919999999999",
            "private-document-title.pdf",
            "secret-memory-value"
        ).forEach { forbidden ->
            assertFalse(answer.contains(forbidden, ignoreCase = true))
        }
    }

    private fun bundle(
        dueReminders: Int = 1,
        reminderNextMinutes: Int? = 20,
        calendarRemaining: Int = 1,
        busyNow: Boolean = false,
        calendarNextMinutes: Int? = 45,
        notificationAttention: Int = 1,
        notificationCapturedAt: LocalDateTime = now,
        batteryPercent: Int = 72,
        offline: Boolean = false,
        libraryNeedsAttention: Int = 1
    ) = MayraContextBundle(
        capturedAt = now,
        device = MayraContextSnapshot(
            capturedAt = now,
            dayPart = DayPart.AFTERNOON,
            connectivity = ContextValue.Available(
                if (offline) ConnectivityState.OFFLINE else ConnectivityState.ONLINE,
                ContextSource.CONNECTIVITY_MANAGER
            ),
            power = ContextValue.Available(
                PowerState(isCharging = false, batteryPercent = batteryPercent),
                ContextSource.BATTERY_MANAGER
            )
        ),
        calendar = CalendarContextSnapshot(
            now,
            ContextValue.Available(
                CalendarAggregate(calendarRemaining, busyNow, calendarNextMinutes),
                ContextSource.CALENDAR_PROVIDER
            )
        ),
        reminders = ReminderContextSnapshot(
            now,
            ContextValue.Available(
                ReminderAggregate(
                    activeCount = maxOf(dueReminders, if (reminderNextMinutes != null) 1 else 0),
                    dueOrOverdueCount = dueReminders,
                    minutesUntilNextReminder = reminderNextMinutes
                ),
                ContextSource.REMINDERS
            )
        ),
        notifications = NotificationContextSnapshot(
            notificationCapturedAt,
            ContextValue.Available(
                NotificationAggregate(
                    activeCount = maxOf(notificationAttention, 1),
                    attentionCount = notificationAttention,
                    categoryCounts = emptyMap()
                ),
                ContextSource.NOTIFICATION_ACCESS
            )
        ),
        contacts = ContactsContextSnapshot(now, ContextValue.Unavailable),
        session = SessionContextSnapshot(
            now,
            ContextValue.Available(
                SessionAggregate(MayraEntryContract.Source.LAUNCHER, 1),
                ContextSource.SESSION
            )
        ),
        knowledge = KnowledgeContextSnapshot(
            capturedAt = now,
            memory = ContextValue.Available(MemoryAggregate(2), ContextSource.MEMORY),
            documents = ContextValue.Available(
                DocumentAggregate(
                    savedCount = 3,
                    currentIndexedCount = 3 - libraryNeedsAttention,
                    needsAttentionCount = libraryNeedsAttention
                ),
                ContextSource.DOCUMENT_LIBRARY
            )
        )
    )
}
