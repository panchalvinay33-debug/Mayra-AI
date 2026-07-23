package ai.mayra.app.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTermMemoryEngineTest {
    @Test
    fun `upsert normalizes keys and preserves original creation time`() {
        val engine = LongTermMemoryEngine()
        engine.remember(" User Profile ", "Preferred Language", " Hindi ", MemoryKind.PREFERENCE, timestamp = 10L)
        val updated = engine.remember("user profile", "preferred language", "Hinglish", MemoryKind.PREFERENCE, timestamp = 20L)

        assertEquals("user_profile:preferred_language", updated.id)
        assertEquals(10L, updated.createdAt)
        assertEquals(20L, updated.updatedAt)
        assertEquals("Hinglish", engine.get("USER PROFILE", "Preferred Language")?.value)
    }

    @Test
    fun `search ranks key matches above value matches`() {
        val engine = LongTermMemoryEngine()
        engine.remember("profile", "language", "Hindi", timestamp = 1L)
        engine.remember("business", "notes", "Language preference for invoices", timestamp = 2L)

        val results = engine.search("language")

        assertEquals("profile:language", results.first().record.id)
        assertTrue(results.first().score > results.last().score)
    }

    @Test
    fun `confidence kind filtering and namespace clearing work`() {
        val engine = LongTermMemoryEngine()
        engine.remember("home", "city", "Pitol", MemoryKind.PROFILE, confidence = 0.9, timestamp = 1L)
        engine.remember("home", "snack", "Tea", MemoryKind.PREFERENCE, confidence = 0.3, timestamp = 2L)

        val results = engine.search("home", minimumConfidence = 0.8, kinds = setOf(MemoryKind.PROFILE))
        assertEquals(1, results.size)
        assertEquals(2, engine.clear("home"))
        assertNull(engine.get("home", "city"))
    }

    @Test
    fun `forget reports whether a record existed`() {
        val engine = LongTermMemoryEngine()
        engine.remember("project", "name", "Mayra AI", timestamp = 1L)

        assertTrue(engine.forget("project", "name"))
        assertFalse(engine.forget("project", "name"))
    }
}
