package ai.mayra.app.context

import ai.mayra.app.MayraEntryContract
import java.time.LocalDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraLocalContextAssistantTest {
    private val now = LocalDateTime.of(2026, 8, 7, 10, 0)

    @Test
    fun explicitContextStatusIsAnsweredFromCoarseAggregates() {
        val answer = MayraLocalContextAnswers.answer("Mayra context status", bundle())

        requireNotNull(answer)
        assertTrue(answer.contains("Local context status"))
        assertTrue(answer.contains("Memory · 4 saved"))
        assertTrue(answer.contains("Library · 2/3 current · 1 need attention"))
        assertTrue(answer.contains("Notifications: 6 active, 2 may need attention"))
        assertTrue(answer.contains("People: 20 contacts, 15 with a phone number"))
    }

    @Test
    fun actionCommandsAreNeverInterceptedByStatusLayer() {
        assertNull(MayraLocalContextAnswers.answer("5.20 ka reminder set karo", bundle()))
        assertNull(MayraLocalContextAnswers.answer("set reminder in 20 minutes", bundle()))
        assertNull(MayraLocalContextAnswers.answer("open WhatsApp", bundle()))
        assertNull(MayraLocalContextAnswers.answer("call mummy", bundle()))
        assertNull(MayraLocalContextAnswers.answer("message Rahul hello", bundle()))
    }

    @Test
    fun libraryAndMemoryAnswersCannotContainRawPrivateContent() {
        val memory = MayraLocalContextAnswers.answer("memory status", bundle()).orEmpty()
        val library = MayraLocalContextAnswers.answer("library status", bundle()).orEmpty()
        val joined = "$memory\n$library"

        listOf(
            "secret-memory-value",
            "private-document-title.pdf",
            "private document body",
            "content://private-uri"
        ).forEach { forbidden ->
            assertFalse(joined.contains(forbidden, ignoreCase = true))
        }
        assertTrue(memory.contains("4 approved personal memories"))
        assertTrue(library.contains("3 saved, 2 current, 1 need indexing or refresh"))
    }

    @Test
    fun agendaNotificationAndPeopleAnswersRemainAggregateOnly() {
        val agenda = MayraLocalContextAnswers.answer("aaj kya due hai", bundle()).orEmpty()
        val notifications = MayraLocalContextAnswers.answer("notification status", bundle()).orEmpty()
        val people = MayraLocalContextAnswers.answer("contacts status", bundle()).orEmpty()
        val joined = "$agenda\n$notifications\n$people"

        assertTrue(agenda.contains("2 remaining today"))
        assertTrue(agenda.contains("1 due or overdue"))
        assertTrue(notifications.contains("6 active"))
        assertTrue(people.contains("20 contacts"))
        listOf("Secret Meeting", "OTP 123456", "Rahul", "+919999999999").forEach { forbidden ->
            assertFalse(joined.contains(forbidden, ignoreCase = true))
        }
    }

    @Test
    fun dailyBriefRanksUrgentSignalsAndIsBounded() {
        val answer = MayraLocalContextAnswers.answer(
            "aaj kya important hai",
            bundle(
                power = PowerState(isCharging = false, batteryPercent = 12),
                connectivity = ConnectivityState.OFFLINE,
                busyNow = true,
                calendarNextMinutes = 15,
                dueReminders = 2,
                notificationAttention = 3,
                libraryNeedsAttention = 2
            )
        ).orEmpty()

        val bullets = answer.lineSequence().count { it.startsWith("• ") }
        assertTrue(answer.startsWith("Daily brief:"))
        assertTrue(answer.contains("2 reminders are due or overdue"))
        assertTrue(answer.contains("Calendar shows you are busy now"))
        assertTrue(answer.contains("3 notifications may need attention"))
        assertTrue(answer.contains("Battery is low at 12%"))
        assertFalse(answer.contains("Device is offline"))
        assertFalse(answer.contains("Library has 2 documents"))
        assertTrue(bullets == 4)
    }

    @Test
    fun staleNotificationAttentionIsExcludedFromDailyBrief() {
        val answer = MayraLocalContextAnswers.answer(
            "daily brief",
            bundle(
                notificationAttention = 4,
                notificationCapturedAt = now.minusMinutes(121),
                dueReminders = 0,
                calendarRemaining = 0,
                calendarNextMinutes = null,
                libraryNeedsAttention = 1
            )
        ).orEmpty()

        assertFalse(answer.contains("notification", ignoreCase = true))
        assertTrue(answer.contains("Library has 1 document needing indexing or refresh"))
    }

    @Test
    fun dailyBriefHasCalmFallbackWhenNothingIsUrgent() {
        val answer = MayraLocalContextAnswers.answer(
            "what should I know",
            bundle(
                dueReminders = 0,
                reminderNextMinutes = 180,
                calendarRemaining = 0,
                calendarNextMinutes = null,
                notificationAttention = 0,
                power = PowerState(isCharging = false, batteryPercent = 80),
                connectivity = ConnectivityState.ONLINE,
                libraryNeedsAttention = 0
            )
        ).orEmpty()

        assertTrue(answer.contains("nothing urgent", ignoreCase = true))
        assertFalse(answer.contains("• "))
    }

    private fun bundle(
        power: PowerState = PowerState(isCharging = false, batteryPercent = 72),
        connectivity: ConnectivityState = ConnectivityState.ONLINE,
        calendarRemaining: Int = 2,
        busyNow: Boolean = false,
        calendarNextMinutes: Long? = 45,
        dueReminders: Int = 1,
        reminderNextMinutes: Long? = 20,
        notificationAttention: Int = 2,
        notificationCapturedAt: LocalDateTime = now,
        libraryNeedsAttention: Int = 1
    ) = MayraContextBundle(
        capturedAt = now,
        device = MayraContextSnapshot(
            capturedAt = now,
            dayPart = DayPart.MORNING,
            connectivity = ContextValue.Available(connectivity, ContextSource.CONNECTIVITY_MANAGER),
            power = ContextValue.Available(power, ContextSource.BATTERY_MANAGER)
        ),
        calendar = CalendarContextSnapshot(
            now,
            ContextValue.Available(
                CalendarAggregate(
                    remainingEventsToday = calendarRemaining,
                    busyNow = busyNow,
                    minutesUntilNextEvent = calendarNextMinutes
                ),
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
                    activeCount = maxOf(6, notificationAttention),
                    attentionCount = notificationAttention,
                    categoryCounts = emptyMap()
                ),
                ContextSource.NOTIFICATION_ACCESS
            )
        ),
        contacts = ContactsContextSnapshot(
            now,
            ContextValue.Available(
                ContactsAggregate(totalContacts = 20, phoneCapableContacts = 15),
                ContextSource.CONTACTS
            )
        ),
        session = SessionContextSnapshot(
            now,
            ContextValue.Available(
                SessionAggregate(MayraEntryContract.Source.LAUNCHER, 1),
                ContextSource.SESSION
            )
        ),
        knowledge = KnowledgeContextSnapshot(
            capturedAt = now,
            memory = ContextValue.Available(MemoryAggregate(4), ContextSource.MEMORY),
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
