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

    fun installedCandidates(
        preferred: List<String>,
        installed: List<String>
    ): List<String> {
        val normalizedInstalled = installed.mapNotNull(::normalize).distinctBy { it.lowercase(Locale.ROOT) }
        if (normalizedInstalled.isEmpty()) return emptyList()

        val ordered = mutableListOf<String>()
        preferred.mapNotNull(::normalize).forEach { wanted ->
            normalizedInstalled.firstOrNull { it.equals(wanted, ignoreCase = true) }?.let { match ->
                if (ordered.none { it.equals(match, ignoreCase = true) }) ordered += match
            }
        }

        normalizedInstalled.forEach { available ->
            if (ordered.none { it.equals(available, ignoreCase = true) }) ordered += available
        }
        return ordered
    }

    fun currentDeviceLocaleTag(): String = Locale.getDefault().toLanguageTag()

    private fun normalize(rawTag: String): String? {
        val cleaned = rawTag.trim().replace('_', '-')
        if (cleaned.isBlank()) return null

        val normalized = Locale.forLanguageTag(cleaned).toLanguageTag()
        return normalized.takeUnless { it.isBlank() || it.equals("und", ignoreCase = true) }
    }
}
