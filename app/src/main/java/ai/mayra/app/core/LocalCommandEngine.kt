package ai.mayra.app.core

import ai.mayra.app.background.MayraNotificationIntelligenceRuntime
import ai.mayra.app.calendar.MayraAgendaRuntime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Deterministic offline command engine used while a cloud AI provider is not configured. */
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

        agendaCommand(clean)?.let { return it }

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
                DeviceInfoType.NOTIFICATIONS ->
                    MayraNotificationIntelligenceRuntime.store.summary().spokenText()
            }

            is AssistantIntent.Chat -> respondToChat(intent.message, recentMessages)
            is AssistantIntent.OpenApp,
            is AssistantIntent.CallContact,
            is AssistantIntent.ComposeMessage,
            is AssistantIntent.CreateReminder ->
                "I understood the action, but it could not be dispatched."
        }
    }

    private fun agendaCommand(message: String): String? {
        val normalized = message.lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.containsAny(
                "today reminders", "todays reminders", "today agenda", "today schedule",
                "aaj ki reminder", "aaj ke reminder", "aaj kya hai", "aaj ka schedule",
                "आज की रिमाइंडर", "आज क्या है"
            )
        ) return MayraAgendaRuntime.todaySummary()

        if (normalized.containsAny(
                "upcoming reminders", "upcoming agenda", "next reminders", "what is next",
                "agli reminder", "aage kya hai", "upcoming kya hai", "आगे क्या है"
            )
        ) return MayraAgendaRuntime.upcomingSummary()

        parseSnooze(normalized)?.let { (query, minutes) ->
            return MayraAgendaRuntime.snoozeReminder(query, minutes)
        }

        parseTargetCommand(normalized, COMPLETE_WORDS)?.let { query ->
            return MayraAgendaRuntime.completeReminder(query)
        }

        parseTargetCommand(normalized, CANCEL_WORDS)?.let { query ->
            return MayraAgendaRuntime.cancelReminder(query)
        }

        return null
    }

    private fun parseSnooze(text: String): Pair<String, Long>? {
        if (!text.containsAny("snooze", "baad yaad", "baad remind", "thodi der", "स्नूज़")) return null
        val minutes = Regex("\\b(\\d{1,4})\\s*(?:minute|minutes|min|mins|मिनट)\\b")
            .find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceIn(1, 10_080) ?: 10L
        val query = text
            .replace(Regex("\\b(\\d{1,4})\\s*(?:minute|minutes|min|mins|मिनट)\\b"), " ")
            .replace(Regex("(?i)\\b(snooze|for|baad|yaad|remind|karo|kar do|thodi der|स्नूज़|करो)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim()
        return query.takeIf(String::isNotBlank)?.let { it to minutes }
    }

    private fun parseTargetCommand(text: String, words: Set<String>): String? {
        val match = words.firstOrNull { Regex("(^|\\s)${Regex.escape(it)}(?=\\s|$)").containsMatchIn(text) }
            ?: return null
        return text.replaceFirst(Regex("(^|\\s)${Regex.escape(match)}(?=\\s|$)"), " ")
            .replace(Regex("(?i)\\b(mayra|reminder|the|my|meri|mera|ko|karo|kar do|please|mark|as)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim().takeIf(String::isNotBlank)
    }

    private fun respondToChat(message: String, recentMessages: List<MayraMessage>): String {
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            normalized.isGreeting() -> greeting()
            normalized.containsAny("who are you", "tum kaun", "aap kaun", "तुम कौन", "आप कौन") ->
                "I’m Mayra, your personal AI assistant. I can chat, use voice, understand your phone, remember relationships, manage reminders and work offline."

            normalized.containsAny("help", "what can you do", "kya kar sakti", "क्या कर सकती", "madad", "मदद") ->
                "You can ask for today’s agenda, upcoming reminders, notification summaries, or say commands such as open an app, call someone, prepare a message, create, complete, snooze or cancel a reminder."

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

    private fun currentTime(): String =
        "It’s ${timeProvider().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))}."

    private fun currentDate(): String =
        "Today is ${dateProvider().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))}."

    private fun contextualFallback(message: String, recentMessages: List<MayraMessage>): String {
        val previousUserMessage = recentMessages.asReversed()
            .firstOrNull { it.sender == MayraMessage.Sender.USER && it.text != message }?.text
        return if (previousUserMessage == null) {
            "I understood: “$message”. My offline brain is active, but a full AI provider is not connected yet."
        } else {
            "I understood: “$message”. I’m keeping our recent conversation in context, including “$previousUserMessage”, while the full AI provider is being connected."
        }
    }

    private fun String.isGreeting(): Boolean =
        trim() in setOf("hi", "hello", "hey", "namaste", "नमस्ते", "good morning", "good evening")

    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)

    private companion object {
        val COMPLETE_WORDS = setOf("complete", "completed", "done", "finish", "ho gaya", "पूरा", "हो गया")
        val CANCEL_WORDS = setOf("cancel", "delete", "remove", "hatao", "radd", "रद्द", "हटाओ")
    }
}