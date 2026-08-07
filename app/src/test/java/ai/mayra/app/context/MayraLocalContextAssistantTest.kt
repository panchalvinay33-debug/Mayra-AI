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

    private fun bundle() = MayraContextBundle(
        capturedAt = now,
        device = MayraContextSnapshot(
            capturedAt = now,
            dayPart = DayPart.MORNING,
            connectivity = ContextValue.Available(ConnectivityState.ONLINE, ContextSource.CONNECTIVITY_MANAGER),
            power = ContextValue.Available(PowerState(isCharging = false, batteryPercent = 72), ContextSource.BATTERY_MANAGER)
        ),
        calendar = CalendarContextSnapshot(
            now,
            ContextValue.Available(
                CalendarAggregate(remainingEventsToday = 2, busyNow = false, minutesUntilNextEvent = 45),
                ContextSource.CALENDAR_PROVIDER
            )
        ),
        reminders = ReminderContextSnapshot(
            now,
            ContextValue.Available(
                ReminderAggregate(activeCount = 3, dueOrOverdueCount = 1, minutesUntilNextReminder = 20),
                ContextSource.REMINDERS
            )
        ),
        notifications = NotificationContextSnapshot(
            now,
            ContextValue.Available(
                NotificationAggregate(activeCount = 6, attentionCount = 2, categoryCounts = emptyMap()),
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
                DocumentAggregate(savedCount = 3, currentIndexedCount = 2, needsAttentionCount = 1),
                ContextSource.DOCUMENT_LIBRARY
            )
        )
    )
}
