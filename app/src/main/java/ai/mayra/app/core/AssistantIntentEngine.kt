package ai.mayra.app.core

import java.util.Locale

/**
 * Deterministic offline intent parser used before a remote AI provider is configured.
 *
 * The parser intentionally accepts natural Hindi/Hinglish sentences instead of requiring every
 * device command to begin with one exact English keyword.
 */
class AssistantIntentEngine(
    private val locale: Locale = Locale.getDefault()
) {

    fun parse(rawInput: String): AssistantIntent {
        val original = rawInput.trim()
        if (original.isEmpty()) return AssistantIntent.Invalid("Please say or type a command.")

        val normalized = normalize(original)

        return when {
            normalized.matchesAny("clear chat", "clear conversation", "delete chat", "chat clear") ->
                AssistantIntent.ClearConversation

            normalized.matchesAny("what time", "current time", "time kya", "samay kya", "kitne baje") ->
                AssistantIntent.DeviceInfo(DeviceInfoType.TIME)

            normalized.matchesAny("battery", "charge kitna", "battery kitni", "charge kitni") ->
                AssistantIntent.DeviceInfo(DeviceInfoType.BATTERY)

            normalized.hasMessageCommand() -> parseMessageIntent(original, normalized)
            normalized.hasCallCommand() -> parseCallIntent(normalized)
            normalized.hasOpenCommand() -> parseOpenIntent(normalized)
            normalized.hasReminderCommand() -> parseReminderIntent(original, normalized)
            else -> AssistantIntent.Chat(original)
        }
    }

    private fun parseOpenIntent(normalized: String): AssistantIntent {
        val appName = extractTargetAfterKeyword(
            normalized,
            keywords = listOf("open", "launch", "khol", "kholo", "chalao", "start"),
            removableWords = OPEN_FILLER_WORDS
        )
        return if (appName.isBlank()) {
            AssistantIntent.Invalid("Which app should I open?")
        } else {
            AssistantIntent.OpenApp(appName)
        }
    }

    private fun parseCallIntent(normalized: String): AssistantIntent {
        val contact = extractTargetAfterKeyword(
            normalized,
            keywords = listOf("call", "phone", "dial", "lagao", "milao"),
            removableWords = CALL_FILLER_WORDS
        )
        return if (contact.isBlank()) {
            AssistantIntent.Invalid("Who should I call?")
        } else {
            AssistantIntent.CallContact(contact)
        }
    }

    private fun parseMessageIntent(original: String, normalized: String): AssistantIntent {
        val command = MESSAGE_COMMANDS
            .mapNotNull { keyword -> normalized.indexOfWord(keyword).takeIf { it >= 0 }?.let { keyword to it } }
            .minByOrNull { it.second }
            ?: return AssistantIntent.Invalid("Who should I message?")

        val normalizedPayload = normalized.substring(command.second + command.first.length).trim()
            .removeLeadingWords(MESSAGE_FILLER_WORDS)
        if (normalizedPayload.isBlank()) return AssistantIntent.Invalid("Who should I message?")

        val originalStart = findOriginalPayloadStart(original, command.first)
        val originalPayload = original.substring(originalStart).trim()
            .removeLeadingWordsIgnoreCase(MESSAGE_FILLER_WORDS)

        val separator = MESSAGE_SEPARATORS
            .mapNotNull { value -> normalizedPayload.indexOf(value).takeIf { it > 0 }?.let { value to it } }
            .minByOrNull { it.second }

        if (separator == null) {
            return AssistantIntent.ComposeMessage(recipient = cleanTarget(originalPayload), message = null)
        }

        val recipient = cleanTarget(originalPayload.substring(0, separator.second))
        val bodyStart = separator.second + separator.first.length
        val message = originalPayload.substring(bodyStart.coerceAtMost(originalPayload.length))
            .trim().trimStart(':').trim()

        return if (recipient.isBlank()) {
            AssistantIntent.Invalid("Who should I message?")
        } else {
            AssistantIntent.ComposeMessage(recipient, message.ifBlank { null })
        }
    }

    private fun parseReminderIntent(original: String, normalized: String): AssistantIntent {
        val keyword = REMINDER_COMMANDS
            .mapNotNull { value -> normalized.indexOf(value).takeIf { it >= 0 }?.let { value to it } }
            .minByOrNull { it.second }
            ?: return AssistantIntent.Invalid("What should I remind you about?")

        val start = findOriginalPayloadStart(original, keyword.first)
        val request = original.substring(start).trim()
            .removeLeadingWordsIgnoreCase(REMINDER_FILLER_WORDS)

        return if (request.isBlank()) {
            AssistantIntent.Invalid("What should I remind you about?")
        } else {
            AssistantIntent.CreateReminder(request)
        }
    }

    private fun extractTargetAfterKeyword(
        normalized: String,
        keywords: List<String>,
        removableWords: Set<String>
    ): String {
        val match = keywords
            .mapNotNull { keyword -> normalized.indexOfWord(keyword).takeIf { it >= 0 }?.let { keyword to it } }
            .minByOrNull { it.second }
            ?: return ""

        return normalized.substring(match.second + match.first.length)
            .trim()
            .removeLeadingWords(removableWords)
            .removeTrailingWords(COMMON_TRAILING_WORDS)
            .let(::cleanTarget)
    }

    private fun findOriginalPayloadStart(original: String, keyword: String): Int {
        val index = original.lowercase(locale).indexOf(keyword)
        return if (index < 0) 0 else index + keyword.length
    }

    private fun normalize(value: String): String = value
        .lowercase(locale)
        .replace(Regex("[^\\p{L}\\p{N}:+ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.hasOpenCommand(): Boolean = OPEN_COMMANDS.any { indexOfWord(it) >= 0 }
    private fun String.hasCallCommand(): Boolean = CALL_COMMANDS.any { indexOfWord(it) >= 0 }
    private fun String.hasMessageCommand(): Boolean = MESSAGE_COMMANDS.any { indexOfWord(it) >= 0 }
    private fun String.hasReminderCommand(): Boolean = REMINDER_COMMANDS.any { contains(it) }

    private fun String.indexOfWord(word: String): Int {
        val pattern = Regex("(^|\\s)${Regex.escape(word)}(?=\\s|$)")
        val result = pattern.find(this) ?: return -1
        return result.range.first + if (result.value.startsWith(" ")) 1 else 0
    }

    private fun String.removeLeadingWords(words: Set<String>): String {
        val tokens = split(' ').filter(String::isNotBlank).toMutableList()
        while (tokens.firstOrNull() in words) tokens.removeAt(0)
        return tokens.joinToString(" ")
    }

    private fun String.removeTrailingWords(words: Set<String>): String {
        val tokens = split(' ').filter(String::isNotBlank).toMutableList()
        while (tokens.lastOrNull() in words) tokens.removeAt(tokens.lastIndex)
        return tokens.joinToString(" ")
    }

    private fun String.removeLeadingWordsIgnoreCase(words: Set<String>): String {
        val tokens = trim().split(Regex("\\s+")).toMutableList()
        while (tokens.firstOrNull()?.lowercase(locale) in words) tokens.removeAt(0)
        return tokens.joinToString(" ")
    }

    private fun cleanTarget(value: String): String = value
        .trim().trim(':', ',', '.', '?', '!')
        .replace(Regex("\\s+"), " ")
}

sealed interface AssistantIntent {
    data class Chat(val message: String) : AssistantIntent
    data class OpenApp(val appName: String) : AssistantIntent
    data class CallContact(val contact: String) : AssistantIntent
    data class ComposeMessage(val recipient: String, val message: String?) : AssistantIntent
    data class CreateReminder(val request: String) : AssistantIntent
    data class DeviceInfo(val type: DeviceInfoType) : AssistantIntent
    data class Invalid(val reason: String) : AssistantIntent
    data object ClearConversation : AssistantIntent
}

enum class DeviceInfoType {
    TIME,
    BATTERY
}

private fun String.matchesAny(vararg values: String): Boolean = values.any(::contains)

private val OPEN_COMMANDS = listOf("open", "launch", "khol", "kholo", "chalao", "start")
private val CALL_COMMANDS = listOf("call", "phone", "dial", "lagao", "milao")
private val MESSAGE_COMMANDS = listOf("send message to", "message", "text", "sms", "msg")
private val REMINDER_COMMANDS = listOf("remind me", "set reminder", "yaad dilana", "reminder laga")

private val OPEN_FILLER_WORDS = setOf(
    "app", "application", "ko", "please", "jara", "zara", "mera", "meri", "the"
)
private val CALL_FILLER_WORDS = setOf(
    "to", "ko", "contact", "number", "please", "jara", "zara", "mera", "meri"
)
private val MESSAGE_FILLER_WORDS = setOf(
    "to", "ko", "contact", "number", "please", "jara", "zara"
)
private val REMINDER_FILLER_WORDS = setOf("to", "ki", "please", "mujhe")
private val COMMON_TRAILING_WORDS = setOf(
    "please", "karo", "kar", "karna", "do", "de", "abhi", "jara", "zara"
)
private val MESSAGE_SEPARATORS = listOf(":", " saying ", " that ", " bolo ", " likho ", " message ")