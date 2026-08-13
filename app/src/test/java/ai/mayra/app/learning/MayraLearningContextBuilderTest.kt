package ai.mayra.app.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraLearningContextBuilderTest {
    @Test fun emptyWhenNoApprovedMemory() {
        assertEquals("", MayraLearningContextBuilder.build(emptyList()))
        assertEquals("", MayraLearningContextBuilder.build(listOf(memory(state = "PENDING"))))
    }

    @Test fun includesOnlyApprovedNonBlankValues() {
        val context = MayraLearningContextBuilder.build(
            listOf(
                memory(key = "response language", value = "Hinglish"),
                memory(key = "pending", value = "secret", state = "PENDING"),
                memory(key = "blank", value = "")
            )
        )
        assertTrue(context.contains("OWNER-APPROVED MAYRA MEMORY"))
        assertTrue(context.contains("[response language] Hinglish"))
        assertFalse(context.contains("secret"))
        assertFalse(context.contains("[blank]"))
    }

    @Test fun contextIsBoundedAndNewlinesAreFlattened() {
        val context = MayraLearningContextBuilder.build(
            listOf(memory(key = "style\nkey", value = "one\ntwo")),
            maxCharacters = 256
        )
        assertTrue(context.length <= 256)
        assertTrue(context.contains("[style key] one two"))
    }

    @Test fun capsMemoryCount() {
        val context = MayraLearningContextBuilder.build(
            (1..30).map { memory(key = "k$it", value = "v$it") },
            maxCharacters = 4000
        )
        assertTrue(context.contains("[k20] v20"))
        assertFalse(context.contains("[k21] v21"))
    }

    private fun memory(
        key: String = "language",
        value: String = "Hindi",
        state: String = LearnedMemoryState.APPROVED.name
    ) = LearnedMemoryEntity(
        id = 1,
        normalizedKey = MayraLearningRepository.normalizeKey(key),
        displayKey = key,
        value = value,
        category = LearningCategory.LANGUAGE_STYLE.name,
        source = LearningSource.EXPLICIT_OWNER_STATEMENT.name,
        confidence = 1.0,
        persistence = LearningPersistence.LONG_TERM.name,
        state = state,
        policyReason = "test",
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
        approvedAtEpochMs = if (state == LearnedMemoryState.APPROVED.name) 1 else null
    )
}
