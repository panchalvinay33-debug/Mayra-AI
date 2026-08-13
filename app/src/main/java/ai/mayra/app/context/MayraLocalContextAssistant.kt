package ai.mayra.app.context

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraAssistantResponse
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.MayraStructuredAssistant
import java.util.Locale

/**
 * Answers explicit context/status questions from J6's already-normalized local Context Fabric.
 *
 * This layer never receives raw calendar text, notification text, contact identity, reminder text,
 * memory values, or document content. Action-like commands are deliberately delegated unchanged.
 */
class MayraLocalContextAssistant(
    private val delegate: MayraAssistant,
    private val contextSource: () -> MayraContextBundle
) : MayraStructuredAssistant {
    override suspend fun replyStructured(
        message: String,
        conversation: List<MayraMessage>
    ): Result<MayraAssistantResponse> {
        val localAnswer = runCatching {
            MayraLocalContextAnswers.answer(message, contextSource())
        }.getOrNull()

        if (localAnswer != null) return Result.success(MayraAssistantResponse(localAnswer))

        return if (delegate is MayraStructuredAssistant) {
            delegate.replyStructured(message, conversation)
        } else {
            delegate.reply(message, conversation).map(::MayraAssistantResponse)
        }
    }
}

/** Pure deterministic formatter kept separate so privacy/routing behavior is unit-testable. */
object MayraLocalContextAnswers {
    fun answer(rawMessage: String, bundle: MayraContextBundle): String? {
        val message = rawMessage.trim().lowercase(Locale.ROOT)
        if (message.isBlank() || message.looksLikeActionCommand()) return null

        return when {
            message.isDailyBriefQuestion() -> dailyBrief(bundle)
            message.isCombinedContextQuestion() -> combined(bundle)
            message.isMemoryStatusQuestion() -> memory(bundle.knowledge)
            message.isLibraryStatusQuestion() -> library(bundle.knowledge)
            message.isAgendaStatusQuestion() -> agenda(bundle)
            message.isNotificationStatusQuestion() -> notifications(bundle.notifications)
            message.isPeopleStatusQuestion() -> people(bundle.contacts)
            message.isPowerStatusQuestion() -> power(bundle.device)
            message.isConnectivityStatusQuestion() -> connectivity(bundle.device)
            else -> null
        }
    }

    private fun dailyBrief(bundle: MayraContextBundle): String {
        val items = mutableListOf<BriefItem>()

        (bundle.reminders.access as? ContextValue.Available)?.value?.let { reminders ->
            when {
                reminders.dueOrOverdueCount > 0 -> items += BriefItem(
                    priority = 100,
                    text = "${reminders.dueOrOverdueCount} reminder${if (reminders.dueOrOverdueCount == 1) " is" else "s are"} due or overdue."
                )
                reminders.minutesUntilNextReminder != null && reminders.minutesUntilNextReminder <= 60 ->
                    items += BriefItem(60, "Next reminder is in ${reminders.minutesUntilNextReminder} min.")
            }
        }

        (bundle.calendar.access as? ContextValue.Available)?.value?.let { calendar ->
            when {
                calendar.busyNow -> items += BriefItem(95, "Calendar shows you are busy now.")
                calendar.minutesUntilNextEvent != null && calendar.minutesUntilNextEvent <= 60 ->
                    items += BriefItem(80, "Next calendar event is in ${calendar.minutesUntilNextEvent} min.")
            }
        }

        if (isFreshSnapshot(bundle.notifications.capturedAt, bundle.capturedAt, MAX_NOTIFICATION_BRIEF_AGE_MINUTES)) {
            (bundle.notifications.access as? ContextValue.Available)?.value?.let { notifications ->
                if (notifications.attentionCount > 0) {
                    items += BriefItem(
                        75,
                        "${notifications.attentionCount} notification${if (notifications.attentionCount == 1) " may" else "s may"} need attention."
                    )
                }
            }
        }

        (bundle.device.power as? ContextValue.Available)?.value?.let { power ->
            val percent = power.batteryPercent
            if (!power.isCharging && percent != null && percent <= 20) {
                items += BriefItem(70, "Battery is low at $percent%.")
            }
        }

        (bundle.device.connectivity as? ContextValue.Available)?.value?.let { connectivity ->
            if (connectivity == ConnectivityState.OFFLINE) {
                items += BriefItem(65, "Device is offline; Mayra's local features remain available.")
            }
        }

        (bundle.knowledge.documents as? ContextValue.Available)?.value?.let { documents ->
            if (documents.needsAttentionCount > 0) {
                items += BriefItem(
                    45,
                    "Library has ${documents.needsAttentionCount} document${if (documents.needsAttentionCount == 1) "" else "s"} needing indexing or refresh."
                )
            }
        }

        val ranked = items
            .sortedByDescending(BriefItem::priority)
            .take(MAX_DAILY_BRIEF_ITEMS)

        return if (ranked.isEmpty()) {
            "Daily brief: nothing urgent is visible in Mayra's coarse local context right now."
        } else {
            buildString {
                append("Daily brief:\n")
                ranked.forEach { append("• ").append(it.text).append('\n') }
                append("Based only on coarse local context; private source text is not included.")
            }
        }
    }

