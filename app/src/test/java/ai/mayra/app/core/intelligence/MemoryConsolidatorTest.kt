package ai.mayra.app.core.intelligence

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryConsolidatorTest {
    @Test
    fun `high value memory is saved and duplicate is skipped`() = runTest {
        val store = InMemoryMemoryStore()
        val consolidator = MemoryConsolidator(
            store = store,
            policy = MemoryConsolidationPolicy(minimumImportance = 40, duplicateThreshold = 0.7)
        )

        val first = consolidator.consolidate(
            listOf(MemoryCandidate("Remember that the user always prefers Hindi replies", setOf("preference")))
        )
        val second = consolidator.consolidate(
            listOf(MemoryCandidate("The user always prefers replies in Hindi", setOf("preference")))
        )

        assertEquals(1, first.saved.size)
        assertEquals(1, second.skippedDuplicates.size)
        assertEquals(1, store.all().size)
    }

    @Test
    fun `low importance candidate is not persisted`() = runTest {
        val store = InMemoryMemoryStore()
        val consolidator = MemoryConsolidator(
            store = store,
            policy = MemoryConsolidationPolicy(minimumImportance = 80)
        )

        val result = consolidator.consolidate(
            listOf(MemoryCandidate("For now the user is currently waiting"))
        )

        assertTrue(result.saved.isEmpty())
        assertEquals(1, result.skippedLowImportance.size)
        assertTrue(store.all().isEmpty())
    }
}