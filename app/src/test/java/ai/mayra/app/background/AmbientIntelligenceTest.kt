package ai.mayra.app.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientIntelligenceTest {
    private val engine = NotificationIntelligenceEngine()

    @Test
    fun otpNotificationIsHighPriorityWithoutOpeningApp() {
        val insight = engine.analyze(event("com.google.android.apps.messaging", "OTP", "Your OTP is 482910"))

        assertEquals(AmbientCategory.OTP, insight.category)
        assertEquals(AmbientPriority.HIGH, insight.priority)
        assertEquals(ScreenDecision.NOTIFICATION_ONLY, insight.screenDecision)
    }

    @Test
    fun suspiciousBankAlertRequiresConfirmation() {
        val insight = engine.analyze(event("com.example.bank", "Security alert", "Unauthorized transaction detected"))

        assertEquals(AmbientCategory.BANKING, insight.category)
        assertEquals(AmbientPriority.CRITICAL, insight.priority)
        assertEquals(ScreenDecision.REQUIRE_CONFIRMATION, insight.screenDecision)
        assertEquals("review_security", insight.suggestedAction)
    }

    @Test
    fun deliveryUpdateCreatesTrackingSuggestion() {
        val insight = engine.analyze(event("com.amazon.mShop.android.shopping", "Order", "Your package is out for delivery"))

        assertEquals(AmbientCategory.DELIVERY, insight.category)
        assertEquals("track_delivery", insight.suggestedAction)
    }

    @Test
    fun promotionStaysLowPriorityAndBackgroundOnly() {
        val insight = engine.analyze(event("com.store", "Mega sale", "Get 40% discount today"))

        assertEquals(AmbientCategory.PROMOTION, insight.category)
        assertEquals(AmbientPriority.LOW, insight.priority)
        assertEquals(ScreenDecision.BACKGROUND_ONLY, insight.screenDecision)
    }

    @Test
    fun dailyBriefingPrefersImportantItemsAndLimitsOutput() {
        val events = listOf(
            event("com.store", "Sale", "Coupon deal", 100L),
            event("com.bank", "Security alert", "Suspicious activity", 200L),
            event("com.messaging", "OTP", "OTP 123456", 300L)
        )

        val briefing = DailyBriefingEngine().build(events, since = 0L, maxItems = 2)

        assertTrue(briefing.contains("Banking update") || briefing.contains("OTP received"))
        assertTrue(briefing.contains("1 more updates"))
    }

    @Test
    fun taskProcessorKeepsSensitiveTasksForConfirmation() {
        val processor = AmbientTaskProcessor()
        val task = BackgroundTask(type = "reply", payload = "Reply to message", createdAt = 1L)

        val outcome = processor.process(task)

        assertTrue(outcome is TaskOutcome.Retry)
        assertEquals("User confirmation is required.", (outcome as TaskOutcome.Retry).reason)
    }

    @Test
    fun safeBackgroundTaskCanCompleteWithoutOpeningScreen() {
        val processor = AmbientTaskProcessor()
        val task = BackgroundTask(type = "track_delivery", payload = "Package update", createdAt = 1L)

        assertEquals(TaskOutcome.Completed, processor.process(task))
    }

    private fun event(
        source: String,
        title: String,
        text: String,
        timestamp: Long = 1L
    ) = AmbientEvent(source, title, text, timestamp)
}
