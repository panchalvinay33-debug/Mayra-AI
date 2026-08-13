package ai.mayra.app.memory

import java.time.Clock
import java.time.Instant

/**
 * Deterministic owner-preference learner.
 *
 * This is intentionally narrow: it only recognizes explicit, standalone preference statements.
 * It never stores them directly. Every learned preference becomes a normal Mayra memory proposal
 * and therefore still requires owner approval before it can influence future replies.
 */
class MayraPreferenceLearner(
    private val clock: Clock = Clock.systemUTC()
) {
    data class LearnedPreference(
        val key: String,
        val value: String,
        val category: MayraMemoryCategory = MayraMemoryCategory.PREFERENCE,
        val confidence: Double,
        val sourceReference: String
    )

    fun observe(message: String): LearnedPreference? {
        val text = message.trim().replace(Regex("\\s+"), " ")
        if (text.isBlank()) return null

        languagePreference(text)?.let { return it }
        responseLengthPreference(text)?.let { return it }
        return null
    }

    fun toCandidate(preference: LearnedPreference): MayraMemoryCandidate = MayraMemoryCandidate(
        key = preference.key,
        value = preference.value,
        category = preference.category,
        provenance = MayraMemoryProvenance(
            sourceType = "self-learning",
            sourceReference = preference.sourceReference,
            capturedAt = Instant.now(clock)
        )
    )

    private fun languagePreference(text: String): LearnedPreference? {
        val normalized = text.lowercase()
        val value = when {
            EXACT_HINGLISH.any { it.matches(normalized) } -> "Hinglish"
            EXACT_HINDI.any { it.matches(normalized) } -> "Hindi"
            EXACT_ENGLISH.any { it.matches(normalized) } -> "English"
            else -> null
        } ?: return null
        return LearnedPreference(
            key = "response language",
            value = value,
            confidence = 0.99,
            sourceReference = "explicit-language-preference"
        )
    }

    private fun responseLengthPreference(text: String): LearnedPreference? {
        val normalized = text.lowercase()
        val value = when {
            EXACT_SHORT.any { it.matches(normalized) } -> "short"
            EXACT_DETAILED.any { it.matches(normalized) } -> "detailed"
            else -> null
        } ?: return null
        return LearnedPreference(
            key = "response length",
            value = value,
            confidence = 0.98,
            sourceReference = "explicit-response-length-preference"
        )
    }

    private companion object {
        val EXACT_HINGLISH = listOf(
            Regex("^(?:mayra[, ]+)?(?:mujhse|mere se|mujhe)?\\s*(?:hinglish me|hinglish mein)\\s*(?:baat karo|reply karo|jawab do|answer do)$"),
            Regex("^(?:please )?(?:reply|answer|talk) in hinglish$"),
            Regex("^(?:mayra[, ]+)?hinglish me baat karo$")
        )
        val EXACT_HINDI = listOf(
            Regex("^(?:mayra[, ]+)?(?:mujhse|mujhe)?\\s*(?:hindi me|hindi mein)\\s*(?:baat karo|reply karo|jawab do|answer do)$"),
            Regex("^(?:please )?(?:reply|answer|talk) in hindi$"),
            Regex("^(?:हिंदी में|हिन्दी में)\\s*(?:बात करो|जवाब दो)$")
        )
        val EXACT_ENGLISH = listOf(
            Regex("^(?:mayra[, ]+)?(?:mujhse|mujhe)?\\s*english me\\s*(?:baat karo|reply karo|jawab do|answer do)$"),
            Regex("^(?:please )?(?:reply|answer|talk) in english$")
        )
        val EXACT_SHORT = listOf(
            Regex("^(?:mayra[, ]+)?(?:mujhe )?(?:short|chhote|chote)\\s*(?:answer|answers|reply|replies|jawab)\\s*(?:do|dena|pasand hai)$"),
            Regex("^(?:please )?(?:keep|make) (?:your )?(?:answers|replies) short$")
        )
        val EXACT_DETAILED = listOf(
            Regex("^(?:mayra[, ]+)?(?:mujhe )?(?:detailed|detail me|detail mein)\\s*(?:answer|answers|reply|replies|jawab)\\s*(?:do|dena|pasand hai)$"),
            Regex("^(?:please )?(?:give|write) detailed (?:answers|replies)$")
        )
    }
}
