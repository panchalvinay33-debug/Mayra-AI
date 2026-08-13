package ai.mayra.app.context

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class MayraContextSnapshotTest {
    @Test
    fun `deriveDayPart maps boundary hours deterministically`() {
        assertEquals(DayPart.NIGHT, deriveDayPart(0))
        assertEquals(DayPart.MORNING, deriveDayPart(5))
        assertEquals(DayPart.MORNING, deriveDayPart(11))
        assertEquals(DayPart.AFTERNOON, deriveDayPart(12))
        assertEquals(DayPart.AFTERNOON, deriveDayPart(16))
        assertEquals(DayPart.EVENING, deriveDayPart(17))
        assertEquals(DayPart.EVENING, deriveDayPart(20))
        assertEquals(DayPart.NIGHT, deriveDayPart(21))
        assertEquals(DayPart.NIGHT, deriveDayPart(23))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deriveDayPart rejects invalid hour`() {
        deriveDayPart(24)
    }

    @Test
    fun `summary is deterministic and provenance-backed`() {
        val snapshot = MayraContextSnapshot(
            capturedAt = LocalDateTime.of(2026, 8, 5, 18, 0),
            dayPart = DayPart.EVENING,
            connectivity = ContextValue.Available(
                ConnectivityState.ONLINE,
                ContextSource.CONNECTIVITY_MANAGER
            ),
            power = ContextValue.Available(
                PowerState(isCharging = true, batteryPercent = 92),
                ContextSource.BATTERY_MANAGER
            )
        )

        assertEquals(listOf("Evening", "92% · charging", "Online"), snapshot.summaryLines())
    }

    @Test
    fun `summary degrades without inventing context`() {
        val snapshot = MayraContextSnapshot(
            capturedAt = LocalDateTime.of(2026, 8, 5, 23, 0),
            dayPart = DayPart.NIGHT
        )

        assertEquals(
            listOf("Night", "Battery unavailable", "Network state unavailable"),
            snapshot.summaryLines()
        )
    }
}
