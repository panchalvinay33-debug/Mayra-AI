package ai.mayra.app.core.device

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CapabilityAwareDeviceActionHandlerTest {
    @Test
    fun `unsupported capability never reaches delegate`() = runTest {
        var called = false
        val handler = CapabilityAwareDeviceActionHandler(
            delegate = DeviceActionHandler { _, _ ->
                called = true
                DeviceActionResult(DeviceActionStatus.SUCCESS)
            },
            capabilities = AndroidCapabilityRegistry(),
            permissionResolver = RuntimePermissionResolver.android(35)
        )

        val result = handler.execute(
            DeviceAction.OpenUrl("https://example.com"),
            DeviceActionContext(sessionId = "session")
        )

        assertEquals(DeviceActionStatus.UNSUPPORTED, result.status)
        assertEquals("capability_unavailable", result.errorCode)
        assertFalse(called)
    }

    @Test
    fun `missing permission blocks notification execution`() = runTest {
        var called = false
        val handler = CapabilityAwareDeviceActionHandler(
            delegate = DeviceActionHandler { _, _ ->
                called = true
                DeviceActionResult(DeviceActionStatus.SUCCESS)
            },
            capabilities = AndroidCapabilityRegistry(setOf(DeviceCapability.SHOW_NOTIFICATION)),
            permissionResolver = RuntimePermissionResolver.android(35)
        )

        val result = handler.execute(
            DeviceAction.ShowNotification("Mayra", "Hello"),
            DeviceActionContext(sessionId = "session")
        )

        assertEquals(DeviceActionStatus.PERMISSION_DENIED, result.status)
        assertEquals(RuntimePermissionResolver.POST_NOTIFICATIONS, result.metadata["missingPermissions"])
        assertFalse(called)
    }

    @Test
    fun `supported and permitted action reaches delegate`() = runTest {
        var called = false
        val handler = CapabilityAwareDeviceActionHandler(
            delegate = DeviceActionHandler { _, _ ->
                called = true
                DeviceActionResult(DeviceActionStatus.SUCCESS, message = "opened")
            },
            capabilities = AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_URL)),
            permissionResolver = RuntimePermissionResolver.android(35)
        )

        val result = handler.execute(
            DeviceAction.OpenUrl("https://example.com"),
            DeviceActionContext(sessionId = "session")
        )

        assertTrue(called)
        assertTrue(result.isSuccess)
        assertEquals("opened", result.message)
    }
}
