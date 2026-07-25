package ai.mayra.app.safety

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
class MayraGlobalStopStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_global_stop", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("mayra_global_stop", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `stop persists across store instances`() {
        val first = MayraGlobalStopStore(context)
        val stopped = first.stop("Emergency owner stop", now = 100L)
        val second = MayraGlobalStopStore(context)

        assertTrue(stopped.stopped)
        assertTrue(second.isStopped())
        assertEquals(100L, second.snapshot().changedAt)
        assertEquals(1L, second.snapshot().generation)
    }

    @Test
    fun `resume clears stop and advances generation`() {
        val store = MayraGlobalStopStore(context)
        store.stop(now = 100L)
        val resumed = store.resume(now = 200L)

        assertFalse(resumed.stopped)
        assertEquals(2L, resumed.generation)
        assertEquals(200L, resumed.changedAt)
    }

    @Test
    fun `reason is bounded and strips new lines`() {
        val store = MayraGlobalStopStore(context)
        val snapshot = store.stop("owner\nrequested " + "x".repeat(300), now = 1L)

        assertFalse(snapshot.reason.orEmpty().contains('\n'))
        assertTrue(snapshot.reason.orEmpty().length <= 160)
    }
}
