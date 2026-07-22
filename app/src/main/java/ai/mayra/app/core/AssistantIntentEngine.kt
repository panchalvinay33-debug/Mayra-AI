package ai.mayra.app.core

import java.util.Locale

/**
 * Lightweight, deterministic intent parser used before a remote AI provider is available.
 *
 * It turns common user phrases into structured actions while keeping unknown requests on
 * the normal chat path. No network access or Android framework dependency is required.
 */
class AssistantIntentEngine(
    private val locale: Locale = Locale.getDefault()
) {

    fun parse(rawInput: String): AssistantIntent {
        val original = rawInput.trim()
        if (original.isEmpty()) return AssistantIntent.Invalid("Please say or type a command.")

        val normalized = original
            .lowercase(locale)
            .replace(Regex("\\s+"), " ")
            .trim()

        return when {
            normalized.matchesAny("clear chat", "clear conversation", "delete chat") ->
                AssistantIntent.ClearConversation

            normalized.matchesAny("what time", "current time", "time kya", "samay kya") ->
                AssistantIntent.DeviceInfo(DeviceInfoType.TIME)

            normalized.matchesAny("battery", "charge kitna", "battery kitni") ->
                AssistantIntent.DeviceInfo(DeviceInfoType.BATTERY)

            normalized.startsWithAny("open ", "launch ", "khol ") -> {
                val appName = normalized.substringAfterFirstPrefix("open ", "launch ", "khol ")
                if (appName.isBlank()) AssistantIntent.Invalid("Which app should I open?")
                else AssistantIntent.OpenApp(appName)
            }

            normalized.startsWithAny("call ", "phone ", "dial ") -> {
                val contact = normalized.substringAfterFirstPrefix("call ", "phone ", "dial ")
                if (contact.isBlank()) AssistantIntent.Invalid("Who should I call?")
                else AssistantIntent.CallContact(contact)
            }

            normalized.startsWithAny("message ", "text ", "send message to ") ->
                parseMessageIntent(original, normalized)

            normalized.startsWithAny("remind me ", "set reminder ", "yaad dilana ") ->
                AssistantIntent.CreateReminder(
                    request = original.substringAfterFirstPrefixIgnoreCase(
                        "remind me ", "set reminder ", "yaad dilana "
                    ).trim()
                ).takeUnless { it.request.isBlank() }
                    ?: AssistantIntent.Invalid("What should I remind you about?")

            else -> AssistantIntent.Chat(original)
        }
    }

    private fun parseMessageIntent(original: String, normalized: String): AssistantIntent {
        val payload = original.substringAfterFirstPrefixIgnoreCase(
            "send message to ", "message ", "text "
        ).trim()

        if (payload.isBlank()) return AssistantIntent.Invalid("Who should I message?")

        val separator = listOf(":", " saying ", " that ", " bolo ")
            .firstOrNull { normalized.contains(it) }

        if (separator == null) {
            return AssistantIntent.ComposeMessage(recipient = payload, message = null)
        }

        val index = payload.lowercase(locale).indexOf(separator.trim())
        if (index <= 0) return AssistantIntent.ComposeMessage(recipient = payload, message = null)

        val recipient = payload.substring(0, index).trim().trimEnd(':')
        val messageStart = index + separator.trim().length
        val message = payload.substring(messageStart).trim().trimStart(':').trim()

        return AssistantIntent.ComposeMessage(
            recipient = recipient,
            message = message.ifBlank { null }
        )
    }
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

private fun String.matchesAny(vararg values: String): Boolean =
    values.any { contains(it) }

private fun String.startsWithAny(vararg prefixes: String): Boolean =
    prefixes.any(::startsWith)

private fun String.substringAfterFirstPrefix(vararg prefixes: String): String {
    val prefix = prefixes.firstOrNull(::startsWith) ?: return this
    return removePrefix(prefix).trim()
}

private fun String.substringAfterFirstPrefixIgnoreCase(vararg prefixes: String): String {
    val prefix = prefixes.firstOrNull { startsWith(it, ignoreCase = true) } ?: return this
    return substring(prefix.length)
}
