package ai.mayra.app.core.intelligence

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryMemoryStoreTest {
    @Test
    fun `search ranks relevance before importance`() = runTest {
        val store = InMemoryMemoryStore()
        store.save(MemoryRecord(id = "important", content = "user likes tea", importance = 100))
        store.save(MemoryRecord(id = "relevant", content = "user likes tea with less sugar", importance = 10))
        store.save(MemoryRecord(id = "other", content = "user likes music", importance = 100))

        val results = store.search(MemoryQuery("tea sugar"))

        assertEquals(listOf("relevant", "important"), results.map { it.id })
    }

    @Test
    fun `tag filtering and limit are deterministic`() = runTest {
        val store = InMemoryMemoryStore()
        store.save(MemoryRecord(id = "one", content = "camera preference", tags = setOf("profile"), createdAt = Instant.parse("2026-01-01T00:00:00Z")))
        store.save(MemoryRecord(id = "two", content = "camera setting", tags = setOf("profile"), createdAt = Instant.parse("2026-02-01T00:00:00Z")))
        store.save(MemoryRecord(id = "three", content = "camera device", tags = setOf("device"), createdAt = Instant.parse("2026-03-01T00:00:00Z")))

        val results = store.search(MemoryQuery(text = "camera", tags = setOf("profile"), limit = 1))

        assertEquals(listOf("two"), results.map { it.id })
    }

    @Test
    fun `save get delete lifecycle works`() = runTest {
        val store = InMemoryMemoryStore()
        val record = MemoryRecord(id = "memory-1", content = "Call mom tomorrow")

        store.save(record)
        assertEquals(record, store.get("memory-1"))
        assertTrue(store.delete("memory-1"))
        assertFalse(store.delete("memory-1"))
        assertNull(store.get("memory-1"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank memory content is rejected`() {
        MemoryRecord(content = "   ")
    }
}
