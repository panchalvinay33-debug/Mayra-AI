package ai.mayra.app.core.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceCapabilityDetectorTest {
    @Test
    fun `detect separates supported and unsupported capabilities`() {
        val detector = DeviceCapabilityDetector(
            mapOf(
                DeviceCapability.APP_LAUNCH to { true },
                DeviceCapability.OPEN_URL to { false }
            )
        )

        val result = detector.detect()

        assertTrue(result.supports(DeviceCapability.APP_LAUNCH))
        assertFalse(result.supports(DeviceCapability.OPEN_URL))
        assertTrue(DeviceCapability.SHARE_TEXT in result.unsupported)
    }

    @Test
    fun `probe failures are isolated and reported`() {
        val detector = DeviceCapabilityDetector(
            mapOf(DeviceCapability.SHOW_NOTIFICATION to { error("broken runtime") })
        )

        val result = detector.detect()

        assertTrue(DeviceCapability.SHOW_NOTIFICATION in result.unsupported)
        assertEquals("IllegalStateException", result.failures[DeviceCapability.SHOW_NOTIFICATION])
    }

    @Test
    fun `refresh atomically replaces registry capabilities`() {
        val registry = AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_SETTINGS))
        val detector = DeviceCapabilityDetector(
            mapOf(
                DeviceCapability.APP_LAUNCH to { true },
                DeviceCapability.CLIPBOARD_WRITE to { true }
            )
        )

        detector.refresh(registry)

        assertEquals(
            setOf(DeviceCapability.APP_LAUNCH, DeviceCapability.CLIPBOARD_WRITE),
            registry.snapshot()
        )
    }
}
