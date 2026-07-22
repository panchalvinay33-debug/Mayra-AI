package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Duration
import java.time.Instant

data class LlmProviderHealth(
    val providerId: String,
    val successes: Long,
    val failures: Long,
    val consecutiveFailures: Int,
    val lastSuccessAt: Instant?,
    val lastFailureAt: Instant?,
    val unhealthyUntil: Instant?
) {
    fun isHealthy(now: Instant): Boolean = unhealthyUntil?.isAfter(now) != true
}

/** Thread-safe health registry with a simple circuit-breaker cooldown. */
class LlmProviderHealthTracker(
    private val failureThreshold: Int = 3,
    private val cooldown: Duration = Duration.ofMinutes(1),
    private val clock: Clock = Clock.systemUTC()
) {
    private data class MutableHealth(
        var successes: Long = 0,
        var failures: Long = 0,
        var consecutiveFailures: Int = 0,
        var lastSuccessAt: Instant? = null,
        var lastFailureAt: Instant? = null,
        var unhealthyUntil: Instant? = null
    )

    private val states = linkedMapOf<String, MutableHealth>()

    init {
        require(failureThreshold > 0) { "Failure threshold must be positive." }
        require(!cooldown.isNegative && !cooldown.isZero) { "Cooldown must be positive." }
    }

    @Synchronized
    fun recordSuccess(providerId: String) {
        val id = normalize(providerId)
        val state = states.getOrPut(id) { MutableHealth() }
        state.successes += 1
        state.consecutiveFailures = 0
        state.lastSuccessAt = clock.instant()
        state.unhealthyUntil = null
    }

    @Synchronized
    fun recordFailure(providerId: String) {
        val id = normalize(providerId)
        val state = states.getOrPut(id) { MutableHealth() }
        val now = clock.instant()
        state.failures += 1
        state.consecutiveFailures += 1
        state.lastFailureAt = now
        if (state.consecutiveFailures >= failureThreshold) {
            state.unhealthyUntil = now.plus(cooldown)
        }
    }

    @Synchronized
    fun isHealthy(providerId: String): Boolean {
        val id = normalize(providerId)
        val state = states[id] ?: return true
        val now = clock.instant()
        if (state.unhealthyUntil?.isAfter(now) == true) return false
        if (state.unhealthyUntil != null) {
            state.unhealthyUntil = null
            state.consecutiveFailures = 0
        }
        return true
    }

    @Synchronized
    fun snapshot(providerId: String): LlmProviderHealth {
        val id = normalize(providerId)
        val state = states[id] ?: MutableHealth()
        return state.toSnapshot(id)
    }

    @Synchronized
    fun snapshots(): List<LlmProviderHealth> =
        states.map { (id, state) -> state.toSnapshot(id) }

    @Synchronized
    fun reset(providerId: String): Boolean = states.remove(normalize(providerId)) != null

    @Synchronized
    fun clear() = states.clear()

    private fun normalize(providerId: String): String = providerId.trim().also {
        require(it.isNotBlank()) { "Provider id cannot be blank." }
    }

    private fun MutableHealth.toSnapshot(providerId: String) = LlmProviderHealth(
        providerId = providerId,
        successes = successes,
        failures = failures,
        consecutiveFailures = consecutiveFailures,
        lastSuccessAt = lastSuccessAt,
        lastFailureAt = lastFailureAt,
        unhealthyUntil = unhealthyUntil
    )
}
