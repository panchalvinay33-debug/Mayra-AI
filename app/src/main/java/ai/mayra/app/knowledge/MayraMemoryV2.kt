package ai.mayra.app.knowledge

import android.content.Context

object MayraMemoryPrivacyGuard {
    private val blockedSignals = listOf(
        Regex("\\b(?:otp|cvv|pin|password|passcode)\\b", RegexOption.IGNORE_CASE),
        Regex("\\b\\d{12,19}\\b"),
        Regex("\\b(?:api[_ -]?key|secret[_ -]?key|private[_ -]?key)\\b", RegexOption.IGNORE_CASE)
    )

    fun looksSensitive(text: String): Boolean = blockedSignals.any { it.containsMatchIn(text) }
}

class MayraMemoryRecall(context: Context) {
    private val memory = MayraPersonalMemory(context)

    fun promptContext(query: String, maxItems: Int = 8, maxCharacters: Int = 1800): String {
        val hits = memory.search(query, includeSensitive = false, limit = maxItems)
        return hits.joinToString("\n") { "- ${it.title}: ${it.preview}" }.take(maxCharacters)
    }

    fun saveConfirmedNote(title: String, body: String, tags: Set<String> = emptySet()): PersonalNote {
        require(title.isNotBlank())
        require(!MayraMemoryPrivacyGuard.looksSensitive("$title $body")) {
            "Passwords, OTPs, card-like numbers and secret keys cannot be saved in normal Mayra memory."
        }
        return memory.saveNote(PersonalNote(title = title.trim(), body = body.trim(), tags = tags, sensitive = false))
    }
}
