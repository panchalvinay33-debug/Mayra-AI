package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Duration
import java.time.Instant

data class PromptCacheEntry(
    val key: String,
    val response: LlmResponse,
    val createdAt: Instant,
    val expiresAt: Instant
)

class PromptCache(
    private val maxEntries: Int = 100,
    private val ttl: Duration = Duration.ofMinutes(10),
    private val clock: Clock = Clock.systemUTC()
) {
    private val entries = linkedMapOf<String, PromptCacheEntry>()

    init {
        require(maxEntries > 0) { "Maximum cache entries must be positive." }
        require(!ttl.isNegative && !ttl.isZero) { "Cache TTL must be positive." }
    }

    @Synchronized
    fun put(key: String, response: LlmResponse): PromptCacheEntry {
        val normalizedKey = key.trim()
        require(normalizedKey.isNotBlank()) { "Cache key cannot be blank." }
        purgeExpired()
        entries.remove(normalizedKey)
        val now = clock.instant()
        val entry = PromptCacheEntry(normalizedKey, response, now, now.plus(ttl))
        entries[normalizedKey] = entry
        trimToSize()
        return entry
    }

    @Synchronized
    fun get(key: String): LlmResponse? {
        purgeExpired()
        val normalizedKey = key.trim()
        val entry = entries.remove(normalizedKey) ?: return null
        entries[normalizedKey] = entry
        return entry.response
    }

    @Synchronized
    fun remove(key: String): Boolean = entries.remove(key.trim()) != null

    @Synchronized
    fun clear() = entries.clear()

    @Synchronized
    fun size(): Int {
        purgeExpired()
        return entries.size
    }

    @Synchronized
    fun snapshot(): List<PromptCacheEntry> {
        purgeExpired()
        return entries.values.toList()
    }

    private fun purgeExpired() {
        val now = clock.instant()
        entries.entries.removeIf { !it.value.expiresAt.isAfter(now) }
    }

    private fun trimToSize() {
        while (entries.size > maxEntries) {
            entries.remove(entries.keys.first())
        }
    }
}
