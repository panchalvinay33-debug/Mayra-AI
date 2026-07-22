package ai.mayra.app.core.device

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceActionDispatcherTest {
    @Test
    fun `unsupported capability is rejected before handler`() = runTest {
        var calls = 0
        val dispatcher = DeviceActionDispatcher(
            AndroidCapabilityRegistry(),
            DeviceActionPolicy(),
            DeviceActionHandler { _, _ ->
                calls++
                DeviceActionResult(DeviceActionStatus.SUCCESS)
            }
        )

        val result = dispatcher.dispatch(
            DeviceAction.OpenUrl("https://example.com"),
            DeviceActionContext("s1")
        )

        assertEquals(DeviceActionStatus.UNSUPPORTED, result.status)
        assertEquals(0, calls)
    }

    @Test
    fun `invalid url is rejected before policy and execution`() = runTest {
        val dispatcher = DeviceActionDispatcher(
            AndroidCapabilityRegistry(setOf(DeviceCapability.OPEN_URL)),
            DeviceActionPolicy(),
            DeviceActionHandler { _, _ -> DeviceActionResult(DeviceActionStatus.SUCCESS) }
        )

        val result = dispatcher.dispatch(
            DeviceAction.OpenUrl("javascript:alert(1)"),
            DeviceActionContext("s1")
        )

        assertEquals(DeviceActionStatus.INVALID_REQUEST, result.status)
        assertEquals("invalid_device_action", result.errorCode)
    }

    @Test
    fun `valid action reaches handler`() = runTest {
        val dispatcher = DeviceActionDispatcher(
            AndroidCapabilityRegistry(setOf(DeviceCapability.SHARE_TEXT)),
            DeviceActionPolicy(),
            DeviceActionHandler { action, _ ->
                DeviceActionResult(
                    DeviceActionStatus.SUCCESS,
                    metadata = mapOf("capability" to action.capability.name)
                )
            }
        )

        val result = dispatcher.dispatch(
            DeviceAction.ShareText("hello"),
            DeviceActionContext("s1")
        )

        assertTrue(result.isSuccess)
        assertEquals("SHARE_TEXT", result.metadata["capability"])
    }

    @Test
    fun `handler exception becomes structured failure`() = runTest {
        val dispatcher = DeviceActionDispatcher(
            AndroidCapabilityRegistry(setOf(DeviceCapability.CLIPBOARD_WRITE)),
            DeviceActionPolicy(),
            DeviceActionHandler { _, _ -> error("bridge unavailable") }
        )

        val result = dispatcher.dispatch(
            DeviceAction.CopyToClipboard("hello"),
            DeviceActionContext("s1")
        )

        assertEquals(DeviceActionStatus.FAILED, result.status)
        assertEquals("device_action_exception", result.errorCode)
        assertTrue(result.metadata.containsKey("exception"))
    }
}
