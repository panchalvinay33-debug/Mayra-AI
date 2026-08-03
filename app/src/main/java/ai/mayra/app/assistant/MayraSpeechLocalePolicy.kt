package ai.mayra.app.assistant

import java.util.Locale

object MayraSpeechLocalePolicy {
    fun candidates(deviceLocaleTag: String): List<String> = buildList {
        fun addIfNew(rawTag: String) {
            val normalized = normalize(rawTag) ?: return
            if (none { it.equals(normalized, ignoreCase = true) }) add(normalized)
        }

        addIfNew(deviceLocaleTag)
        addIfNew("hi-IN")
        addIfNew("en-IN")
        addIfNew("en-US")
    }

    fun currentDeviceLocaleTag(): String = Locale.getDefault().toLanguageTag()

    private fun normalize(rawTag: String): String? {
        val cleaned = rawTag.trim().replace('_', '-')
        if (cleaned.isBlank()) return null

        val normalized = Locale.forLanguageTag(cleaned).toLanguageTag()
        return normalized.takeUnless { it.isBlank() || it.equals("und", ignoreCase = true) }
    }
}
