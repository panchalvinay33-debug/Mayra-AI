package ai.mayra.app.learning

import org.junit.Assert.assertTrue
import org.junit.Test

class MayraSelfLearningPolicyTest {
    @Test
    fun rejectsSecrets() {
        val decision = MayraSelfLearningPolicy.evaluate(
            candidate(key = "upi_pin", value = "1234", category = LearningCategory.FINANCE)
        )
        assertTrue(decision is LearningDecision.Reject)
    }

    @Test
    fun sensitiveMemoryRequiresConfirmation() {
        val decision = MayraSelfLearningPolicy.evaluate(
            candidate(key = "medicine", value = "take after dinner", category = LearningCategory.HEALTH)
        )
        assertTrue(decision is LearningDecision.RequireConfirmation)
    }

    @Test
    fun permanentMemoryRequiresConfirmation() {
        val decision = MayraSelfLearningPolicy.evaluate(
            candidate(
                key = "preferred_language",
                value = "Hinglish",
                category = LearningCategory.LANGUAGE_STYLE,
                persistence = LearningPersistence.PERMANENT
            )
        )
        assertTrue(decision is LearningDecision.RequireConfirmation)
    }

    @Test
    fun lowRiskStyleCanBeLearnedReversibly() {
        val decision = MayraSelfLearningPolicy.evaluate(
            candidate(
                key = "response_length",
                value = "short",
                category = LearningCategory.RESPONSE_STYLE,
                source = LearningSource.REPEATED_BEHAVIOR,
                confidence = 0.97
            )
        )
        assertTrue(decision is LearningDecision.AllowLowRisk)
    }

    @Test
    fun uncertainModelInferenceIsRejected() {
        val decision = MayraSelfLearningPolicy.evaluate(
            candidate(
                key = "preferred_language",
                value = "Hindi",
                category = LearningCategory.LANGUAGE_STYLE,
                source = LearningSource.MODEL_INFERENCE,
                confidence = 0.65
            )
        )
        assertTrue(decision is LearningDecision.Reject)
    }

    private fun candidate(
        key: String,
        value: String,
        category: LearningCategory,
        source: LearningSource = LearningSource.EXPLICIT_OWNER_STATEMENT,
        confidence: Double = 1.0,
        persistence: LearningPersistence = LearningPersistence.LONG_TERM
    ) = LearningCandidate(key, value, category, source, confidence, persistence)
}
