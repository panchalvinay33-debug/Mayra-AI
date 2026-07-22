package ai.mayra.app.core.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimePermissionResolverTest {
    @Test
    fun `notifications require runtime permission on android 13 and newer`() {
        val resolver = RuntimePermissionResolver.android(apiLevel = 33)

        val denied = resolver.resolve(DeviceCapability.SHOW_NOTIFICATION, emptySet())
        val granted = resolver.resolve(
            DeviceCapability.SHOW_NOTIFICATION,
            setOf(RuntimePermissionResolver.POST_NOTIFICATIONS)
        )

        assertFalse(denied.isGranted)
        assertEquals(setOf(RuntimePermissionResolver.POST_NOTIFICATIONS), denied.missing)
        assertTrue(granted.isGranted)
    }

    @Test
    fun `notifications need no runtime permission before android 13`() {
        val resolver = RuntimePermissionResolver.android(apiLevel = 32)

        assertTrue(resolver.canExecute(DeviceCapability.SHOW_NOTIFICATION, emptySet()))
    }

    @Test
    fun `unmapped capabilities are executable without runtime permission`() {
        val resolver = RuntimePermissionResolver.android(apiLevel = 35)

        assertTrue(resolver.canExecute(DeviceCapability.OPEN_URL, emptySet()))
        assertTrue(resolver.canExecute(DeviceCapability.CLIPBOARD_WRITE, emptySet()))
    }
}
