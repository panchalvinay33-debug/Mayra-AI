package ai.mayra.app.runtime

import ai.mayra.app.TestMayraApplication
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class RuntimeNotificationPermissionTest {
    @Test
    fun `pre Android 13 with enabled notifications is ready`() {
        assertEquals(
            RuntimeNotificationReadiness.READY,
            classifyNotificationReadiness(
                requiresRuntimePermission = false,
                runtimePermissionGranted = false,
                notificationsEnabled = true
            )
        )
    }

    @Test
    fun `Android 13 missing runtime permission requires permission`() {
        assertEquals(
            RuntimeNotificationReadiness.PERMISSION_REQUIRED,
            classifyNotificationReadiness(
                requiresRuntimePermission = true,
                runtimePermissionGranted = false,
                notificationsEnabled = false
            )
        )
    }

    @Test
    fun `granted permission with disabled system notifications is blocked`() {
        assertEquals(
            RuntimeNotificationReadiness.SYSTEM_BLOCKED,
            classifyNotificationReadiness(
                requiresRuntimePermission = true,
                runtimePermissionGranted = true,
                notificationsEnabled = false
            )
        )
    }

    @Test
    fun `scan blocked messages explain exact recovery`() {
        assertEquals(
            "Grant notification permission before scanning.",
            notificationScanBlockedMessage(RuntimeNotificationReadiness.PERMISSION_REQUIRED)
        )
        assertEquals(
            "Enable notifications in system settings before scanning.",
            notificationScanBlockedMessage(RuntimeNotificationReadiness.SYSTEM_BLOCKED)
        )
    }

    @Test
    fun `notification settings intent targets current package`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = notificationSettingsIntent(context)

        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }
}
