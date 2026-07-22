package ai.mayra.app.core.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LongTermMemoryEngineTest {

    @Test
    fun rememberNormalizesAndRetrievesRecord() {
        val engine = LongTermMemoryEngine()

        val stored = engine.remember(
            namespace = " User Profile ",
            key = " Preferred Name ",
            value = "  Vinay   Panchal  ",
            kind = MemoryKind.PROFILE,
            timestamp = 100L,
            source = " conversation "
        )

        assertEquals("user_profile", stored.namespace)
        assertEquals("preferred_name", stored.key)
        assertEquals("Vinay Panchal", stored.value)
        assertEquals("conversation", stored.source)
        assertEquals(stored, engine.get("USER profile", "preferred-name"))
    }

    @Test
    fun upsertPreservesCreatedAtAndUpdatesValue() {
        val engine = LongTermMemoryEngine()
        engine.remember("preferences", "language", "Hindi", timestamp = 10L)

        val updated = engine.upsert(
            MemoryRecord(
                namespace = "preferences",
                key = "language",
                value = "Hindi and English",
                kind = MemoryKind.PREFERENCE,
                confidence = 0.9,
                createdAt = 20L,
                updatedAt = 30L
            )
        )

        assertEquals(10L, updated.createdAt)
        assertEquals(30L, updated.updatedAt)
        assertEquals("Hindi and English", updated.value)
        assertEquals(1, engine.snapshot().totalCount)
    }

    @Test
    fun searchRanksKeyMatchesAheadOfValueMatches() {
        val engine = LongTermMemoryEngine(
            listOf(
                MemoryRecord("profile", "city", "Pitol", createdAt = 1L),
                MemoryRecord("project", "mayra_ai", "Assistant project for Pitol", createdAt = 2L),
                MemoryRecord("profile", "district", "Jhabua", createdAt = 3L)
            )
        )

        val results = engine.search("city pitol")

        assertEquals(2, results.size)
        assertEquals("profile:city", results.first().record.id)
        assertTrue(results.first().score > results.last().score)
    }

    @Test
    fun searchSupportsConfidenceAndKindFilters() {
        val engine = LongTermMemoryEngine(
            listOf(
                MemoryRecord(
                    "preferences",
                    "photo_style",
                    "realistic photography",
                    MemoryKind.PREFERENCE,
                    confidence = 0.95,
                    createdAt = 1L
                ),
                MemoryRecord(
                    "projects",
                    "photo_app",
                    "realistic photography editor",
                    MemoryKind.PROJECT,
                    confidence = 0.4,
                    createdAt = 2L
                )
            )
        )

        val results = engine.search(
            query = "realistic photography",
            minimumConfidence = 0.8,
            kinds = setOf(MemoryKind.PREFERENCE)
        )

        assertEquals(1, results.size)
        assertEquals("preferences:photo_style", results.single().record.id)
    }

    @Test
    fun forgetAndNamespaceClearReturnAccurateCounts() {
        val engine = LongTermMemoryEngine()
        engine.remember("profile", "name", "Vinay", timestamp = 1L)
        engine.remember("profile", "city", "Pitol", timestamp = 2L)
        engine.remember("preferences", "language", "Hindi", timestamp = 3L)

        assertTrue(engine.forget("profile", "name"))
        assertFalse(engine.forget("profile", "name"))
        assertNull(engine.get("profile", "name"))
        assertEquals(1, engine.clear("profile"))
        assertEquals(1, engine.snapshot().totalCount)
        assertEquals(1, engine.clear())
        assertEquals(0, engine.snapshot().totalCount)
    }

    @Test
    fun snapshotIsDeterministicAndReportsMetadata() {
        val engine = LongTermMemoryEngine()
        engine.remember("zeta", "second", "B", timestamp = 20L)
        engine.remember("alpha", "first", "A", timestamp = 10L)

        val snapshot = engine.snapshot()

        assertEquals(listOf("alpha:first", "zeta:second"), snapshot.records.map { it.id })
        assertEquals(setOf("alpha", "zeta"), snapshot.namespaces)
        assertEquals(20L, snapshot.lastUpdatedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun recordRejectsInvalidConfidence() {
        MemoryRecord(
            namespace = "profile",
            key = "name",
            value = "Vinay",
            confidence = 1.1,
            createdAt = 1L
        )
    }
}
