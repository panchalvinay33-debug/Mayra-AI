package ai.mayra.app.core.intelligence

import java.time.Duration
import java.time.Instant

data class ToolExecutionMetric(
    val toolId: String,
    val status: ToolExecutionStatus,
    val attempt: Int,
    val startedAt: Instant,
    val completedAt: Instant,
    val errorCode: String? = null
) {
    init {
        require(toolId.isNotBlank()) { "Tool id cannot be blank." }
        require(attempt > 0) { "Attempt must be positive." }
        require(!completedAt.isBefore(startedAt)) { "Completion cannot precede start." }
    }

    val duration: Duration get() = Duration.between(startedAt, completedAt)
}

data class ToolExecutionSummary(
    val toolId: String,
    val total: Int,
    val successes: Int,
    val failures: Int,
    val denied: Int,
    val notFound: Int,
    val averageDurationMillis: Long
)

class ToolExecutionTelemetry(private val capacity: Int = 500) {
    private val metrics = ArrayDeque<ToolExecutionMetric>()

    init {
        require(capacity > 0) { "Telemetry capacity must be positive." }
    }

    @Synchronized
    fun record(metric: ToolExecutionMetric) {
        metrics.addLast(metric)
        while (metrics.size > capacity) metrics.removeFirst()
    }

    @Synchronized
    fun snapshot(toolId: String? = null): List<ToolExecutionMetric> = metrics
        .filter { toolId == null || it.toolId == toolId }

    @Synchronized
    fun summarize(toolId: String): ToolExecutionSummary {
        val selected = metrics.filter { it.toolId == toolId }
        val average = if (selected.isEmpty()) 0L else selected.sumOf { it.duration.toMillis() } / selected.size
        return ToolExecutionSummary(
            toolId = toolId,
            total = selected.size,
            successes = selected.count { it.status == ToolExecutionStatus.SUCCESS },
            failures = selected.count { it.status == ToolExecutionStatus.FAILED },
            denied = selected.count { it.status == ToolExecutionStatus.DENIED },
            notFound = selected.count { it.status == ToolExecutionStatus.NOT_FOUND },
            averageDurationMillis = average
        )
    }

    @Synchronized
    fun clear() {
        metrics.clear()
    }
}
