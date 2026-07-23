package ai.mayra.app.core.briefing

import ai.mayra.app.core.notification.NotificationAssessment
import ai.mayra.app.core.notification.NotificationPriority
import ai.mayra.app.core.notification.NotificationSignal

/**
 * Builds a compact, deterministic daily briefing from already-assessed notifications.
 *
 * The engine is deliberately Android-free so it can be tested on the JVM and reused by
 * voice, home-screen, scheduled-worker, and notification-listener surfaces.
 */
data class BriefingItem(
    val title: String,
    val detail: String?,
    val sourcePackage: String,
    val priority: NotificationPriority,
    val score: Int,
    val postedAt: Long
)

data class DailyBriefing(
    val generatedAt: Long,
    val headline: String,
    val items: List<BriefingItem>,
    val urgentCount: Int,
    val highCount: Int,
    val normalCount: Int,
    val suppressedCount: Int
) {
    val hasAnythingImportant: Boolean
        get() = items.isNotEmpty()
}

class DailyBriefingEngine(
    private val maxItems: Int = DEFAULT_MAX_ITEMS,
    private val maxItemsPerPackage: Int = DEFAULT_MAX_ITEMS_PER_PACKAGE
) {
    init {
        require(maxItems > 0) { "maxItems must be greater than zero" }
        require(maxItemsPerPackage > 0) { "maxItemsPerPackage must be greater than zero" }
    }

    fun build(
        assessedNotifications: List<Pair<NotificationSignal, NotificationAssessment>>,
        generatedAt: Long
    ): DailyBriefing {
        val eligible = assessedNotifications
            .filter { (_, assessment) -> assessment.shouldIncludeInBriefing }
            .sortedWith(
                compareByDescending<Pair<NotificationSignal, NotificationAssessment>> {
                    it.second.score
                }.thenByDescending { it.first.postedAt }
            )

        val packageCounts = mutableMapOf<String, Int>()
        val selected = mutableListOf<Pair<NotificationSignal, NotificationAssessment>>()

        for (entry in eligible) {
            if (selected.size >= maxItems) break

            val packageName = entry.first.packageName
            val currentCount = packageCounts[packageName] ?: 0
            if (currentCount >= maxItemsPerPackage) continue

            selected += entry
            packageCounts[packageName] = currentCount + 1
        }

        val items = selected.map { (signal, assessment) ->
            BriefingItem(
                title = signal.title.cleanOrFallback(signal.packageName),
                detail = signal.body.cleanOrNull(),
                sourcePackage = signal.packageName,
                priority = assessment.priority,
                score = assessment.score,
                postedAt = signal.postedAt
            )
        }

        val urgentCount = eligible.count { it.second.priority == NotificationPriority.URGENT }
        val highCount = eligible.count { it.second.priority == NotificationPriority.HIGH }
        val normalCount = eligible.count { it.second.priority == NotificationPriority.NORMAL }
        val suppressedCount = assessedNotifications.size - eligible.size

        return DailyBriefing(
            generatedAt = generatedAt,
            headline = createHeadline(
                urgentCount = urgentCount,
                highCount = highCount,
                normalCount = normalCount,
                selectedCount = items.size
            ),
            items = items,
            urgentCount = urgentCount,
            highCount = highCount,
            normalCount = normalCount,
            suppressedCount = suppressedCount
        )
    }

    private fun createHeadline(
        urgentCount: Int,
        highCount: Int,
        normalCount: Int,
        selectedCount: Int
    ): String = when {
        urgentCount > 0 -> "$urgentCount urgent update${urgentCount.pluralSuffix()} need attention"
        highCount > 0 -> "$highCount important update${highCount.pluralSuffix()} in your briefing"
        selectedCount > 0 -> "$selectedCount update${selectedCount.pluralSuffix()} ready for you"
        normalCount > 0 -> "$normalCount update${normalCount.pluralSuffix()} available"
        else -> "You're all caught up"
    }

    private fun String?.cleanOrFallback(fallback: String): String =
        cleanOrNull() ?: fallback

    private fun String?.cleanOrNull(): String? = this
        ?.trim()
        ?.replace(WHITESPACE_REGEX, " ")
        ?.takeIf { it.isNotEmpty() }
        ?.take(MAX_TEXT_LENGTH)

    private fun Int.pluralSuffix(): String = if (this == 1) "" else "s"

    companion object {
        const val DEFAULT_MAX_ITEMS = 8
        const val DEFAULT_MAX_ITEMS_PER_PACKAGE = 2
        const val MAX_TEXT_LENGTH = 240
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
