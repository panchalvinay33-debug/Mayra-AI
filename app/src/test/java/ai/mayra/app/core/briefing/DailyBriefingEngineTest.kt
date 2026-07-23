package ai.mayra.app.core.briefing

import ai.mayra.app.core.notification.NotificationAssessment
import ai.mayra.app.core.notification.NotificationCategory
import ai.mayra.app.core.notification.NotificationPriority
import ai.mayra.app.core.notification.NotificationSignal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyBriefingEngineTest {
    @Test
    fun prioritizesUrgentItemsAndSuppressesLowPriorityNoise() {
        val engine = DailyBriefingEngine(maxItems = 3, maxItemsPerPackage = 2)
        val urgent = signal("com.family", "Call home", 100L) to assessment(
            NotificationPriority.URGENT,
            score = 95,
            include = true
        )
        val normal = signal("com.calendar", "Meeting", 200L) to assessment(
            NotificationPriority.NORMAL,
            score = 50,
            include = true
        )
        val promotion = signal("com.shop", "Sale", 300L) to assessment(
            NotificationPriority.LOW,
            score = 5,
            include = false
        )

        val briefing = engine.build(listOf(normal, promotion, urgent), generatedAt = 999L)

        assertEquals(999L, briefing.generatedAt)
        assertEquals("1 urgent update need attention", briefing.headline)
        assertEquals(listOf("Call home", "Meeting"), briefing.items.map { it.title })
        assertEquals(1, briefing.urgentCount)
        assertEquals(1, briefing.normalCount)
        assertEquals(1, briefing.suppressedCount)
        assertTrue(briefing.hasAnythingImportant)
    }

    @Test
    fun capsItemsPerPackageToKeepBriefingDiverse() {
        val engine = DailyBriefingEngine(maxItems = 4, maxItemsPerPackage = 1)
        val entries = listOf(
            signal("com.chat", "First", 400L) to assessment(NotificationPriority.HIGH, 90, true),
            signal("com.chat", "Second", 300L) to assessment(NotificationPriority.HIGH, 80, true),
            signal("com.mail", "Email", 200L) to assessment(NotificationPriority.HIGH, 70, true),
            signal("com.calendar", "Event", 100L) to assessment(NotificationPriority.NORMAL, 60, true)
        )

        val briefing = engine.build(entries, generatedAt = 500L)

        assertEquals(listOf("First", "Email", "Event"), briefing.items.map { it.title })
        assertEquals(3, briefing.items.map { it.sourcePackage }.distinct().size)
    }

    @Test
    fun cleansTextAndFallsBackToPackageNameForBlankTitle() {
        val engine = DailyBriefingEngine()
        val entry = NotificationSignal(
            packageName = "com.example.app",
            title = "   ",
            body = "  Multiple\n spaces\tbecome one  ",
            postedAt = 1L,
            category = NotificationCategory.MESSAGE
        ) to assessment(NotificationPriority.NORMAL, 50, true)

        val item = engine.build(listOf(entry), generatedAt = 2L).items.single()

        assertEquals("com.example.app", item.title)
        assertEquals("Multiple spaces become one", item.detail)
    }

    @Test
    fun emptyBriefingReportsCaughtUp() {
        val engine = DailyBriefingEngine()

        val briefing = engine.build(emptyList(), generatedAt = 10L)

        assertEquals("You're all caught up", briefing.headline)
        assertTrue(briefing.items.isEmpty())
        assertFalse(briefing.hasAnythingImportant)
        assertEquals(0, briefing.suppressedCount)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidMaximumItemCount() {
        DailyBriefingEngine(maxItems = 0)
    }

    private fun signal(packageName: String, title: String, postedAt: Long) = NotificationSignal(
        packageName = packageName,
        title = title,
        body = null,
        postedAt = postedAt,
        category = NotificationCategory.OTHER
    )

    private fun assessment(
        priority: NotificationPriority,
        score: Int,
        include: Boolean
    ) = NotificationAssessment(
        priority = priority,
        score = score,
        reasons = emptyList(),
        shouldInterrupt = priority == NotificationPriority.URGENT,
        shouldIncludeInBriefing = include
    )
}
