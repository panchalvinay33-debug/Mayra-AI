package ai.mayra.app.core.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationIntelligenceTest {
    private val engine = NotificationIntelligence(
        importantPackages = setOf("com.example.family"),
        mutedPackages = setOf("com.example.noisy")
    )

    @Test
    fun urgentCallFromImportantAppInterrupts() {
        val assessment = engine.assess(
            NotificationSignal(
                packageName = "com.example.family",
                title = "Missed call",
                body = "Emergency - call back ASAP",
                postedAt = 100L,
                category = NotificationCategory.CALL
            )
        )

        assertEquals(NotificationPriority.URGENT, assessment.priority)
        assertEquals(100, assessment.score)
        assertTrue(assessment.shouldInterrupt)
        assertTrue(assessment.shouldIncludeInBriefing)
    }

    @Test
    fun promotionIsSuppressedFromBriefing() {
        val assessment = engine.assess(
            NotificationSignal(
                packageName = "com.example.store",
                title = "Limited time sale",
                body = "Use this coupon and shop now",
                postedAt = 100L,
                category = NotificationCategory.PROMOTION
            )
        )

        assertEquals(NotificationPriority.LOW, assessment.priority)
        assertFalse(assessment.shouldInterrupt)
        assertFalse(assessment.shouldIncludeInBriefing)
    }

    @Test
    fun mutedAppCannotInterruptEvenWithUrgentText() {
        val assessment = engine.assess(
            NotificationSignal(
                packageName = "com.example.noisy",
                title = "Urgent",
                body = "Emergency",
                postedAt = 100L,
                category = NotificationCategory.MESSAGE
            )
        )

        assertEquals(NotificationPriority.LOW, assessment.priority)
        assertEquals(0, assessment.score)
        assertFalse(assessment.shouldInterrupt)
    }

    @Test
    fun rankingUsesScoreThenNewestTimestamp() {
        val low = NotificationSignal(
            packageName = "com.example.store",
            title = "Sale",
            body = null,
            postedAt = 300L,
            category = NotificationCategory.PROMOTION
        )
        val olderHigh = NotificationSignal(
            packageName = "com.example.calendar",
            title = "Meeting",
            body = null,
            postedAt = 100L,
            category = NotificationCategory.CALENDAR
        )
        val newerHigh = olderHigh.copy(postedAt = 200L, title = "Appointment")

        val ranked = engine.rank(listOf(low, olderHigh, newerHigh))

        assertEquals(newerHigh, ranked[0].first)
        assertEquals(olderHigh, ranked[1].first)
        assertEquals(low, ranked[2].first)
    }
}
