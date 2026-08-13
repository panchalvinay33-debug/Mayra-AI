package ai.mayra.app.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraLearningReviewModelTest {
    @Test fun separatesPendingAndApprovedAndHidesOtherStates() {
        val snapshot = MayraLearningReviewModel.snapshot(
            pending = listOf(memory("p", LearnedMemoryState.PENDING, 2)),
            approved = listOf(
                memory("a", LearnedMemoryState.APPROVED, 3),
                memory("x", LearnedMemoryState.FORGOTTEN, 4, value = "")
            )
        )
        assertEquals(1, snapshot.pendingCount)
        assertEquals(1, snapshot.approvedCount)
        assertEquals("p", snapshot.pending.single().key)
        assertEquals("a", snapshot.approved.single().key)
    }

    @Test fun sortsNewestFirstAndBoundsSections() {
        val memories = (1L..120L).map { memory("k$it", LearnedMemoryState.APPROVED, it) }
        val snapshot = MayraLearningReviewModel.snapshot(emptyList(), memories, maxPerSection = 20)
        assertEquals(20, snapshot.approvedCount)
        assertEquals("k120", snapshot.approved.first().key)
        assertEquals("k101", snapshot.approved.last().key)
    }

    @Test fun flattensAndBoundsDisplayValues() {
        val snapshot = MayraLearningReviewModel.snapshot(
            emptyList(),
            listOf(memory("language", LearnedMemoryState.APPROVED, 1, value = "Hindi\nHinglish  " + "x".repeat(400)))
        )
        val value = snapshot.approved.single().value
        assertTrue(!value.contains('\n'))
        assertTrue(value.length <= 300)
    }

    private fun memory(
        key: String,
        state: LearnedMemoryState,
        updated: Long,
        value: String = "value"
    ) = LearnedMemoryEntity(
        normalizedKey = key,
        displayKey = key,
        value = value,
        category = LearningCategory.OTHER.name,
        source = LearningSource.EXPLICIT_OWNER_STATEMENT.name,
        confidence = 1.0,
        persistence = LearningPersistence.LONG_TERM.name,
        state = state.name,
        policyReason = "test",
        createdAtEpochMs = 1,
        updatedAtEpochMs = updated
    )
}
