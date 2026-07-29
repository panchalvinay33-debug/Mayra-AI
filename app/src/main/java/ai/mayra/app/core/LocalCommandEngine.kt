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

        val parsedIntent = intentEngine.parse(clean)
        val intent = if (parsedIntent is AssistantIntent.Chat) {
            resolveFollowUpIntent(clean, recentMessages) ?: parsedIntent
        } else {
            parsedIntent
        }
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

    /**
     * Completes narrow multi-turn commands without treating arbitrary previous chat as an action.
     * Only an immediately preceding Mayra clarification can activate this continuation.
     */
    private fun resolveFollowUpIntent(
        message: String,
        recentMessages: List<MayraMessage>
    ): AssistantIntent? {
        val previous = recentMessages
            .dropLastWhile { it.sender == MayraMessage.Sender.USER && it.text.trim() == message }
            .lastOrNull()
            ?: return null
        if (previous.sender != MayraMessage.Sender.MAYRA) return null

        val prompt = previous.text.trim().lowercase(Locale.ROOT)
        return when {
            prompt.contains("what should i remind you about") ||
                prompt.contains("kis baat ki yaad") ||
                prompt.contains("क्या याद") -> AssistantIntent.CreateReminder(message)
            else -> null
        }
    }

    private fun respondToChat(message: String, recentMessages: List<MayraMessage>): String {
        val normalized = message.lowercase(Locale.ROOT)
        return when {
            normalized.isWellbeingQuestion() ->
                "Main bilkul theek hoon 😊 Aap kaise ho? Batao, aaj main aapki kya madad karun?"

            normalized.isGreeting() -> greeting()
            normalized.containsAny("who are you", "tum kaun", "aap kaun", "तुम कौन", "आप कौन") ->
                "I’m Mayra, your private on-device assistant. I can chat offline, accept voice input, use approved personal memory, search imported documents, and safely prepare supported phone actions."

            normalized.isCapabilityQuestion() -> capabilitySummary()
            normalized.containsAny("date", "today", "aaj", "tarikh", "आज", "तारीख") -> currentDate()
            normalized.containsAny("thank", "thanks", "dhanyavad", "shukriya", "धन्यवाद", "शुक्रिया") ->
                "You’re welcome 😊"

            normalized.containsAny("bye", "good night", "alvida", "बाय", "शुभ रात्रि", "अलविदा") ->
                "Take care. I’ll be here when you need me."

            else -> contextualFallback(message, recentMessages)
        }
    }

    private fun capabilitySummary(): String =
        """
        Abhi main ye kaam kar sakti hoon:
        • Hindi/Hinglish text aur voice input samajhna.
        • Aapki approval ke baad personal baatein local memory mein save, use, edit aur delete karna.
        • TXT, PDF aur DOCX documents import karke unmein search, summary aur grounded answers dena.
        • Time/date batana aur supported phone actions—app kholna, contact call, message draft, reminder—safe confirmation ke saath prepare karna.
        • Activity History, Device readiness aur privacy controls dikhana.

        Abhi internet-based general AI aur fresh web knowledge connected nahi hai, isliye open-ended questions par meri offline intelligence limited rahegi.
        """.trimIndent()

    private fun greeting(): String {
        val hour = timeProvider().hour
        val dayPart = when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Hello"
        }
        return "$dayPart! Main Mayra hoon 😊 Batao, main aapki kya madad karun?"
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
            "I understood: “$message”. Main abhi offline mode mein hoon; general AI provider connected nahi hai. Aap voice, memory, documents, time/date ya supported device actions try kar sakte hain."
        } else {
            "I understood: “$message”. Main recent context—“$previousUserMessage”—yaad rakh rahi hoon, lekin general AI provider connected nahi hone se open-ended answer limited hai."
        }
    }

    private fun String.isGreeting(): Boolean =
        trim() in setOf(
            "hi", "hello", "hey", "namaste", "नमस्ते", "good morning", "good evening",
            "hii", "helo", "hello mayra", "hi mayra"
        )

    private fun String.isWellbeingQuestion(): Boolean =
        containsAny(
            "how are you", "how r u", "kaisi ho", "kesi ho", "kaise ho", "kese ho",
            "tum kaisi ho", "tum kesi ho", "aap kaise ho", "aap kesi hain",
            "कैसी हो", "कैसे हो", "आप कैसी हैं", "आप कैसे हैं"
        )

    private fun String.isCapabilityQuestion(): Boolean =
        containsAny(
            "help", "what can you do", "what are your capabilities", "your capabilities", "capability",
            "capabilities", "kya kar sakti", "kya kya kar sakti", "kya kya kar sakte", "tum kya kar sakti",
            "tm kya kar sakti", "tumhari capability", "tmhari capability", "aap kya kar sakte", "madad", "मदद",
            "क्या कर सकती", "क्या क्या कर सकती", "क्षमता", "खासियत"
        )

    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)
}
