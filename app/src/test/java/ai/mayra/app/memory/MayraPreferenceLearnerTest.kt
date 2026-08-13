package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraPreferenceLearnerTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-05T12:00:00Z"), ZoneOffset.UTC)
    private val learner = MayraPreferenceLearner(clock)

    @Test fun learnsExplicitHinglishPreference() {
        val learned = learner.observe("Hinglish me baat karo")
        assertEquals("response language", learned?.key)
        assertEquals("Hinglish", learned?.value)
        assertTrue((learned?.confidence ?: 0.0) >= 0.98)
    }

    @Test fun learnsHindiAndEnglishPreferences() {
        assertEquals("Hindi", learner.observe("reply in Hindi")?.value)
        assertEquals("English", learner.observe("please answer in English")?.value)
    }

    @Test fun learnsResponseLengthPreference() {
        assertEquals("short", learner.observe("short answer do")?.value)
        assertEquals("detailed", learner.observe("please give detailed answers")?.value)
    }

    @Test fun ordinaryRequestsAreNeverLearned() {
        assertNull(learner.observe("Hindi me Delhi ka weather batao"))
        assertNull(learner.observe("Mujhe kal 8 baje yaad dilana"))
        assertNull(learner.observe("Open WhatsApp"))
    }

    @Test fun candidateHasSelfLearningProvenance() {
        val candidate = learner.toCandidate(learner.observe("Hinglish me baat karo")!!)
        assertEquals(MayraMemoryCategory.PREFERENCE, candidate.category)
        assertEquals("self-learning", candidate.provenance.sourceType)
        assertEquals(Instant.parse("2026-08-05T12:00:00Z"), candidate.provenance.capturedAt)
    }

    @Test fun learnedPreferenceStillRequiresOwnerApprovalBeforeStorage() {
        val manager = MayraPersonalMemoryManager(MayraInMemoryPersonalMemoryStore(), clock)
        val controller = MayraMemoryChatController(manager, clock, learner)

        val result = controller.handle("Hinglish me baat karo")

        assertTrue(result is MayraMemoryChatResult.NeedsApproval)
        assertTrue(manager.activeMemories().isEmpty())
        assertEquals(1, manager.pendingCount())
    }
}