    private fun combined(bundle: MayraContextBundle): String = buildList {
        add("Local context status:")
        add("• ${deviceLine(bundle.device)}")
        add("• ${agendaLine(bundle)}")
        bundle.knowledge.summaryLines().forEach { add("• $it") }
        add("• ${notificationLine(bundle.notifications)}")
        add("• ${peopleLine(bundle.contacts)}")
        add("Only coarse local context is shown here; private source text is not included.")
    }.joinToString("\n")

    private fun memory(snapshot: KnowledgeContextSnapshot): String = when (val value = snapshot.memory) {
        is ContextValue.Available ->
            "Memory status: ${value.value.savedCount} approved personal memor${if (value.value.savedCount == 1) "y is" else "ies are"} saved locally."
        ContextValue.NotGranted -> "Memory status: not enabled."
        ContextValue.Unavailable -> "Memory status: currently unavailable."
    }

    private fun library(snapshot: KnowledgeContextSnapshot): String = when (val value = snapshot.documents) {
        is ContextValue.Available -> {
            val docs = value.value
            when {
                docs.savedCount == 0 -> "Library status: no saved documents yet."
                docs.needsAttentionCount > 0 ->
                    "Library status: ${docs.savedCount} saved, ${docs.currentIndexedCount} current, ${docs.needsAttentionCount} need indexing or refresh."
                else -> "Library status: ${docs.savedCount} saved and all ${docs.currentIndexedCount} indexed documents are current."
            }
        }
        ContextValue.NotGranted -> "Library status: not enabled."
        ContextValue.Unavailable -> "Library status: currently unavailable."
    }

    private fun agenda(bundle: MayraContextBundle): String {
        val calendar = when (val value = bundle.calendar.access) {
            is ContextValue.Available -> buildString {
                append("Calendar: ${value.value.remainingEventsToday} remaining today")
                if (value.value.busyNow) append(", busy now")
                value.value.minutesUntilNextEvent?.let { append(", next in $it min") }
                append('.')
            }
            ContextValue.NotGranted -> "Calendar: access not enabled."
            ContextValue.Unavailable -> "Calendar: unavailable."
        }
        val reminders = when (val value = bundle.reminders.access) {
            is ContextValue.Available -> buildString {
                append("Reminders: ${value.value.activeCount} active, ${value.value.dueOrOverdueCount} due or overdue")
                value.value.minutesUntilNextReminder?.let { append(", next in $it min") }
                append('.')
            }
            ContextValue.NotGranted -> "Reminders: access not enabled."
            ContextValue.Unavailable -> "Reminders: unavailable."
        }
        return "$calendar $reminders"
    }

    private fun notifications(snapshot: NotificationContextSnapshot): String = notificationLine(snapshot)
    private fun people(snapshot: ContactsContextSnapshot): String = peopleLine(snapshot)
    private fun power(snapshot: MayraContextSnapshot): String = when (val value = snapshot.power) {
        is ContextValue.Available -> {
            val percent = value.value.batteryPercent?.let { "$it%" } ?: "percentage unavailable"
            "Battery status: $percent, ${if (value.value.isCharging) "charging" else "not charging"}."
        }
        ContextValue.NotGranted -> "Battery status: access not enabled."
        ContextValue.Unavailable -> "Battery status: unavailable."
    }

    private fun connectivity(snapshot: MayraContextSnapshot): String = when (val value = snapshot.connectivity) {
        is ContextValue.Available -> "Connectivity status: ${value.value.name.lowercase(Locale.ROOT)}."
        ContextValue.NotGranted -> "Connectivity status: access not enabled."
        ContextValue.Unavailable -> "Connectivity status: unavailable."
    }

