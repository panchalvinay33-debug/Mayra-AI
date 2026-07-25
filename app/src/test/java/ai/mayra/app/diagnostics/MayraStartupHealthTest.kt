package ai.mayra.app.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraStartupHealthTest {
    private lateinit var context: Context
    private lateinit var health: MayraStartupHealth

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_startup_health", Context.MODE_PRIVATE).edit().clear().commit()
        health = MayraStartupHealth(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("mayra_startup_health", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `begin marks startup incomplete until complete is called`() {
        health.begin(now = 10L)
        val during = health.snapshot()
        assertTrue(during.previousStartInterrupted)

        health.complete(now = 20L)
        val after = health.snapshot()
        assertFalse(after.previousStartInterrupted)
        assertEquals(20L, after.lastCompletedAt)
    }

    @Test
    fun `safe step records bounded failure without throwing`() {
        health.begin(now = 1L)

        val passed = health.safeStep("phone pulse warmup") {
            error("OEM service unavailable")
        }

        assertFalse(passed)
        val snapshot = health.snapshot()
        assertTrue(snapshot.degraded)
        assertEquals(listOf("phone pulse warmup"), snapshot.failedSteps)
        assertEquals("IllegalStateException", snapshot.lastErrorType)
        assertTrue(snapshot.ownerSummary().contains("did not finish"))
    }

    @Test
    fun `successful step remains clean`() {
        health.begin(now = 1L)
        assertTrue(health.safeStep("memory prune") { Unit })
        health.complete(now = 2L)

        val snapshot = health.snapshot()
        assertFalse(snapshot.degraded)
        assertEquals("Mayra startup completed normally.", snapshot.ownerSummary())
    }
}
