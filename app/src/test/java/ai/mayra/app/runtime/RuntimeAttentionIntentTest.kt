package ai.mayra.app.runtime

import ai.mayra.app.TestMayraApplication
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class RuntimeAttentionIntentTest {
    @Test
    fun `runtime alert opens dedicated internal control activity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = runtimeControlIntent(context)

        assertEquals(RuntimeControlActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }

    @Test
    fun `runtime alert action targets internal receiver`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = runtimeAttentionActionIntent(context, RuntimeAttentionActionReceiver.ACTION_SNOOZE)

        assertEquals(RuntimeAttentionActionReceiver::class.java.name, intent.component?.className)
        assertEquals(RuntimeAttentionActionReceiver.ACTION_SNOOZE, intent.action)
    }
}
