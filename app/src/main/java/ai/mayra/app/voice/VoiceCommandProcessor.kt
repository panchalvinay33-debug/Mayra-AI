package ai.mayra.app.voice

/**
 * Normalizes raw speech-recognition text before it enters the AI pipeline.
 * This class is Android-independent so it can be unit-tested without a device.
 */
class VoiceCommandProcessor(
    private val wakeWords: Set<String> = setOf("mayra", "hey mayra", "ok mayra")
) {
    fun process(rawText: String): VoiceCommandResult {
        val normalized = rawText
            .trim()
            .replace(Regex("\\s+"), " ")

        if (normalized.isBlank()) {
            return VoiceCommandResult.Rejected("No speech was detected.")
        }

        val command = removeWakeWord(normalized).trim()
        if (command.isBlank()) {
            return VoiceCommandResult.Rejected("Please say a command after the wake word.")
        }

        return VoiceCommandResult.Accepted(
            originalText = rawText,
            command = command,
            usedWakeWord = command.length != normalized.length
        )
    }

    private fun removeWakeWord(text: String): String {
        val lower = text.lowercase()
        val wakeWord = wakeWords
            .sortedByDescending { it.length }
            .firstOrNull { candidate ->
                lower == candidate || lower.startsWith("$candidate ")
            }
            ?: return text

        return text.drop(wakeWord.length)
    }
}

sealed interface VoiceCommandResult {
    data class Accepted(
        val originalText: String,
        val command: String,
        val usedWakeWord: Boolean
    ) : VoiceCommandResult

    data class Rejected(val reason: String) : VoiceCommandResult
}
