package ai.mayra.app.platform.device

import android.content.ClipboardManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidClipboardGatewayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `copy writes plain text to Android clipboard`() {
        val gateway = AndroidClipboardGateway(context)

        assertTrue(gateway.copy("Mayra test", "hello"))

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("hello", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        assertEquals("Mayra test", clipboard.primaryClipDescription?.label?.toString())
    }

    @Test
    fun `copy rejects empty text`() {
        assertFalse(AndroidClipboardGateway(context).copy("label", ""))
    }
}
