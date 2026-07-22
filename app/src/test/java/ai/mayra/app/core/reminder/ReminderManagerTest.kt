package ai.mayra.app.core.reminder

import ai.mayra.app.data.local.MayraDatabase
import ai.mayra.app.data.repository.ReminderRepository
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderManagerTest {
    private lateinit var database: MayraDatabase
    private lateinit var manager: ReminderManager
    private var now = 1_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MayraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = ReminderManager(ReminderRepository(database.reminderDao())) { now }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createNormalizesFieldsAndPersistsReminder() = runTest {
        val id = manager.create(
            title = "  Call Rahul  ",
            triggerAt = now + 60_000,
            description = "  Discuss event  ",
            repeatRule = " DAILY "
        )

        val stored = manager.get(id)

        assertEquals("Call Rahul", stored?.title)
        assertEquals("Discuss event", stored?.description)
        assertEquals(ReminderManager.REPEAT_DAILY, stored?.repeatRule)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pastReminderIsRejected() = runTest {
        manager.create("Past", triggerAt = now - 1)
    }

    @Test
    fun dueCompleteAndPendingFlowStayConsistent() = runTest {
        val id = manager.create("Medicine", triggerAt = now)

        assertEquals(listOf(id), manager.due().map { it.id })
        assertTrue(manager.complete(id))
        assertTrue(manager.due().isEmpty())
        assertFalse(manager.observePending().first().any { it.id == id })
    }

    @Test
    fun snoozeMovesReminderForwardFromLatestRelevantTime() = runTest {
        val id = manager.create("Water plants", triggerAt = now + 120_000)

        assertTrue(manager.snooze(id, 60_000))
        assertEquals(now + 180_000, manager.get(id)?.triggerAt)

        now += 300_000
        assertTrue(manager.snooze(id, 60_000))
        assertEquals(now + 60_000, manager.get(id)?.triggerAt)
    }

    @Test
    fun cancellingMissingReminderReturnsFalse() = runTest {
        assertFalse(manager.cancel(999))
        assertNull(manager.get(999))
    }

    @Test
    fun cancelDeletesExistingReminder() = runTest {
        val id = manager.create("Temporary", triggerAt = now + 60_000)

        assertTrue(manager.cancel(id))
        assertNull(manager.get(id))
    }
}
