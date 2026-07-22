package ai.mayra.app.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Deterministic offline command engine used while a cloud AI provider is not configured.
 *
 * Common commands are parsed into structured intents. Device commands are delegated to
 * [ActionDispatcher], while conversational requests remain local and network-free.
 */
class LocalCommandEngine(
    private val intentEngine: AssistantIntentEngine = AssistantIntentEngine(),
    private val actionDispatcher: ActionDispatcher = ActionDispatcher(),
    private val dateProvider: () -> LocalDate = LocalDate::now,
    private val timeProvider: () -> LocalTime = LocalTime::now
) {
    suspend fun respond(
        message: String,
        recentMessages: List<MayraMessage> = emptyList()
    ): String {
        val clean = message.trim()
        require(clean.isNotEmpty()) { "Message cannot be empty" }

        val intent = intentEngine.parse(clean)
        actionDispatcher.dispatch(intent)?.let { return it }

        return when (intent) {
            is AssistantIntent.Invalid -> intent.reason
            AssistantIntent.ClearConversation ->
                "Use the Clear button to remove this conversation. Voice-controlled clearing will be connected with the app action layer."

            is AssistantIntent.DeviceInfo -> when (intent.type) {
                DeviceInfoType.TIME -> currentTime()
                DeviceInfoType.BATTERY ->
                    "Battery reading needs Android device access. The command is understood and ready for the device-action layer."
            }

            is AssistantIntent.Chat -> respondToChat(intent.message, recentMessages)

            // These intents are handled by ActionDispatcher before this branch.
            is AssistantIntent.OpenApp,
            is AssistantIntent.CallContact,
            is AssistantIntent.ComposeMessage,
            is AssistantIntent.CreateReminder ->
                "I understood the action, but it could not be dispatched."
        }
    }

    private fun respondToChat(message: String, recentMessages: List<MayraMessage>): String {
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            normalized.isGreeting() -> greeting()
            normalized.containsAny("who are you", "tum kaun", "aap kaun", "तुम कौन", "आप कौन") ->
                "I’m Mayra, your personal AI assistant. I can already chat, accept voice input, remember the current conversation, understand common phone commands, and work offline."

            normalized.containsAny("help", "what can you do", "kya kar sakti", "क्या कर सकती", "madad", "मदद") ->
                "You can ask for the date or time, use voice input, or say commands such as open an app, call someone, send a message, create a reminder, check battery, or clear chat. Device actions are being connected safely with confirmation."

            normalized.containsAny("date", "today", "aaj", "tarikh", "आज", "तारीख") -> currentDate()
            normalized.containsAny("thank", "thanks", "dhanyavad", "shukriya", "धन्यवाद", "शुक्रिया") ->
                "You’re welcome 😊"

            normalized.containsAny("bye", "good night", "alvida", "बाय", "शुभ रात्रि", "अलविदा") ->
                "Take care. I’ll be here when you need me."

            else -> contextualFallback(message, recentMessages)
        }
    }

    private fun greeting(): String {
        val hour = timeProvider().hour
        val dayPart = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello"
        }
        return "$dayPart! I’m Mayra. How can I help you?"
    }

    private fun currentTime(): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        return "It’s ${timeProvider().format(formatter)}."
    }

    private fun currentDate(): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
        return "Today is ${dateProvider().format(formatter)}."
    }

    private fun contextualFallback(message: String, recentMessages: List<MayraMessage>): String {
        val previousUserMessage = recentMessages
            .asReversed()
            .firstOrNull { it.sender == MayraMessage.Sender.USER && it.text != message }
            ?.text

        return if (previousUserMessage == null) {
            "I understood: “$message”. My offline brain is active, but a full AI provider is not connected yet."
        } else {
            "I understood: “$message”. I’m keeping our recent conversation in context, including “$previousUserMessage”, while the full AI provider is being connected."
        }
    }

    private fun String.isGreeting(): Boolean =
        trim() in setOf("hi", "hello", "hey", "namaste", "नमस्ते", "good morning", "good evening")

    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)
}
