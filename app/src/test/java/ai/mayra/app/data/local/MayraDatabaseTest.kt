package ai.mayra.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraDatabaseTest {
    private lateinit var database: MayraDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MayraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun memoryCanBeStoredAndSearched() = runTest {
        val id = database.memoryDao().upsert(
            MemoryEntity(category = "preference", content = "User prefers Hindi", importance = 8)
        )

        val stored = database.memoryDao().getById(id)
        val results = database.memoryDao().search("Hindi")

        assertNotNull(stored)
        assertEquals("preference", stored?.category)
        assertEquals(1, results.size)
    }

    @Test
    fun conversationSessionIsObservedInChronologicalOrder() = runTest {
        val dao = database.conversationDao()
        dao.insert(ConversationEntity(sessionId = "session-1", role = "user", message = "Hello", createdAt = 1))
        dao.insert(ConversationEntity(sessionId = "session-1", role = "assistant", message = "Hi", createdAt = 2))

        val messages = dao.observeSession("session-1").first()

        assertEquals(listOf("Hello", "Hi"), messages.map { it.message })
    }

    @Test
    fun dueReminderCanBeCompleted() = runTest {
        val dao = database.reminderDao()
        val id = dao.upsert(ReminderEntity(title = "Call", triggerAt = 100))

        assertEquals(1, dao.due(now = 100).size)
        assertEquals(1, dao.markCompleted(id))
        assertFalse(dao.observePending().first().any { it.id == id })
    }
}
