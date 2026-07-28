package ai.mayra.app.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MayraRuntimePersistenceTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("runtime-persistence-test", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun persistentLogSurvivesNewInstanceAndPreservesUnicode() {
        val preferences = context.getSharedPreferences("runtime-persistence-test", Context.MODE_PRIVATE)
        val plan = MayraRoutingPolicy.routeAndPlan("मेरी PDF खोजो")
        val recorder = MayraActivityRecorder(MayraPersistentActivityLog(preferences), fixedClock())

        recorder.record(plan, MayraActivityStatus.EXECUTED, "हिंदी result ✓")

        val restored = MayraPersistentActivityLog(preferences).snapshot()
        assertEquals(1, restored.size)
        assertEquals("हिंदी result ✓", restored.single().detail)
        assertEquals(Instant.parse("2026-07-28T00:00:00Z"), restored.single().timestamp)
    }

    @Test
    fun persistentLogEnforcesBoundedRetention() {
        val preferences = context.getSharedPreferences("runtime-persistence-test", Context.MODE_PRIVATE)
        val log = MayraPersistentActivityLog(preferences, maxRecords = 2)
        val plan = MayraRoutingPolicy.routeAndPlan("Hello")
        val recorder = MayraActivityRecorder(log, fixedClock())

        recorder.record(plan, MayraActivityStatus.EXECUTED, "one")
        recorder.record(plan, MayraActivityStatus.EXECUTED, "two")
        recorder.record(plan, MayraActivityStatus.EXECUTED, "three")

        assertEquals(listOf("two", "three"), log.snapshot().map { it.detail })
    }

    @Test
    fun corruptRowsAreIgnoredAndClearRemovesHistory() {
        val preferences = context.getSharedPreferences("runtime-persistence-test", Context.MODE_PRIVATE)
        preferences.edit().putString("mayra.runtime.activity.v1", "broken-row").commit()
        val log = MayraPersistentActivityLog(preferences)

        assertTrue(log.snapshot().isEmpty())
        assertTrue(log.clear())
        assertTrue(log.snapshot().isEmpty())
    }

    @Test
    fun confirmationTokenIsBoundOneTimeAndReplaySafe() {
        val clock = fixedClock()
        val store = MayraConfirmationTokenStore(clock, Duration.ofMinutes(2))
        val message = "Delete file report.pdf"
        val decision = MayraQueryRouter.route(message)
        val token = store.issue(message, decision)

        assertEquals(MayraConfirmationResult.MISMATCH, store.consume(token.value, "Delete file other.pdf", decision))
        assertEquals(MayraConfirmationResult.ACCEPTED, store.consume(token.value, message, decision))
        assertEquals(MayraConfirmationResult.ALREADY_USED, store.consume(token.value, message, decision))
    }

    @Test
    fun expiredConfirmationTokenCannotExecute() {
        val issuedAt = Instant.parse("2026-07-28T00:00:00Z")
        val mutableClock = MutableClock(issuedAt)
        val store = MayraConfirmationTokenStore(mutableClock, Duration.ofSeconds(30))
        val message = "Delete file report.pdf"
        val decision = MayraQueryRouter.route(message)
        val token = store.issue(message, decision)

        mutableClock.now = issuedAt.plusSeconds(31)

        assertEquals(MayraConfirmationResult.EXPIRED, store.consume(token.value, message, decision))
    }

    @Test(expected = IllegalArgumentException::class)
    fun tokenCannotBeIssuedForSafeAction() {
        val decision = MayraQueryRouter.route("Open file manager")
        MayraConfirmationTokenStore(fixedClock()).issue("Open file manager", decision)
    }

    private fun fixedClock(): Clock = Clock.fixed(Instant.parse("2026-07-28T00:00:00Z"), ZoneOffset.UTC)

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant(): Instant = now
    }
}
