package ai.mayra.app.core.intelligence

import java.time.Instant

class ResilientToolExecutor(
    private val retryPolicy: ToolRetryPolicy = ToolRetryPolicy(),
    private val telemetry: ToolExecutionTelemetry = ToolExecutionTelemetry(),
    private val now: () -> Instant = Instant::now,
    private val delay: suspend (Long) -> Unit = {}
) {
    suspend fun execute(tool: MayraTool, invocation: ToolInvocation): ToolResult {
        var attempt = 1
        while (true) {
            val startedAt = now()
            val result = try {
                tool.execute(invocation).copy(toolId = tool.manifest.id)
            } catch (error: Throwable) {
                ToolResult(
                    toolId = tool.manifest.id,
                    status = ToolExecutionStatus.FAILED,
                    errorCode = "tool_execution_failed",
                    metadata = mapOf("exception" to (error::class.simpleName ?: "Throwable"))
                )
            }
            val completedAt = now()
            telemetry.record(
                ToolExecutionMetric(
                    toolId = tool.manifest.id,
                    status = result.status,
                    attempt = attempt,
                    startedAt = startedAt,
                    completedAt = completedAt,
                    errorCode = result.errorCode
                )
            )

            if (!retryPolicy.shouldRetry(attempt, result)) {
                return result.copy(metadata = result.metadata + ("attempts" to attempt.toString()))
            }

            attempt += 1
            val waitMillis = retryPolicy.delayBeforeAttempt(attempt)
            if (waitMillis > 0) delay(waitMillis)
        }
    }

    fun telemetrySnapshot(toolId: String? = null): List<ToolExecutionMetric> =
        telemetry.snapshot(toolId)

    fun telemetrySummary(toolId: String): ToolExecutionSummary = telemetry.summarize(toolId)
}
