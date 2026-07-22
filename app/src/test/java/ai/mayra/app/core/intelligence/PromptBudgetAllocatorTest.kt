package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PromptBudgetAllocatorTest {

    private val allocator = PromptBudgetAllocator(
        minimumInputCharacters = 500,
        maximumPromptCharacters = 4_000,
        defaultResponseCharacters = 1_000
    )

    @Test
    fun `small contexts retain full target and memory capacity`() {
        val allocation = allocator.allocate(contextCharacters = 800, memoryCandidates = 8)

        assertEquals(800, allocation.contextCharacterTarget)
        assertEquals(8, allocation.memoryItemLimit)
        assertEquals(3_000, allocation.budget.inputCharacterLimit)
    }

    @Test
    fun `oversized contexts reduce target and memory count`() {
        val allocation = allocator.allocate(contextCharacters = 8_000, memoryCandidates = 12)

        assertEquals(4, allocation.memoryItemLimit)
        assertTrue(allocation.contextCharacterTarget < allocation.budget.inputCharacterLimit)
        assertTrue("context_compression_required" in allocation.rationale)
    }

    @Test
    fun `requested output is capped while preserving minimum input`() {
        val allocation = allocator.allocate(
            contextCharacters = 2_000,
            requestedOutputCharacters = 10_000
        )

        assertEquals(500, allocation.budget.inputCharacterLimit)
        assertEquals(3_500, allocation.budget.reservedResponseCharacters)
    }
}
