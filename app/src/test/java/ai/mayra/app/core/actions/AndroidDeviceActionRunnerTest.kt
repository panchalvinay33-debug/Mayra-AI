package ai.mayra.app.core.actions

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AndroidDeviceActionRunnerTest {
    @Test
    fun callIntentOpensDialerWithoutPlacingCall() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "+91 98765 43210")

        val output = runner.run(request)
        val intent = requireNotNull(captured)

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_DIAL, intent.action)
        assertEquals("tel:+91 98765 43210", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(output.contains("owner must start the call"))
        assertFalse(output.contains("Started call"))
    }

    @Test
    fun messageIntentPreservesRecipientAndBodyForUserReview() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )

        val output = runner.run(request(DeviceActionType.SEND_MESSAGE, "9876543210", "  Hello\nMayra  "))
        val intent = requireNotNull(captured)

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_SENDTO, intent.action)
        assertEquals("smsto:9876543210", intent.dataString)
        assertEquals("Hello Mayra", intent.getStringExtra(AndroidDeviceActionSpecFactory.EXTRA_TEXT))
        assertTrue(output.contains("has not been sent"))
    }

    @Test
    fun dial target rejects URI injection`() {
        assertThrows(IllegalArgumentException::class.java) {
            AndroidDeviceActionSpecFactory.create(
                request(DeviceActionType.CALL_CONTACT, "12345?body=hidden")
            )
        }
    }

    @Test
    fun message target rejects scheme injection`() {
        assertThrows(IllegalArgumentException::class.java) {
            AndroidDeviceActionSpecFactory.create(
                request(DeviceActionType.SEND_MESSAGE, "smsto:12345", "Hello")
            )
        }
    }

    @Test
    fun dial target must contain a number`() {
        assertThrows(IllegalArgumentException::class.java) {
            AndroidDeviceActionSpecFactory.create(
                request(DeviceActionType.CALL_CONTACT, "Mummy")
            )
        }
    }

    @Test
    fun openAppIntentAppliesPackageAndLauncherCategory() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )
        val request = DeviceActionRequest(
            id = "open-youtube",
            type = DeviceActionType.OPEN_APP,
            target = "YouTube",
            createdAt = 100L,
            metadata = mapOf("packageName" to "com.google.android.youtube")
        )

        runner.run(request)
        val intent = requireNotNull(captured)

        assertEquals("com.google.android.youtube", intent.`package`)
        assertTrue(
            intent.categories.orEmpty()
                .contains(AndroidDeviceActionSpecFactory.CATEGORY_LAUNCHER)
        )
    }

    @Test
    fun invalidPackageNameFailsClosed() {
        assertThrows(IllegalArgumentException::class.java) {
            AndroidDeviceActionSpecFactory.create(
                DeviceActionRequest(
                    id = "bad-package",
                    type = DeviceActionType.OPEN_APP,
                    target = "Browser",
                    createdAt = 100L,
                    metadata = mapOf("packageName" to "com.example.app;evil")
                )
            )
        }
    }

    @Test
    fun reminderIntentMapsSanitizedTitleAndDetailExtras() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )

        runner.run(request(DeviceActionType.CREATE_REMINDER, "Medicine", "At\n8 PM"))
        val intent = requireNotNull(captured)

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_INSERT, intent.action)
        assertEquals("Medicine", intent.getStringExtra(AndroidDeviceActionSpecFactory.EXTRA_TITLE))
        assertEquals("At 8 PM", intent.getStringExtra(AndroidDeviceActionSpecFactory.EXTRA_TEXT))
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String? = null
    ) = DeviceActionRequest(
        id = "request-${type.name}",
        type = type,
        target = target,
        payload = payload,
        createdAt = 100L
    )
}
