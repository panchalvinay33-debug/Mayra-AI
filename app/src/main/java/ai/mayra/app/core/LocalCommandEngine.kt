package ai.mayra.app.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Small, deterministic offline command engine used while a cloud AI provider is not configured.
 * It keeps the app useful without network access and provides a clean routing boundary for Phase 2.
 */
class LocalCommandEngine(
    private val dateProvider: () -> LocalDate = LocalDate::now,
    private val timeProvider: () -> LocalTime = LocalTime::now
) {
    fun respond(message: String, recentMessages: List<MayraMessage> = emptyList()): String {
        val clean = message.trim()
        require(clean.isNotEmpty()) { "Message cannot be empty" }

        val normalized = clean.lowercase(Locale.ROOT)
        return when {
            normalized.isGreeting() -> greeting()
            normalized.containsAny("who are you", "tum kaun", "aap kaun", "तुम कौन", "आप कौन") ->
                "I’m Mayra, your personal AI assistant. Right now I can chat, accept voice input, tell the date or time, and run a small set of offline commands."

            normalized.containsAny("help", "what can you do", "kya kar sakti", "क्या कर सकती", "madad", "मदद") ->
                "You can ask me the date or time, speak using the microphone, or chat with me. My secure online AI and phone-action skills will be connected in the next phase."

            normalized.containsAny("time", "samay", "kitne baje", "समय", "कितने बजे") -> currentTime()
            normalized.containsAny("date", "today", "aaj", "tarikh", "आज", "तारीख") -> currentDate()
            normalized.containsAny("thank", "thanks", "dhanyavad", "shukriya", "धन्यवाद", "शुक्रिया") ->
                "You’re welcome 😊"

            normalized.containsAny("bye", "good night", "alvida", "बाय", "शुभ रात्रि", "अलविदा") ->
                "Take care. I’ll be here when you need me."

            else -> contextualFallback(clean, recentMessages)
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
