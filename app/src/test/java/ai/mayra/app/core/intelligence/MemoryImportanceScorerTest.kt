package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemoryImportanceScorerTest {
    private val scorer = MemoryImportanceScorer()

    @Test
    fun `durable preferences score above transient notes`() {
        val durable = scorer.score("Remember that the user always prefers Hindi replies", setOf("preference"))
        val transient = scorer.score("For now the user is currently waiting")

        assertTrue(durable > transient)
        assertTrue(durable in 0..100)
        assertTrue(transient in 0..100)
    }

    @Test
    fun `blank memory is rejected`() {
        assertFailsWith<IllegalArgumentException> { scorer.score("   ") }
    }
}