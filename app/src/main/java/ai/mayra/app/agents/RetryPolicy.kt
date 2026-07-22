package ai.mayra.app.agents

import kotlinx.coroutines.delay

fun interface RetryDelayStrategy {
    fun delayMillis(attempt: Int): Long
}

data class RetryPolicy(
    val maxAttempts: Int = 1,
    val delayStrategy: RetryDelayStrategy = RetryDelayStrategy { 0L }
) {
    init {
        require(maxAttempts > 0) { "maxAttempts must be greater than zero" }
    }

    suspend fun waitBeforeRetry(attempt: Int) {
        val delayMillis = delayStrategy.delayMillis(attempt).coerceAtLeast(0L)
        if (delayMillis > 0L) delay(delayMillis)
    }

    companion object {
        val None = RetryPolicy(maxAttempts = 1)

        fun linear(maxAttempts: Int, delayMillis: Long): RetryPolicy = RetryPolicy(
            maxAttempts = maxAttempts,
            delayStrategy = RetryDelayStrategy { attempt -> delayMillis * attempt }
        )
    }
}
