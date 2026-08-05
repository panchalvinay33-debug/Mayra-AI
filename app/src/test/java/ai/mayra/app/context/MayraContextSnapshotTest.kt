package ai.mayra.app.context

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
}
