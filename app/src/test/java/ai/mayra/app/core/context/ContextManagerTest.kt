package ai.mayra.app.core.context

import ai.mayra.app.core.memory.MayraMemoryManager
import ai.mayra.app.data.local.MayraDatabase
import ai.mayra.app.data.local.MemoryEntity
import ai.mayra.app.data.repository.ConversationRepository
import ai.mayra.app.data.repository.MemoryRepository
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContextManagerTest {
    private lateinit var database: MayraDatabase
    private lateinit var manager: ContextManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MayraDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        manager = ContextManager(
            MayraMemoryManager(
                memoryRepository = MemoryRepository(database.memoryDao()),
                conversationRepository = ConversationRepository(database.conversationDao()),
                clock = { 100L }
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun buildUsesLatestUserMessageAsDefaultRecallQuery() = runTest {
        database.memoryDao().upsert(
            MemoryEntity(
                category = "preference",
                content = "User likes Hindi replies",
                importance = 9,
                createdAt = 1,
                updatedAt = 1
            )
        )
        manager.recordExchange("s1", "Please reply in Hindi", "Bilkul")

        val context = manager.build("s1")

        assertEquals("Please reply in Hindi", context.lastUserMessage)
        assertEquals("Bilkul", context.lastAssistantMessage)
        assertEquals(1, context.recalledMemories.size)
    }

    @Test
    fun explicitMemoryQueryOverridesLatestMessage() = runTest {
        database.memoryDao().upsert(
            MemoryEntity(category = "business", content = "Tent house inventory", importance = 8)
        )
        manager.recordExchange("s2", "Hello", "Hi")

        val context = manager.build("s2", memoryQuery = "inventory")

        assertEquals("Tent house inventory", context.recalledMemories.single().content)
    }

    @Test
    fun blankSessionIsRejected() = runTest {
        val result = runCatching { manager.build("   ") }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
