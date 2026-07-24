package ai.mayra.app.runtime

import kotlin.test.Test
import kotlin.test.assertEquals

class RuntimeNotificationControlsTest {
    @Test
    fun `scan message reports posted runtime alert`() {
        assertEquals("Runtime alert posted.", notificationScanMessage(true))
    }

    @Test
    fun `scan message reports no new alert`() {
        assertEquals("No new runtime alert to post.", notificationScanMessage(false))
    }
}
