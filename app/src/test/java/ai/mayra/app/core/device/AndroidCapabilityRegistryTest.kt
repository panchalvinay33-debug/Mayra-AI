package ai.mayra.app.core.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidCapabilityRegistryTest {
    @Test
    fun `registry supports deterministic lifecycle`() {
        val registry = AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_URL))

        assertTrue(registry.supports(DeviceCapability.OPEN_URL))
        assertTrue(registry.register(DeviceCapability.SHARE_TEXT))
        assertFalse(registry.register(DeviceCapability.SHARE_TEXT))
        assertEquals(
            setOf(DeviceCapability.OPEN_URL, DeviceCapability.SHARE_TEXT),
            registry.snapshot()
        )
        assertTrue(registry.unregister(DeviceCapability.OPEN_URL))
        assertFalse(registry.supports(DeviceCapability.OPEN_URL))
    }

    @Test
    fun `register all reports newly added capabilities`() {
        val registry = AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_URL))

        val added = registry.registerAll(
            listOf(DeviceCapability.OPEN_URL, DeviceCapability.CLIPBOARD_WRITE, DeviceCapability.SHARE_TEXT)
        )

        assertEquals(2, added)
        assertEquals(3, registry.snapshot().size)
    }
}
