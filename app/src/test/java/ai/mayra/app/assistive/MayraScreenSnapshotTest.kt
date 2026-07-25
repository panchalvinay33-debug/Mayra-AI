package ai.mayra.app.assistive

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MayraScreenSnapshotTest {
    @Test
    fun `fresh screen context remains available for thirty seconds`() {
        val snapshot = MayraScreenSnapshot(
            packageName = "com.example",
            windowTitle = "Example",
            visibleText = listOf("Visible text"),
            capturedAt = 1_000L
        )

        assertTrue(snapshot.isFresh(now = 31_000L))
        assertFalse(snapshot.isFresh(now = 31_001L))
    }

    @Test
    fun `future timestamp is treated as fresh instead of crashing`() {
        val snapshot = MayraScreenSnapshot(
            packageName = "com.example",
            windowTitle = null,
            visibleText = emptyList(),
            capturedAt = 10_000L
        )

        assertTrue(snapshot.isFresh(now = 9_000L))
    }
}
