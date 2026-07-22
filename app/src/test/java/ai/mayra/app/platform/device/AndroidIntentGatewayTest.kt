package ai.mayra.app.platform.device

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AndroidIntentGatewayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val gateway = AndroidIntentGateway(context)

    @Test
    fun `unsafe url scheme is rejected`() {
        assertFalse(gateway.canOpenUrl("javascript:alert(1)"))
        assertFalse(gateway.openUrl("file:///data/private.txt"))
    }

    @Test
    fun `invalid package name is rejected`() {
        assertFalse(gateway.canLaunchApp("not a package"))
        assertFalse(gateway.launchApp(""))
    }

    @Test
    fun `settings action starts Android settings intent`() {
        assertTrue(gateway.openSettings("wifi"))

        val application = context.applicationContext as Application
        val started = shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(Settings.ACTION_WIFI_SETTINGS, started.action)
    }

    @Test
    fun `share rejects blank text`() {
        assertFalse(gateway.shareText("   ", "Share"))
    }
}
