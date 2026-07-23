package ai.mayra.app.core.memory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ai.mayra.app.data.local.MayraDatabase
import ai.mayra.app.data.repository.ConversationRepository
import ai.mayra.app.data.repository.MemoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraMemoryManagerTest {
    private lateinit var database: MayraDatabase
    private lateinit var manager: MayraMemoryManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MayraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = MayraMemoryManager(
            memoryRepository = MemoryRepository(database.memoryDao()),
            conversationRepository = ConversationRepository(database.conversationDao()),
            clock = { 1_000L }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun rememberNormalizesValuesAndPersistsMemory() = runTest {
        val id = manager.remember(
            content = "  User prefers Hindi  ",
            category = "  preference  ",
            importance = 99
        )

        val stored = database.memoryDao().getById(id)

        assertEquals("User prefers Hindi", stored?.content)
        assertEquals("preference", stored?.category)
        assertEquals(MayraMemoryManager.MAX_IMPORTANCE, stored?.importance)
        assertEquals(1_000L, stored?.createdAt)
    }

    @Test
    fun recallReturnsEmptyForBlankQuery() = runTest {
        manager.remember("User prefers Hindi")

        assertTrue(manager.recall("   ").isEmpty())
    }

    @Test
    fun conversationMessagesAreStoredChronologically() = runTest {
        val sessionId = "session-1"
        manager.appendUserMessage(sessionId, " Hello ")
        manager.appendAssistantMessage(sessionId, " Hi! ")

        val messages = manager.recentConversation(sessionId)

        assertEquals(listOf("Hello", "Hi!"), messages.map { it.message })
        assertEquals(listOf("user", "assistant"), messages.map { it.role })
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankMemoryIsRejected() = runTest {
        manager.remember("   ")
    }
}
