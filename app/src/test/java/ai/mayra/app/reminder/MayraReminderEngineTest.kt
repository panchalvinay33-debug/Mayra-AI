package ai.mayra.app.reminder

import ai.mayra.app.TestMayraApplication
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class MayraReminderEngineTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val now = Instant.parse("2026-07-24T05:30:00Z") // 11:00 AM IST
    private val parser = MayraReminderParser(Clock.fixed(now, zone), zone)

    @Test
    fun `relative minutes are parsed exactly`() {
        val result = assertIs<ReminderParseResult.Parsed>(parser.parse("Medicine in 20 minutes"))

        assertEquals("Medicine", result.title)
        assertEquals(now.plusSeconds(20 * 60).toEpochMilli(), result.dueAt)
    }

    @Test
    fun `tomorrow evening time is parsed`() {
        val result = assertIs<ReminderParseResult.Parsed>(parser.parse("Call doctor tomorrow at 7 PM"))
        val due = Instant.ofEpochMilli(result.dueAt).atZone(zone)

        assertEquals(25, due.dayOfMonth)
        assertEquals(19, due.hour)
        assertTrue(result.title.contains("Call doctor"))
    }

    @Test
    fun `hinglish day and time are parsed`() {
        val result = assertIs<ReminderParseResult.Parsed>(parser.parse("Dawai kal subah 7 baje"))
        val due = Instant.ofEpochMilli(result.dueAt).atZone(zone)

        assertEquals(25, due.dayOfMonth)
        assertEquals(7, due.hour)
    }

    @Test
    fun `unrelated quantity is not treated as clock time`() {
        val result = parser.parse("Take 2 tablets tomorrow")

        assertIs<ReminderParseResult.NeedsClarification>(result)
    }

    @Test
    fun `missing time requests clarification`() {
        val result = parser.parse("Pay electricity bill tomorrow")

        assertIs<ReminderParseResult.NeedsClarification>(result)
    }

    @Test
    fun `past clock time moves to next day`() {
        val result = assertIs<ReminderParseResult.Parsed>(parser.parse("Drink water at 9 AM"))
        val due = Instant.ofEpochMilli(result.dueAt).atZone(zone)

        assertEquals(25, due.dayOfMonth)
        assertEquals(9, due.hour)
    }

    @Test
    fun `store persists and lifecycle transitions remain consistent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("mayra_owned_reminders", Context.MODE_PRIVATE).edit().clear().commit()
        val store = MayraReminderStore(context)
        val reminder = MayraReminder(
            id = "medicine-test",
            title = "Medicine",
            dueAt = now.plusSeconds(600).toEpochMilli(),
            createdAt = now.toEpochMilli()
        )
        store.upsert(reminder)

        assertEquals(reminder, store.find(reminder.id))
        val snoozed = assertNotNull(store.snooze(reminder.id, Duration.ofMinutes(10), now.toEpochMilli()))
        assertEquals(ReminderState.SNOOZED, snoozed.state)
        assertEquals(now.plusSeconds(600).toEpochMilli(), snoozed.dueAt)
        val notified = assertNotNull(store.markNotified(reminder.id, now.plusSeconds(600).toEpochMilli()))
        assertEquals(ReminderState.DUE, notified.state)
        assertEquals(1, notified.notificationCount)
        val completed = assertNotNull(store.complete(reminder.id, now.plusSeconds(700).toEpochMilli()))
        assertEquals(ReminderState.COMPLETED, completed.state)
        assertTrue(store.active().none { it.id == reminder.id })
        assertTrue(store.delete(reminder.id))
        assertFalse(store.delete(reminder.id))
    }
}
