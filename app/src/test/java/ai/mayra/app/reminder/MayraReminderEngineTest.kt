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
import kotlin.test.assertNull
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
        assertIs<ReminderParseResult.NeedsClarification>(parser.parse("Take 2 tablets tomorrow"))
    }

    @Test
    fun `missing time requests clarification`() {
        assertIs<ReminderParseResult.NeedsClarification>(parser.parse("Pay electricity bill tomorrow"))
    }

    @Test
    fun `past clock time moves to next day`() {
        val result = assertIs<ReminderParseResult.Parsed>(parser.parse("Drink water at 9 AM"))
        val due = Instant.ofEpochMilli(result.dueAt).atZone(zone)
        assertEquals(25, due.dayOfMonth)
        assertEquals(9, due.hour)
    }

    @Test
    fun `store persists and lifecycle transitions increment revision`() {
        val store = cleanStore()
        val reminder = reminder()
        store.upsert(reminder)

        assertEquals(reminder, store.find(reminder.id))
        val snoozed = assertNotNull(store.snooze(reminder.id, Duration.ofMinutes(10), now.toEpochMilli()))
        assertEquals(ReminderState.SNOOZED, snoozed.state)
        assertEquals(now.plusSeconds(600).toEpochMilli(), snoozed.dueAt)
        assertEquals(1L, snoozed.revision)

        val notified = assertNotNull(
            store.markNotified(
                id = reminder.id,
                expectedRevision = snoozed.revision,
                expectedDueAt = snoozed.dueAt,
                now = now.plusSeconds(600).toEpochMilli()
            )
        )
        assertEquals(ReminderState.DUE, notified.state)
        assertEquals(1, notified.notificationCount)
        assertEquals(2L, notified.revision)

        val completed = assertNotNull(store.complete(reminder.id, now.plusSeconds(700).toEpochMilli()))
        assertEquals(ReminderState.COMPLETED, completed.state)
        assertEquals(3L, completed.revision)
        assertTrue(store.active().none { it.id == reminder.id })
        assertTrue(store.delete(reminder.id))
        assertFalse(store.delete(reminder.id))
    }

    @Test
    fun `stale worker revision cannot mark reminder notified`() {
        val store = cleanStore()
        val original = reminder()
        store.upsert(original)
        val snoozed = assertNotNull(store.snooze(original.id, Duration.ofMinutes(10), now.toEpochMilli()))

        val stale = store.markNotified(
            id = original.id,
            expectedRevision = original.revision,
            expectedDueAt = original.dueAt,
            now = original.dueAt
        )

        assertNull(stale)
        assertEquals(snoozed, store.find(original.id))
        assertEquals(0, snoozed.notificationCount)
    }

    @Test
    fun `completed reminder cannot be revived by stale snooze action`() {
        val store = cleanStore()
        store.upsert(reminder())
        val completed = assertNotNull(store.complete("medicine-test", now.toEpochMilli()))

        assertNull(store.snooze(completed.id, Duration.ofMinutes(10), now.plusSeconds(5).toEpochMilli()))
        assertEquals(ReminderState.COMPLETED, store.find(completed.id)?.state)
    }

    @Test
    fun `invalid snooze durations are rejected`() {
        val store = cleanStore()
        store.upsert(reminder())

        assertNull(store.snooze("medicine-test", Duration.ZERO, now.toEpochMilli()))
        assertNull(store.snooze("medicine-test", Duration.ofMinutes(-1), now.toEpochMilli()))
        assertNull(store.snooze("medicine-test", Duration.ofDays(31), now.toEpochMilli()))
        assertEquals(0L, store.find("medicine-test")?.revision)
    }

    @Test
    fun `legacy reminder without revision loads as revision zero`() {
        val context = context()
        context.getSharedPreferences("mayra_owned_reminders", Context.MODE_PRIVATE).edit()
            .putString(
                "items",
                """[{"id":"legacy","title":"Legacy","dueAt":1000,"createdAt":500,"updatedAt":500,"state":"SCHEDULED","priority":"NORMAL","followUp":true,"notificationCount":0,"lastNotifiedAt":null}]"""
            )
            .commit()

        val loaded = assertNotNull(MayraReminderStore(context).find("legacy"))
        assertEquals(0L, loaded.revision)
        assertEquals(ReminderState.SCHEDULED, loaded.state)
    }

    @Test
    fun `lifecycle policy only allows active reminders to mutate`() {
        assertTrue(ReminderLifecyclePolicy.canComplete(ReminderState.DUE))
        assertTrue(ReminderLifecyclePolicy.canSnooze(ReminderState.MISSED))
        assertFalse(ReminderLifecyclePolicy.canNotify(ReminderState.DUE))
        assertFalse(ReminderLifecyclePolicy.canComplete(ReminderState.COMPLETED))
        assertFalse(ReminderLifecyclePolicy.canCancel(ReminderState.CANCELLED))
    }

    private fun cleanStore(): MayraReminderStore {
        val context = context()
        context.getSharedPreferences("mayra_owned_reminders", Context.MODE_PRIVATE).edit().clear().commit()
        return MayraReminderStore(context)
    }

    private fun context(): Context = ApplicationProvider.getApplicationContext()

    private fun reminder() = MayraReminder(
        id = "medicine-test",
        title = "Medicine",
        dueAt = now.plusSeconds(600).toEpochMilli(),
        createdAt = now.toEpochMilli()
    )
}
