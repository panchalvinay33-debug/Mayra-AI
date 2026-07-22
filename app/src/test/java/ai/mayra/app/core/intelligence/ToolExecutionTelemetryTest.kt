package ai.mayra.app.core.intelligence

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolExecutionTelemetryTest {
    @Test
    fun `summarizes status counts and average duration`() {
        val telemetry = ToolExecutionTelemetry()
        val start = Instant.parse("2026-01-01T00:00:00Z")
        telemetry.record(ToolExecutionMetric("device.open_url", ToolExecutionStatus.SUCCESS, 1, start, start.plusMillis(10)))
        telemetry.record(ToolExecutionMetric("device.open_url", ToolExecutionStatus.FAILED, 2, start, start.plusMillis(30), "timeout"))

        val summary = telemetry.summarize("device.open_url")

        assertEquals(2, summary.total)
        assertEquals(1, summary.successes)
        assertEquals(1, summary.failures)
        assertEquals(20L, summary.averageDurationMillis)
    }

    @Test
    fun `evicts oldest metrics when capacity is exceeded`() {
        val telemetry = ToolExecutionTelemetry(capacity = 2)
        val start = Instant.parse("2026-01-01T00:00:00Z")
        telemetry.record(ToolExecutionMetric("one", ToolExecutionStatus.SUCCESS, 1, start, start))
        telemetry.record(ToolExecutionMetric("two", ToolExecutionStatus.SUCCESS, 1, start, start))
        telemetry.record(ToolExecutionMetric("three", ToolExecutionStatus.SUCCESS, 1, start, start))

        assertEquals(listOf("two", "three"), telemetry.snapshot().map { it.toolId })
    }
}
