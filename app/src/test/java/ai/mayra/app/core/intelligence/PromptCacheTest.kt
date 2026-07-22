package ai.mayra.app.core.intelligence

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PromptCacheTest {
    private val now = Instant.parse("2026-07-22T12:00:00Z")

    @Test
    fun `stores and retrieves response`() {
        val cache = PromptCache(clock = Clock.fixed(now, ZoneOffset.UTC))
        cache.put("hello", response("one"))
        assertEquals("one", cache.get("hello")?.content)
        assertEquals(1, cache.size())
    }

    @Test
    fun `evicts least recently used entry`() {
        val cache = PromptCache(maxEntries = 2, clock = Clock.fixed(now, ZoneOffset.UTC))
        cache.put("a", response("a"))
        cache.put("b", response("b"))
        cache.get("a")
        cache.put("c", response("c"))
        assertEquals("a", cache.get("a")?.content)
        assertNull(cache.get("b"))
        assertEquals("c", cache.get("c")?.content)
    }

    @Test
    fun `expired entry is removed`() {
        val writeClock = Clock.fixed(now, ZoneOffset.UTC)
        val cache = PromptCache(ttl = Duration.ofSeconds(1), clock = writeClock)
        cache.put("key", response("value"))

        val laterCache = PromptCache(ttl = Duration.ofSeconds(1), clock = Clock.fixed(now.plusSeconds(2), ZoneOffset.UTC))
        assertNull(laterCache.get("key"))
    }

    @Test
    fun `remove and clear update cache size`() {
        val cache = PromptCache(clock = Clock.fixed(now, ZoneOffset.UTC))
        cache.put("a", response("a"))
        cache.put("b", response("b"))
        cache.remove("a")
        assertEquals(1, cache.size())
        cache.clear()
        assertEquals(0, cache.size())
    }

    private fun response(content: String) = LlmResponse(providerId = "test", content = content, createdAt = now)
}
