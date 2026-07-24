package ai.mayra.app.calendar

import ai.mayra.app.TestMayraApplication
import ai.mayra.app.reminder.MayraReminder
import ai.mayra.app.reminder.MayraReminderStore
import ai.mayra.app.reminder.ReminderState
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class MayraAgendaEngineTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val zone = ZoneId.systemDefault()

    @Test
    fun `today summary combines events and reminders`() {
        clearStores()
        MayraAgendaRuntime.install(context)
        val now = LocalDate.now().atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val event = MayraAgendaEventParser(zone).create(
            title = "Team meeting",
            date = LocalDate.now(),
            time = LocalTime.of(11, 0),
            now = now
        )
        MayraAgendaStore(context).upsert(event)
        MayraReminderStore(context).upsert(
            MayraReminder(
                id = "medicine-today",
                title = "Medicine",
                dueAt = LocalDate.now().atTime(20, 0).atZone(zone).toInstant().toEpochMilli(),
                createdAt = now
            )
        )

        val summary = MayraAgendaRuntime.todaySummary(now, zone)

        assertTrue(summary.contains("1 events and 1 reminders"))
        assertTrue(summary.contains("Team meeting"))
        assertTrue(summary.contains("Medicine"))
    }

    @Test
    fun `complete reminder uses exact unique match`() {
        clearStores()
        MayraAgendaRuntime.install(context)
        val now = System.currentTimeMillis()
        MayraReminderStore(context).upsert(
            MayraReminder(id = "walk", title = "Morning walk", dueAt = now + 60_000, createdAt = now)
        )

        val response = MayraAgendaRuntime.completeReminder("morning walk", now)

        assertTrue(response.contains("Completed reminder"))
        assertEquals(ReminderState.COMPLETED, MayraReminderStore(context).find("walk")?.state)
    }

    @Test
    fun `ambiguous reminder match never chooses`() {
        clearStores()
        MayraAgendaRuntime.install(context)
        val now = System.currentTimeMillis()
        val store = MayraReminderStore(context)
        store.upsert(MayraReminder(id = "med-1", title = "Morning medicine", dueAt = now + 60_000, createdAt = now))
        store.upsert(MayraReminder(id = "med-2", title = "Night medicine", dueAt = now + 120_000, createdAt = now))

        val response = MayraAgendaRuntime.completeReminder("medicine", now)

        assertTrue(response.contains("multiple matching reminders"))
        assertEquals(ReminderState.SCHEDULED, store.find("med-1")?.state)
        assertEquals(ReminderState.SCHEDULED, store.find("med-2")?.state)
    }

    @Test
    fun `snooze and cancel update reminder lifecycle`() {
        clearStores()
        MayraAgendaRuntime.install(context)
        val now = System.currentTimeMillis()
        val store = MayraReminderStore(context)
        store.upsert(MayraReminder(id = "bill", title = "Electricity bill", dueAt = now, createdAt = now))

        val snoozed = MayraAgendaRuntime.snoozeReminder("electricity bill", 30, now)
        assertTrue(snoozed.contains("30 minutes"))
        assertEquals(ReminderState.SNOOZED, store.find("bill")?.state)

        val cancelled = MayraAgendaRuntime.cancelReminder("electricity bill", now + 1_000)
        assertTrue(cancelled.contains("Cancelled reminder"))
        assertEquals(ReminderState.CANCELLED, store.find("bill")?.state)
    }

    @Test
    fun `agenda event persists recurrence and can move`() {
        clearStores()
        val store = MayraAgendaStore(context)
        val event = MayraAgendaEventParser(zone).create(
            title = "Weekly review",
            date = LocalDate.now().plusDays(1),
            time = LocalTime.of(18, 0),
            recurrence = AgendaRecurrence.WEEKLY,
            now = 100L
        )
        store.upsert(event)

        val loaded = store.all().single()
        assertEquals(AgendaRecurrence.WEEKLY, loaded.recurrence)
        val moved = store.move(loaded.id, loaded.startsAt + 3_600_000, loaded.endsAt + 3_600_000, 200L)
        assertNotNull(moved)
        assertEquals(200L, moved.updatedAt)
    }

    private fun clearStores() {
        context.getSharedPreferences("mayra_owned_agenda", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_owned_reminders", Context.MODE_PRIVATE).edit().clear().commit()
    }
}