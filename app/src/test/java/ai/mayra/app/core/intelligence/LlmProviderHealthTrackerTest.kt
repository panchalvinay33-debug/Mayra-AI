package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmProviderHealthTrackerTest {

    @Test
    fun `provider becomes unhealthy after threshold and recovers after cooldown`() {
        val clock = MutableClock(Instant.parse("2026-07-22T10:00:00Z"))
        val tracker = LlmProviderHealthTracker(
            failureThreshold = 2,
            cooldown = Duration.ofSeconds(30),
            clock = clock
        )

        tracker.recordFailure("remote")
        assertTrue(tracker.isHealthy("remote"))
        tracker.recordFailure("remote")
        assertFalse(tracker.isHealthy("remote"))

        clock.advance(Duration.ofSeconds(31))
        assertTrue(tracker.isHealthy("remote"))
        assertEquals(0, tracker.snapshot("remote").consecutiveFailures)
    }

    @Test
    fun `success resets consecutive failures and records totals`() {
        val tracker = LlmProviderHealthTracker(failureThreshold = 3)
        tracker.recordFailure("local")
        tracker.recordSuccess("local")

        val health = tracker.snapshot("local")
        assertEquals(1, health.failures)
        assertEquals(1, health.successes)
        assertEquals(0, health.consecutiveFailures)
        assertTrue(tracker.isHealthy("local"))
    }

    private class MutableClock(private var instant: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("UTC")
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = instant
        fun advance(duration: Duration) {
            instant = instant.plus(duration)
        }
    }
}