    private fun deviceLine(snapshot: MayraContextSnapshot): String {
        val connectivity = when (val value = snapshot.connectivity) {
            is ContextValue.Available -> value.value.name.lowercase(Locale.ROOT)
            ContextValue.NotGranted -> "connectivity not enabled"
            ContextValue.Unavailable -> "connectivity unavailable"
        }
        val battery = when (val value = snapshot.power) {
            is ContextValue.Available -> value.value.batteryPercent?.let { "$it% battery" } ?: "battery available"
            ContextValue.NotGranted -> "battery not enabled"
            ContextValue.Unavailable -> "battery unavailable"
        }
        return "Device · $connectivity · $battery"
    }

    private fun agendaLine(bundle: MayraContextBundle): String {
        val calendarCount = (bundle.calendar.access as? ContextValue.Available)?.value?.remainingEventsToday
        val dueCount = (bundle.reminders.access as? ContextValue.Available)?.value?.dueOrOverdueCount
        return "Agenda · ${calendarCount?.let { "$it calendar remaining" } ?: "calendar unavailable"} · ${dueCount?.let { "$it reminders due" } ?: "reminders unavailable"}"
    }

    private fun notificationLine(snapshot: NotificationContextSnapshot): String = when (val value = snapshot.access) {
        is ContextValue.Available ->
            "Notifications: ${value.value.activeCount} active, ${value.value.attentionCount} may need attention."
        ContextValue.NotGranted -> "Notifications: access not enabled."
        ContextValue.Unavailable -> "Notifications: unavailable."
    }

    private fun peopleLine(snapshot: ContactsContextSnapshot): String = when (val value = snapshot.access) {
        is ContextValue.Available ->
            "People: ${value.value.totalContacts} contacts, ${value.value.phoneCapableContacts} with a phone number."
        ContextValue.NotGranted -> "People: contacts access not enabled."
        ContextValue.Unavailable -> "People: unavailable."
    }

    private fun String.looksLikeActionCommand(): Boolean = ACTION_MARKERS.any(::contains)

    private fun String.isDailyBriefQuestion(): Boolean = containsAny(
        "daily brief", "today brief", "today's brief", "what should i know", "what is important today",
        "what's important today", "aaj kya important", "aaj kya zaroori", "aaj kya jaruri",
        "aaj ka brief", "mera brief"
    )

    private fun String.isCombinedContextQuestion(): Boolean =
        containsAny("context status", "mayra context", "local context status", "context kya hai")

    private fun String.isMemoryStatusQuestion(): Boolean =
        containsAny("memory status", "memory me kitni", "memory mein kitni", "how many memories", "saved memories")

    private fun String.isLibraryStatusQuestion(): Boolean =
        containsAny("library status", "library health", "document status", "documents status", "docs status", "library ready", "documents ready", "kitne document", "kitni files")

    private fun String.isAgendaStatusQuestion(): Boolean =
        containsAny("agenda status", "aaj kya due", "what is due", "what's due", "calendar status", "reminder status", "reminders status", "kitne reminder due", "busy now")

    private fun String.isNotificationStatusQuestion(): Boolean =
        containsAny("notification status", "notifications status", "how many notifications", "kitni notification", "kitne notification")

    private fun String.isPeopleStatusQuestion(): Boolean =
        containsAny("people status", "contacts status", "contact status", "how many contacts", "kitne contact", "kitni contacts")

    private fun String.isPowerStatusQuestion(): Boolean =
        containsAny("battery status", "battery kitni", "charge kitna", "charge kitni")

    private fun String.isConnectivityStatusQuestion(): Boolean =
        containsAny("connectivity status", "network status", "internet status", "online status", "offline status")

    private fun String.containsAny(vararg values: String): Boolean = values.any(::contains)

    private data class BriefItem(val priority: Int, val text: String)

    private val ACTION_MARKERS = listOf(
        "set reminder", "reminder set", "reminder laga", "remind me", "yaad dilana",
        "call ", "phone ", "dial ", "message ", "send message", "send sms", "sms ",
        "open ", "launch ", "kholo", "khol ", "chalao"
    )

    private const val MAX_DAILY_BRIEF_ITEMS = 4
    private const val MAX_NOTIFICATION_BRIEF_AGE_MINUTES = 120L
}
