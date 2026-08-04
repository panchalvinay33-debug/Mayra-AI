package ai.mayra.app.learning

/**
 * Deterministic safety boundary for Mayra self-learning.
 *
 * The language model may propose a candidate, but only this policy can decide whether it may be
 * shown for confirmation, saved automatically as a low-risk preference, or rejected completely.
 */
object MayraSelfLearningPolicy {
    private val forbiddenKeyFragments = listOf(
        "password", "passcode", "pin", "otp", "cvv", "card_number", "upi_pin",
        "bank_account", "aadhaar", "pan_number", "private_key", "api_key", "token"
    )

    private val sensitiveCategories = setOf(
        LearningCategory.IDENTITY,
        LearningCategory.HEALTH,
        LearningCategory.FINANCE,
        LearningCategory.RELATIONSHIP,
        LearningCategory.LOCATION_PATTERN,
        LearningCategory.CONTACT_PREFERENCE
    )

    fun evaluate(candidate: LearningCandidate): LearningDecision {
        val key = candidate.key.trim().lowercase()
        val value = candidate.value.trim()

        if (key.isBlank() || value.isBlank()) {
            return LearningDecision.Reject("empty key or value")
        }
        if (key.length > 80 || value.length > 500) {
            return LearningDecision.Reject("candidate exceeds bounded storage limits")
        }
        if (forbiddenKeyFragments.any(key::contains)) {
            return LearningDecision.Reject("secret or credential-like information")
        }
        if (candidate.source == LearningSource.MODEL_INFERENCE && candidate.confidence < 0.90) {
            return LearningDecision.Reject("model inference confidence too low")
        }
        if (candidate.category in sensitiveCategories) {
            return LearningDecision.RequireConfirmation("sensitive personal memory")
        }
        if (candidate.persistence == LearningPersistence.PERMANENT) {
            return LearningDecision.RequireConfirmation("permanent memory requires owner approval")
        }
        if (candidate.source == LearningSource.EXPLICIT_OWNER_STATEMENT) {
            return LearningDecision.RequireConfirmation("explicit owner statement should be reviewable")
        }

        return when (candidate.category) {
            LearningCategory.RESPONSE_STYLE,
            LearningCategory.LANGUAGE_STYLE,
            LearningCategory.UI_PREFERENCE -> LearningDecision.AllowLowRisk("reversible low-risk preference")
            else -> LearningDecision.RequireConfirmation("owner confirmation required")
        }
    }
}

data class LearningCandidate(
    val key: String,
    val value: String,
    val category: LearningCategory,
    val source: LearningSource,
    val confidence: Double,
    val persistence: LearningPersistence = LearningPersistence.SESSION
)

enum class LearningCategory {
    RESPONSE_STYLE,
    LANGUAGE_STYLE,
    UI_PREFERENCE,
    ROUTINE,
    CONTACT_PREFERENCE,
    LOCATION_PATTERN,
    IDENTITY,
    HEALTH,
    FINANCE,
    RELATIONSHIP,
    OTHER
}

enum class LearningSource {
    EXPLICIT_OWNER_STATEMENT,
    OWNER_CORRECTION,
    REPEATED_BEHAVIOR,
    MODEL_INFERENCE
}

enum class LearningPersistence { SESSION, LONG_TERM, PERMANENT }

sealed interface LearningDecision {
    val reason: String

    data class AllowLowRisk(override val reason: String) : LearningDecision
    data class RequireConfirmation(override val reason: String) : LearningDecision
    data class Reject(override val reason: String) : LearningDecision
}
