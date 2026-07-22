package ai.mayra.app.core.device

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformDeviceActionHandlerTest {
    @Test
    fun `launch app returns success when platform launches`() = runTest {
        val handler = handler(intents = FakeIntents(launchable = true, launchResult = true))

        val result = handler.execute(
            DeviceAction.LaunchApp("com.example.app"),
            DeviceActionContext("session")
        )

        assertEquals(DeviceActionStatus.SUCCESS, result.status)
    }

    @Test
    fun `launch app returns unsupported when package cannot resolve`() = runTest {
        val handler = handler(intents = FakeIntents(launchable = false))

        val result = handler.execute(
            DeviceAction.LaunchApp("missing.app"),
            DeviceActionContext("session")
        )

        assertEquals(DeviceActionStatus.UNSUPPORTED, result.status)
        assertEquals("app_not_found", result.errorCode)
    }

    @Test
    fun `open url reports missing handler`() = runTest {
        val handler = handler(intents = FakeIntents(urlSupported = false))

        val result = handler.execute(
            DeviceAction.OpenUrl("https://example.com"),
            DeviceActionContext("session")
        )

        assertEquals(DeviceActionStatus.UNSUPPORTED, result.status)
        assertEquals("url_handler_not_found", result.errorCode)
    }

    @Test
    fun `clipboard failure has structured code`() = runTest {
        val handler = handler(clipboard = DeviceClipboardGateway { _, _ -> false })

        val result = handler.execute(
            DeviceAction.CopyToClipboard("hello"),
            DeviceActionContext("session")
        )

        assertEquals(DeviceActionStatus.FAILED, result.status)
        assertEquals("clipboard_write_failed", result.errorCode)
    }

    @Test
    fun `platform exception becomes structured failure`() = runTest {
        val throwing = object : DeviceNotificationGateway {
            override fun show(channelId: String, title: String, message: String): Boolean {
                error("boom")
            }
        }
        val handler = handler(notifications = throwing)

        val result = handler.execute(
            DeviceAction.ShowNotification("Mayra", "Hello"),
            DeviceActionContext("session")
        )

        assertEquals(DeviceActionStatus.FAILED, result.status)
        assertEquals("platform_action_exception", result.errorCode)
        assertTrue(result.metadata.containsKey("exception"))
    }

    private fun handler(
        intents: DeviceIntentGateway = FakeIntents(),
        clipboard: DeviceClipboardGateway = DeviceClipboardGateway { _, _ -> true },
        notifications: DeviceNotificationGateway = DeviceNotificationGateway { _, _, _ -> true }
    ) = PlatformDeviceActionHandler(intents, clipboard, notifications)

    private class FakeIntents(
        private val launchable: Boolean = true,
        private val launchResult: Boolean = true,
        private val urlSupported: Boolean = true
    ) : DeviceIntentGateway {
        override fun canLaunchApp(packageName: String) = launchable
        override fun launchApp(packageName: String) = launchResult
        override fun canOpenUrl(url: String) = urlSupported
        override fun openUrl(url: String) = true
        override fun shareText(text: String, title: String?) = true
        override fun openSettings(section: String?) = true
    }
}
