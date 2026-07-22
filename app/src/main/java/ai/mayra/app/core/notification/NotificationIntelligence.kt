package ai.mayra.app.core.notification

/**
 * Privacy-first, deterministic notification triage for Mayra AI.
 *
 * This module intentionally contains no Android framework dependency so it can be
 * unit-tested on the JVM and reused by notification-listener, inbox, and briefing
 * features.
 */
data class NotificationSignal(
    val packageName: String,
    val title: String?,
    val body: String?,
    val postedAt: Long,
    val category: NotificationCategory = NotificationCategory.OTHER,
    val isOngoing: Boolean = false,
    val isGroupSummary: Boolean = false
)

enum class NotificationCategory {
    MESSAGE,
    CALL,
    REMINDER,
    CALENDAR,
    TRANSACTION,
    DELIVERY,
    SYSTEM,
    PROMOTION,
    OTHER
}

enum class NotificationPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

data class NotificationAssessment(
    val priority: NotificationPriority,
    val score: Int,
    val reasons: List<String>,
    val shouldInterrupt: Boolean,
    val shouldIncludeInBriefing: Boolean
)

class NotificationIntelligence(
    private val importantPackages: Set<String> = emptySet(),
    private val mutedPackages: Set<String> = emptySet(),
    private val urgentKeywords: Set<String> = DEFAULT_URGENT_KEYWORDS,
    private val promotionalKeywords: Set<String> = DEFAULT_PROMOTIONAL_KEYWORDS
) {
    fun assess(signal: NotificationSignal): NotificationAssessment {
        val reasons = mutableListOf<String>()
        var score = categoryScore(signal.category)

        if (signal.packageName in mutedPackages) {
            score -= 100
            reasons += "muted app"
        }

        if (signal.packageName in importantPackages) {
            score += 20
            reasons += "important app"
        }

        if (signal.isOngoing) {
            score -= 10
            reasons += "ongoing notification"
        }

        if (signal.isGroupSummary) {
            score -= 15
            reasons += "group summary"
        }

        val searchableText = listOfNotNull(signal.title, signal.body)
            .joinToString(" ")
            .lowercase()

        val urgentMatch = urgentKeywords.firstOrNull { searchableText.contains(it) }
        if (urgentMatch != null) {
            score += 40
            reasons += "urgent keyword: $urgentMatch"
        }

        val promotionalMatch = promotionalKeywords.firstOrNull { searchableText.contains(it) }
        if (promotionalMatch != null) {
            score -= 35
            reasons += "promotional keyword: $promotionalMatch"
        }

        val boundedScore = score.coerceIn(0, 100)
        val priority = when {
            boundedScore >= 80 -> NotificationPriority.URGENT
            boundedScore >= 60 -> NotificationPriority.HIGH
            boundedScore >= 30 -> NotificationPriority.NORMAL
            else -> NotificationPriority.LOW
        }

        return NotificationAssessment(
            priority = priority,
            score = boundedScore,
            reasons = reasons.ifEmpty { listOf("default category priority") },
            shouldInterrupt = priority == NotificationPriority.URGENT,
            shouldIncludeInBriefing = priority != NotificationPriority.LOW
        )
    }

    fun rank(signals: List<NotificationSignal>): List<Pair<NotificationSignal, NotificationAssessment>> =
        signals
            .map { it to assess(it) }
            .sortedWith(
                compareByDescending<Pair<NotificationSignal, NotificationAssessment>> { it.second.score }
                    .thenByDescending { it.first.postedAt }
            )

    private fun categoryScore(category: NotificationCategory): Int = when (category) {
        NotificationCategory.CALL -> 70
        NotificationCategory.REMINDER -> 65
        NotificationCategory.CALENDAR -> 60
        NotificationCategory.TRANSACTION -> 55
        NotificationCategory.MESSAGE -> 50
        NotificationCategory.DELIVERY -> 40
        NotificationCategory.SYSTEM -> 25
        NotificationCategory.OTHER -> 20
        NotificationCategory.PROMOTION -> 5
    }

    companion object {
        val DEFAULT_URGENT_KEYWORDS: Set<String> = setOf(
            "urgent",
            "emergency",
            "asap",
            "missed call",
            "payment failed",
            "overdue",
            "cancelled"
        )

        val DEFAULT_PROMOTIONAL_KEYWORDS: Set<String> = setOf(
            "sale",
            "discount",
            "offer",
            "coupon",
            "limited time",
            "shop now"
        )
    }
}
