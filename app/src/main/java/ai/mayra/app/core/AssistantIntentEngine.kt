package ai.mayra.app.core

import java.util.Locale

/** Offline deterministic parser for common English, Hindi and Hinglish assistant commands. */
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
            normalized.findCommand(MESSAGE_COMMANDS) != null -> parseMessageIntent(original, normalized)
            normalized.findCommand(CALL_COMMANDS) != null -> parseTargetIntent(
                normalized,
                CALL_COMMANDS,
                "Who should I call?",
                AssistantIntent::CallContact
            )
            normalized.findCommand(OPEN_COMMANDS) != null -> parseTargetIntent(
                normalized,
                OPEN_COMMANDS,
                "Which app should I open?",
                AssistantIntent::OpenApp
            )
            REMINDER_COMMANDS.any(normalized::contains) -> parseReminderIntent(original, normalized)
            else -> AssistantIntent.Chat(original)
        }
    }

    private fun parseTargetIntent(
        normalized: String,
        commands: List<String>,
        emptyMessage: String,
        factory: (String) -> AssistantIntent
    ): AssistantIntent {
        val match = normalized.findCommand(commands) ?: return AssistantIntent.Invalid(emptyMessage)
        val after = normalized.substring(match.index + match.keyword.length)
            .cleanCommandSide()
        val before = normalized.substring(0, match.index)
            .cleanCommandSide()

        val target = when {
            after.isUsefulTarget() -> after
            before.isUsefulTarget() -> before
            else -> ""
        }
        return if (target.isBlank()) AssistantIntent.Invalid(emptyMessage) else factory(target)
    }

    private fun parseMessageIntent(original: String, normalized: String): AssistantIntent {
        val match = normalized.findCommand(MESSAGE_COMMANDS)
            ?: return AssistantIntent.Invalid("Who should I message?")

        val before = normalized.substring(0, match.index).cleanCommandSide()
        val afterNormalized = normalized.substring(match.index + match.keyword.length)
            .trim().removeLeadingWords(MESSAGE_ACTION_WORDS)
        val afterOriginal = original.substringAfterKeyword(match.keyword)
            .trim().removeLeadingWordsIgnoreCase(MESSAGE_ACTION_WORDS)

        // Natural Hindi order: "Mummy ko message likho main aa raha hu".
        if (before.isUsefulTarget()) {
            val body = afterOriginal.trim().trimStart(':').trim().ifBlank { null }
            return AssistantIntent.ComposeMessage(before, body)
        }

        if (afterNormalized.isBlank()) return AssistantIntent.Invalid("Who should I message?")
        val separator = MESSAGE_SEPARATORS
            .mapNotNull { token -> afterNormalized.indexOf(token).takeIf { it > 0 }?.let { token to it } }
            .minByOrNull { it.second }

        if (separator == null) {
            return AssistantIntent.ComposeMessage(afterOriginal.cleanTarget(), null)
        }

        val recipient = afterOriginal.substring(0, separator.second).cleanTarget()
        val bodyStart = separator.second + separator.first.length
        val body = afterOriginal.substring(bodyStart.coerceAtMost(afterOriginal.length))
            .trim().trimStart(':').trim().ifBlank { null }
        return if (recipient.isBlank()) {
            AssistantIntent.Invalid("Who should I message?")
        } else {
            AssistantIntent.ComposeMessage(recipient, body)
        }
    }

    private fun parseReminderIntent(original: String, normalized: String): AssistantIntent {
        val match = REMINDER_COMMANDS
            .mapNotNull { keyword -> normalized.indexOf(keyword).takeIf { it >= 0 }?.let { CommandMatch(keyword, it) } }
            .minByOrNull(CommandMatch::index)
            ?: return AssistantIntent.Invalid("What should I remind you about?")
        val request = original.substringAfterKeyword(match.keyword)
            .trim().removeLeadingWordsIgnoreCase(REMINDER_FILLER_WORDS)
        return if (request.isBlank()) {
            AssistantIntent.Invalid("What should I remind you about?")
        } else {
            AssistantIntent.CreateReminder(request)
        }
    }

    private fun normalize(value: String): String = value
        .lowercase(locale)
        .replace(Regex("[^\\p{L}\\p{N}:+ ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun String.findCommand(commands: List<String>): CommandMatch? = commands
        .mapNotNull { keyword -> indexOfWord(keyword).takeIf { it >= 0 }?.let { CommandMatch(keyword, it) } }
        .sortedWith(compareBy<CommandMatch> { it.index }.thenByDescending { it.keyword.length })
        .firstOrNull()

    private fun String.indexOfWord(word: String): Int {
        val result = Regex("(^|\\s)${Regex.escape(word)}(?=\\s|$)").find(this) ?: return -1
        return result.range.first + if (result.value.startsWith(' ')) 1 else 0
    }

    private fun String.cleanCommandSide(): String = trim()
        .removeLeadingWords(COMMAND_FILLER_WORDS)
        .removeTrailingWords(COMMAND_TRAILING_WORDS)
        .cleanTarget()

    private fun String.isUsefulTarget(): Boolean =
        isNotBlank() && split(' ').any { it !in COMMAND_FILLER_WORDS && it !in COMMAND_TRAILING_WORDS }

    private fun String.removeLeadingWords(words: Set<String>): String {
        val tokens = split(Regex("\\s+")).filter(String::isNotBlank).toMutableList()
        while (tokens.firstOrNull() in words) tokens.removeAt(0)
        return tokens.joinToString(" ")
    }

    private fun String.removeTrailingWords(words: Set<String>): String {
        val tokens = split(Regex("\\s+")).filter(String::isNotBlank).toMutableList()
        while (tokens.lastOrNull() in words) tokens.removeAt(tokens.lastIndex)
        return tokens.joinToString(" ")
    }

    private fun String.removeLeadingWordsIgnoreCase(words: Set<String>): String {
        val tokens = split(Regex("\\s+")).filter(String::isNotBlank).toMutableList()
        while (tokens.firstOrNull()?.lowercase(locale) in words) tokens.removeAt(0)
        return tokens.joinToString(" ")
    }

    private fun String.substringAfterKeyword(keyword: String): String {
        val index = lowercase(locale).indexOf(keyword)
        return if (index < 0) this else substring(index + keyword.length)
    }

    private fun String.cleanTarget(): String = trim()
        .trim(':', ',', '.', '?', '!')
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

enum class DeviceInfoType { TIME, BATTERY }

private data class CommandMatch(val keyword: String, val index: Int)
private fun String.matchesAny(vararg values: String): Boolean = values.any(::contains)

private val OPEN_COMMANDS = listOf("open", "launch", "khol", "kholo", "chalao", "start")
private val CALL_COMMANDS = listOf("call", "phone", "dial", "lagao", "milao")
private val MESSAGE_COMMANDS = listOf("send message to", "send sms to", "message", "text", "sms", "msg")
private val REMINDER_COMMANDS = listOf("remind me", "set reminder", "yaad dilana", "reminder laga")

private val COMMAND_FILLER_WORDS = setOf(
    "mayra", "mira", "please", "jara", "zara", "mera", "meri", "mere", "the",
    "app", "application", "contact", "number", "to"
)
private val COMMAND_TRAILING_WORDS = setOf(
    "ko", "please", "karo", "kar", "karna", "do", "de", "abhi", "jara", "zara",
    "open", "call", "phone", "dial", "launch", "start", "khol", "kholo", "chalao"
)
private val MESSAGE_ACTION_WORDS = setOf("to", "ko", "likho", "bolo", "saying", "that")
private val REMINDER_FILLER_WORDS = setOf("to", "ki", "please", "mujhe")
private val MESSAGE_SEPARATORS = listOf(":", " saying ", " that ", " bolo ", " likho ")