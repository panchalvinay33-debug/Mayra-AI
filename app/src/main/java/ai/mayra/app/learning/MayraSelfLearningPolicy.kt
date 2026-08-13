package ai.mayra.app.learning

/** Deterministic trust boundary for Mayra self-learning. */
object MayraSelfLearningPolicy {
    private val forbiddenKeyFragments = listOf(
        "password", "passcode", "pin", "otp", "cvv", "card_number", "upi_pin",
        "bank_account", "aadhaar", "pan_number", "private_key", "api_key", "token", "secret"
    )
    private val forbiddenValuePatterns = listOf(
        Regex("(?i)\\b(?:otp|cvv|upi\\s*pin|password|passcode|api\\s*key|private\\s*key)\\b"),
        Regex("\\b\\d{12}\\b"),
        Regex("\\b\\d{13,19}\\b"),
        Regex("(?i)\\b[A-Z]{5}[0-9]{4}[A-Z]\\b")
    )
    private val sensitiveCategories = setOf(
        LearningCategory.IDENTITY, LearningCategory.HEALTH, LearningCategory.FINANCE,
        LearningCategory.RELATIONSHIP, LearningCategory.LOCATION_PATTERN,
        LearningCategory.CONTACT_PREFERENCE
    )

    fun evaluate(candidate: LearningCandidate): LearningDecision {
        val key = candidate.key.trim().lowercase()
        val value = candidate.value.trim()
        if (key.isBlank() || value.isBlank()) return LearningDecision.Reject("empty key or value")
        if (key.length > 80 || value.length > 500) return LearningDecision.Reject("candidate exceeds bounded storage limits")
        if (!candidate.confidence.isFinite() || candidate.confidence !in 0.0..1.0) return LearningDecision.Reject("invalid confidence")
        if (forbiddenKeyFragments.any(key::contains) || forbiddenValuePatterns.any { it.containsMatchIn(value) }) {
            return LearningDecision.Reject("secret or credential-like information")
        }
        if (candidate.source == LearningSource.MODEL_INFERENCE && candidate.confidence < 0.90) {
            return LearningDecision.Reject("model inference confidence too low")
        }
        if (candidate.category in sensitiveCategories) return LearningDecision.RequireConfirmation("sensitive personal memory")
        if (candidate.persistence == LearningPersistence.PERMANENT) return LearningDecision.RequireConfirmation("permanent memory requires owner approval")
        if (candidate.source == LearningSource.EXPLICIT_OWNER_STATEMENT) return LearningDecision.RequireConfirmation("explicit owner statement should be reviewable")
        return when (candidate.category) {
            LearningCategory.RESPONSE_STYLE, LearningCategory.LANGUAGE_STYLE, LearningCategory.UI_PREFERENCE ->
                LearningDecision.AllowLowRisk("reversible low-risk preference")
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
    RESPONSE_STYLE, LANGUAGE_STYLE, UI_PREFERENCE, ROUTINE, CONTACT_PREFERENCE,
    LOCATION_PATTERN, IDENTITY, HEALTH, FINANCE, RELATIONSHIP, OTHER
}

enum class LearningSource { EXPLICIT_OWNER_STATEMENT, OWNER_CORRECTION, REPEATED_BEHAVIOR, MODEL_INFERENCE }
enum class LearningPersistence { SESSION, LONG_TERM, PERMANENT }

sealed interface LearningDecision {
    val reason: String
    data class AllowLowRisk(override val reason: String) : LearningDecision
    data class RequireConfirmation(override val reason: String) : LearningDecision
    data class Reject(override val reason: String) : LearningDecision
}
