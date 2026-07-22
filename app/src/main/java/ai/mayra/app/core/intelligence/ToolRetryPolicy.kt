package ai.mayra.app.core.intelligence

import kotlin.math.min

data class ToolRetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 150,
    val maxDelayMillis: Long = 1_500,
    val multiplier: Double = 2.0,
    val retryableErrorCodes: Set<String> = setOf(
        "tool_execution_failed",
        "temporary_failure",
        "timeout"
    )
) {
    init {
        require(maxAttempts > 0) { "Maximum attempts must be positive." }
        require(initialDelayMillis >= 0) { "Initial delay cannot be negative." }
        require(maxDelayMillis >= initialDelayMillis) {
            "Maximum delay cannot be smaller than initial delay."
        }
        require(multiplier >= 1.0) { "Retry multiplier must be at least 1." }
    }

    fun shouldRetry(attempt: Int, result: ToolResult): Boolean =
        attempt < maxAttempts &&
            result.status == ToolExecutionStatus.FAILED &&
            result.errorCode in retryableErrorCodes

    fun delayBeforeAttempt(attempt: Int): Long {
        require(attempt >= 2) { "Delay is defined only for retry attempts." }
        if (initialDelayMillis == 0L) return 0L

        var delay = initialDelayMillis.toDouble()
        repeat(attempt - 2) {
            delay = min(delay * multiplier, maxDelayMillis.toDouble())
        }
        return min(delay.toLong(), maxDelayMillis)
    }
}
