package ai.mayra.app.core.intelligence

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemoryRetrieverTest {

    @Test
    fun `retrieval filters by importance and limit`() = runBlocking {
        val store = InMemoryMemoryStore()
        store.save(MemoryRecord(id = "low", content = "user likes tea", importance = 20))
        store.save(MemoryRecord(id = "high", content = "user likes tea in morning", importance = 90))
        store.save(MemoryRecord(id = "mid", content = "tea preference", importance = 60))

        val result = MemoryRetriever(store).retrieve(
            text = "tea",
            limit = 2,
            minimumImportance = 50
        )

        assertEquals(listOf("high", "mid"), result.map { it.id })
    }

    @Test
    fun `retrieval respects tags`() = runBlocking {
        val store = InMemoryMemoryStore()
        store.save(MemoryRecord(id = "food", content = "likes spicy food", tags = setOf("preference")))
        store.save(MemoryRecord(id = "work", content = "likes focused work", tags = setOf("productivity")))

        val result = MemoryRetriever(store).retrieve(
            text = "likes",
            tags = setOf("productivity")
        )

        assertEquals(listOf("work"), result.map { it.id })
    }

    @Test
    fun `blank retrieval text is rejected`() = runBlocking {
        val retriever = MemoryRetriever(InMemoryMemoryStore())
        assertFailsWith<IllegalArgumentException> { retriever.retrieve(" ") }
    }
}