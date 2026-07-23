package ai.mayra.app.core.actions

import android.app.Application
import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AndroidDeviceActionRunnerTest {
    @Test
    fun callIntentIsBuiltAndStarted() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "+91 98765 43210")

        val output = runner.run(request)
        val intent = requireNotNull(captured)

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_CALL, intent.action)
        assertEquals("tel:+91 98765 43210", intent.dataString)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals("Started call to +91 98765 43210.", output)
    }

    @Test
    fun messageIntentPreservesRecipientAndBody() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )

        runner.run(request(DeviceActionType.SEND_MESSAGE, "9876543210", "  Hello Mayra  "))
        val intent = requireNotNull(captured)

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_SENDTO, intent.action)
        assertEquals("smsto:9876543210", intent.dataString)
        assertEquals(
            "Hello Mayra",
            intent.getStringExtra(AndroidDeviceActionSpecFactory.EXTRA_TEXT)
        )
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
    fun reminderIntentMapsTitleAndDetailExtras() = runBlocking {
        var captured: Intent? = null
        val runner = AndroidDeviceActionRunner(
            starter = AndroidIntentStarter { captured = it }
        )

        runner.run(request(DeviceActionType.CREATE_REMINDER, "Medicine", "At 8 PM"))
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
