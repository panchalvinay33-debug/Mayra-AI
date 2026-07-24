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
    suspend fun respond(message: String, recentMessages: List<MayraMessage> = emptyList()): String {
        val clean = message.trim()
        require(clean.isNotEmpty()) { "Message cannot be empty" }
        agendaCommand(clean)?.let { return it }
        val intent = intentEngine.parse(clean)
        actionDispatcher.dispatch(intent)?.let { return it }
        return when (intent) {
            is AssistantIntent.Invalid -> intent.reason
            AssistantIntent.ClearConversation -> "Use the Clear button to remove this conversation."
            is AssistantIntent.DeviceInfo -> when (intent.type) {
                DeviceInfoType.TIME -> currentTime()
                DeviceInfoType.BATTERY -> "Battery reading needs Android device access."
                DeviceInfoType.NOTIFICATIONS -> MayraNotificationIntelligenceRuntime.store.summary().spokenText()
            }
            is AssistantIntent.Chat -> respondToChat(intent.message, recentMessages)
            is AssistantIntent.OpenApp, is AssistantIntent.CallContact, is AssistantIntent.ComposeMessage, is AssistantIntent.CreateReminder ->
                "I understood the action, but it could not be dispatched."
        }
    }

    private fun agendaCommand(message: String): String? {
        val normalized = normalize(message)
        if (normalized.containsAny("today reminders", "todays reminders", "today agenda", "today schedule", "aaj ki reminder", "aaj ke reminder", "aaj kya hai", "aaj ka schedule", "आज की रिमाइंडर", "आज क्या है")) return MayraAgendaRuntime.todaySummary()
        if (normalized.containsAny("upcoming reminders", "upcoming agenda", "next reminders", "what is next", "agli reminder", "aage kya hai", "upcoming kya hai", "आगे क्या है")) return MayraAgendaRuntime.upcomingSummary()

        parseReminderMove(message, normalized)?.let { (query, time) -> return MayraAgendaRuntime.rescheduleReminder(query, time) }
        if (looksLikeEventCreation(normalized)) return MayraAgendaRuntime.createEvent(message)
        parseSnooze(normalized)?.let { (query, minutes) -> return MayraAgendaRuntime.snoozeReminder(query, minutes) }
        parseTargetCommand(normalized, COMPLETE_WORDS)?.let { return MayraAgendaRuntime.completeReminder(it) }
        parseTargetCommand(normalized, CANCEL_WORDS)?.let { return MayraAgendaRuntime.cancelReminder(it) }
        return null
    }

    private fun looksLikeEventCreation(text: String): Boolean =
        text.containsAny("add event", "create event", "schedule meeting", "calendar me", "calendar mein", "event add", "meeting add")

    private fun parseReminderMove(original: String, normalized: String): Pair<String, String>? {
        if (!normalized.containsAny("move reminder", "reschedule reminder", "reminder ko", "reminder at", "reminder kal", "reminder tomorrow")) return null
        val marker = Regex("(?i)\\b(?:to|at|ko|kal|tomorrow|today|aaj)\\b").findAll(normalized).lastOrNull() ?: return null
        val query = normalized.substring(0, marker.range.first)
            .replace(Regex("(?i)\\b(mayra|move|reschedule|reminder|meri|mera|the|my)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim()
        val time = original.substring(marker.range.first).trim()
        return if (query.isBlank() || time.isBlank()) null else query to time
    }

    private fun parseSnooze(text: String): Pair<String, Long>? {
        if (!text.containsAny("snooze", "baad yaad", "baad remind", "thodi der", "स्नूज़")) return null
        val minutes = Regex("\\b(\\d{1,4})\\s*(?:minute|minutes|min|mins|मिनट)\\b").find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()?.coerceIn(1, 10_080) ?: 10L
        val query = text.replace(Regex("\\b(\\d{1,4})\\s*(?:minute|minutes|min|mins|मिनट)\\b"), " ")
            .replace(Regex("(?i)\\b(snooze|for|baad|yaad|remind|karo|kar do|thodi der|स्नूज़|करो)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim()
        return query.takeIf(String::isNotBlank)?.let { it to minutes }
    }

    private fun parseTargetCommand(text: String, words: Set<String>): String? {
        val match = words.firstOrNull { Regex("(^|\\s)${Regex.escape(it)}(?=\\s|$)").containsMatchIn(text) } ?: return null
        return text.replaceFirst(Regex("(^|\\s)${Regex.escape(match)}(?=\\s|$)"), " ")
            .replace(Regex("(?i)\\b(mayra|reminder|the|my|meri|mera|ko|karo|kar do|please|mark|as)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim().takeIf(String::isNotBlank)
    }

    private fun respondToChat(message: String, recentMessages: List<MayraMessage>): String {
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            normalized.isGreeting() -> greeting()
            normalized.containsAny("who are you", "tum kaun", "aap kaun", "तुम कौन", "आप कौन") -> "I’m Mayra, your personal AI assistant. I can chat, use voice, understand your phone, remember relationships, manage reminders and your private agenda offline."
            normalized.containsAny("help", "what can you do", "kya kar sakti", "क्या कर सकती", "madad", "मदद") -> "Ask for today’s agenda, add an event, move a reminder, or create, complete, snooze and cancel reminders."
            normalized.containsAny("date", "today", "aaj", "tarikh", "आज", "तारीख") -> currentDate()
            normalized.containsAny("thank", "thanks", "dhanyavad", "shukriya", "धन्यवाद", "शुक्रिया") -> "You’re welcome 😊"
            normalized.containsAny("bye", "good night", "alvida", "बाय", "शुभ रात्रि", "अलविदा") -> "Take care. I’ll be here when you need me."
            else -> contextualFallback(message, recentMessages)
        }
    }

    private fun greeting(): String {
        val dayPart = when (timeProvider().hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Hello" }
        return "$dayPart! I’m Mayra. How can I help you?"
    }
    private fun currentTime() = "It’s ${timeProvider().format(DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH))}."
    private fun currentDate() = "Today is ${dateProvider().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH))}."
    private fun contextualFallback(message: String, recentMessages: List<MayraMessage>): String {
        val previous = recentMessages.asReversed().firstOrNull { it.sender == MayraMessage.Sender.USER && it.text != message }?.text
        return if (previous == null) "I understood: “$message”. My offline brain is active, but a full AI provider is not connected yet." else "I understood: “$message”. I’m keeping our recent conversation in context, including “$previous”."
    }
    private fun normalize(value: String) = value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun String.isGreeting() = trim() in setOf("hi", "hello", "hey", "namaste", "नमस्ते", "good morning", "good evening")
    private fun String.containsAny(vararg values: String) = values.any(::contains)
    private companion object {
        val COMPLETE_WORDS = setOf("complete", "completed", "done", "finish", "ho gaya", "पूरा", "हो गया")
        val CANCEL_WORDS = setOf("cancel", "delete", "remove", "hatao", "radd", "रद्द", "हटाओ")
    }
}