package ai.mayra.app.core.intelligence

import kotlin.math.min

/** Controls bounded retries for transient LLM generation failures. */
data class LlmRetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelayMillis: Long = 250,
    val maxDelayMillis: Long = 2_000,
    val multiplier: Double = 2.0,
    val retryable: (Throwable) -> Boolean = { true }
) {
    init {
        require(maxAttempts > 0) { "Maximum attempts must be positive." }
        require(initialDelayMillis >= 0) { "Initial delay cannot be negative." }
        require(maxDelayMillis >= initialDelayMillis) {
            "Maximum delay cannot be smaller than the initial delay."
        }
        require(multiplier >= 1.0) { "Retry multiplier must be at least 1." }
    }

    fun shouldRetry(attempt: Int, error: Throwable): Boolean =
        attempt < maxAttempts && retryable(error)

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
