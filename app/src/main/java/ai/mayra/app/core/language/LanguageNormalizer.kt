package ai.mayra.app.core.language

import java.util.Locale

/**
 * Normalizes English, Hindi, and common Hinglish input before intent matching.
 * It preserves Devanagari text while making punctuation and spacing predictable.
 */
class LanguageNormalizer {

    fun normalize(input: String): String = input
        .trim()
        .lowercase(Locale.ROOT)
        .replace(SMART_APOSTROPHES, "'")
        .replace(PUNCTUATION, " ")
        .replace(MULTIPLE_SPACES, " ")
        .trim()

    fun tokens(input: String): List<String> {
        val normalized = normalize(input)
        return if (normalized.isEmpty()) emptyList() else normalized.split(' ')
    }

    companion object {
        private val SMART_APOSTROPHES = Regex("[‘’`]")
        private val PUNCTUATION = Regex("[^\\p{L}\\p{N}']+")
        private val MULTIPLE_SPACES = Regex("\\s+")
    }
}
