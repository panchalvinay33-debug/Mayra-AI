package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LlmRetryPolicyTest {

    @Test
    fun `backoff grows and respects maximum delay`() {
        val policy = LlmRetryPolicy(
            maxAttempts = 5,
            initialDelayMillis = 100,
            maxDelayMillis = 350,
            multiplier = 2.0
        )

        assertEquals(100, policy.delayBeforeAttempt(2))
        assertEquals(200, policy.delayBeforeAttempt(3))
        assertEquals(350, policy.delayBeforeAttempt(4))
        assertEquals(350, policy.delayBeforeAttempt(5))
    }

    @Test
    fun `retry predicate and attempt limit are both enforced`() {
        val policy = LlmRetryPolicy(
            maxAttempts = 3,
            retryable = { it is IllegalStateException }
        )

        assertTrue(policy.shouldRetry(1, IllegalStateException("temporary")))
        assertFalse(policy.shouldRetry(1, IllegalArgumentException("permanent")))
        assertFalse(policy.shouldRetry(3, IllegalStateException("too late")))
    }

    @Test
    fun `invalid configuration is rejected`() {
        assertFailsWith<IllegalArgumentException> { LlmRetryPolicy(maxAttempts = 0) }
        assertFailsWith<IllegalArgumentException> {
            LlmRetryPolicy(initialDelayMillis = 10, maxDelayMillis = 5)
        }
        assertFailsWith<IllegalArgumentException> { LlmRetryPolicy(multiplier = 0.5) }
    }
}
