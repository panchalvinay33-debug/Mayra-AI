package ai.mayra.app.context

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraAssistantResponse
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.MayraStructuredAssistant
import java.util.Locale

/**
 * Deterministic local follow-up guidance from J6 coarse context.
 *
 * Suggestions never contain source titles, message text, contact identity, reminder text,
 * personal-memory values or document content. Non-suggestion requests delegate unchanged.
 */
class MayraLocalNextSuggestionAssistant(
    private val delegate: MayraAssistant,
    private val contextSource: () -> MayraContextBundle
) : MayraStructuredAssistant {
    override suspend fun replyStructured(
        message: String,
        conversation: List<MayraMessage>
    ): Result<MayraAssistantResponse> {
        val localAnswer = runCatching {
            MayraLocalNextSuggestions.answer(message, contextSource())
        }.getOrNull()

        if (localAnswer != null) return Result.success(MayraAssistantResponse(localAnswer))

        return if (delegate is MayraStructuredAssistant) {
            delegate.replyStructured(message, conversation)
        } else {
            delegate.reply(message, conversation).map(::MayraAssistantResponse)
        }
    }
}

object MayraLocalNextSuggestions {
    fun answer(rawMessage: String, bundle: MayraContextBundle): String? {
        val message = rawMessage.trim().lowercase(Locale.ROOT)
        if (!message.isNextSuggestionQuestion() || message.looksLikeActionCommand()) return null

        val suggestions = mutableListOf<Suggestion>()

        (bundle.reminders.access as? ContextValue.Available)?.value?.let { reminders ->
            when {
                reminders.dueOrOverdueCount > 0 -> suggestions += Suggestion(
                    100,
                    "Review your due reminders first (${reminders.dueOrOverdueCount} waiting)."
                )
                reminders.minutesUntilNextReminder != null && reminders.minutesUntilNextReminder <= 60 ->
                    suggestions += Suggestion(72, "A reminder is coming up in ${reminders.minutesUntilNextReminder} min.")
            }
        }

        (bundle.calendar.access as? ContextValue.Available)?.value?.let { calendar ->
            when {
                calendar.busyNow -> suggestions += Suggestion(95, "You are busy now; avoid starting something that needs focus.")
                calendar.minutesUntilNextEvent != null && calendar.minutesUntilNextEvent <= 60 ->
                    suggestions += Suggestion(85, "Prepare for the next calendar event in ${calendar.minutesUntilNextEvent} min.")
            }
        }

        if (isFreshSnapshot(bundle.notifications.capturedAt, bundle.capturedAt, MAX_NOTIFICATION_AGE_MINUTES)) {
            (bundle.notifications.access as? ContextValue.Available)?.value?.let { notifications ->
                if (notifications.attentionCount > 0) {
                    suggestions += Suggestion(
                        75,
                        "Check ${notifications.attentionCount} notification${if (notifications.attentionCount == 1) "" else "s"} that may need attention."
                    )
                }
            }
        }

        (bundle.device.power as? ContextValue.Available)?.value?.let { power ->
            val percent = power.batteryPercent
            if (!power.isCharging && percent != null && percent <= 20) {
                suggestions += Suggestion(70, "Charge the phone soon; battery is at $percent%.")
            }
        }

        (bundle.device.connectivity as? ContextValue.Available)?.value?.let { connectivity ->
            if (connectivity == ConnectivityState.OFFLINE) {
                suggestions += Suggestion(60, "Stay with Mayra's local features until connectivity returns.")
            }
        }

        (bundle.knowledge.documents as? ContextValue.Available)?.value?.let { documents ->
            if (documents.needsAttentionCount > 0) {
                suggestions += Suggestion(
                    45,
                    "Refresh the Document Library when convenient (${documents.needsAttentionCount} need indexing or refresh)."
                )
            }
        }

        val ranked = suggestions
            .sortedByDescending(Suggestion::priority)
            .take(MAX_SUGGESTIONS)

        return if (ranked.isEmpty()) {
            "Nothing urgent stands out in Mayra's coarse local context. You can continue with your current task."
        } else {
            buildString {
                append("Next suggestions:\n")
                ranked.forEach { append("• ").append(it.text).append('\n') }
                append("Based only on coarse local context; private source text is not included.")
            }
        }
    }

    private fun String.isNextSuggestionQuestion(): Boolean = containsAny(
        "what next", "what should i do", "what do i do next", "what should i do now",
        "next kya", "ab kya karu", "ab kya karun", "ab kya karna", "kya karu ab",
        "mujhe ab kya karna chahiye"
    )

    private fun String.looksLikeActionCommand(): Boolean = ACTION_MARKERS.any(::contains)
    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)

    private data class Suggestion(val priority: Int, val text: String)

    private val ACTION_MARKERS = listOf(
        "set reminder", "reminder set", "reminder laga", "remind me", "yaad dilana",
        "call ", "phone ", "dial ", "message ", "send message", "send sms", "sms ",
        "open ", "launch ", "kholo", "khol ", "chalao"
    )

    private const val MAX_SUGGESTIONS = 3
    private const val MAX_NOTIFICATION_AGE_MINUTES = 120L
}
