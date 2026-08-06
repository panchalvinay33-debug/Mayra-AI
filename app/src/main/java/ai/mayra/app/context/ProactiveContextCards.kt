package ai.mayra.app.context

import ai.mayra.app.MayraEntryContract
import java.time.Duration
import java.time.LocalDateTime

/** Deterministic, local-only J6 proactive cards. No model ranking and no free-form private content. */
enum class ProactiveCardKind {
    REMINDER_DUE,
    BUSY_NOW,
    CALENDAR_SOON,
    NOTIFICATION_ATTENTION,
    LOW_BATTERY,
    OFFLINE,
    REMINDER_SOON,
    RECENT_VOICE_SESSION
}

data class ProactiveContextCard(
    val kind: ProactiveCardKind,
    val priority: Int,
    val title: String,
    val detail: String
) {
    init {
        require(priority in 0..100)
        require(title.isNotBlank())
        require(detail.isNotBlank())
    }
}

/**
 * Produces a bounded, deterministic local ranking from already-normalized context. Contacts are not
 * ranked because a contact count alone is not an actionable situation. No raw source data enters
 * these cards.
 */
fun rankProactiveContextCards(
    bundle: MayraContextBundle,
    maxCards: Int = 3
): List<ProactiveContextCard> {
    require(maxCards in 1..5)
    val cards = mutableListOf<ProactiveContextCard>()

    (bundle.reminders.access as? ContextValue.Available)?.value?.let { reminders ->
        if (reminders.dueOrOverdueCount > 0) {
            cards += ProactiveContextCard(
                ProactiveCardKind.REMINDER_DUE,
                100,
                "Reminder due",
                "${reminders.dueOrOverdueCount} reminder${if (reminders.dueOrOverdueCount == 1) "" else "s"} due or overdue"
            )
        } else if (reminders.minutesUntilNextReminder != null && reminders.minutesUntilNextReminder <= 60) {
            cards += ProactiveContextCard(
                ProactiveCardKind.REMINDER_SOON,
                60,
                "Reminder coming up",
                "Next reminder in ${reminders.minutesUntilNextReminder} min"
            )
        }
    }

    (bundle.calendar.access as? ContextValue.Available)?.value?.let { calendar ->
        when {
            calendar.busyNow -> cards += ProactiveContextCard(
                ProactiveCardKind.BUSY_NOW,
                95,
                "Busy now",
                "Calendar shows an active event"
            )
            calendar.minutesUntilNextEvent != null && calendar.minutesUntilNextEvent <= 60 ->
                cards += ProactiveContextCard(
                    ProactiveCardKind.CALENDAR_SOON,
                    80,
                    "Calendar soon",
                    "Next event in ${calendar.minutesUntilNextEvent} min"
                )
        }
    }

    if (isFreshSnapshot(bundle.notifications.capturedAt, bundle.capturedAt, MAX_NOTIFICATION_CARD_AGE_MINUTES)) {
        (bundle.notifications.access as? ContextValue.Available)?.value?.let { notifications ->
            if (notifications.attentionCount > 0) {
                cards += ProactiveContextCard(
                    ProactiveCardKind.NOTIFICATION_ATTENTION,
                    75,
                    "Notifications need attention",
                    "${notifications.attentionCount} may need attention"
                )
            }
        }
    }

    (bundle.device.power as? ContextValue.Available)?.value?.let { power ->
        val percent = power.batteryPercent
        if (!power.isCharging && percent != null && percent <= 20) {
            cards += ProactiveContextCard(
                ProactiveCardKind.LOW_BATTERY,
                70,
                "Low battery",
                "$percent% remaining"
            )
        }
    }

    (bundle.device.connectivity as? ContextValue.Available)?.value?.let { connectivity ->
        if (connectivity == ConnectivityState.OFFLINE) {
            cards += ProactiveContextCard(
                ProactiveCardKind.OFFLINE,
                65,
                "Offline",
                "Mayra Home and local features remain available"
            )
        }
    }

    (bundle.session.access as? ContextValue.Available)?.value?.let { session ->
        if (session.source == MayraEntryContract.Source.VOICE_SESSION && session.minutesSinceEntry <= 15) {
            cards += ProactiveContextCard(
                ProactiveCardKind.RECENT_VOICE_SESSION,
                40,
                "Recent voice session",
                "Voice entry ${session.minutesSinceEntry} min ago"
            )
        }
    }

    return cards
        .distinctBy(ProactiveContextCard::kind)
        .sortedWith(compareByDescending<ProactiveContextCard> { it.priority }.thenBy { it.kind.ordinal })
        .take(maxCards)
}

internal fun isFreshSnapshot(
    capturedAt: LocalDateTime,
    now: LocalDateTime,
    maxAgeMinutes: Long
): Boolean {
    require(maxAgeMinutes >= 0L)
    if (capturedAt.isAfter(now)) return false
    return Duration.between(capturedAt, now).toMinutes() <= maxAgeMinutes
}

private const val MAX_NOTIFICATION_CARD_AGE_MINUTES = 120L
