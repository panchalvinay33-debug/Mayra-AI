package ai.mayra.app.runtime

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeAttentionIntentTest {
    @Test
    fun `runtime alert opens dedicated internal control activity`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val intent = runtimeControlIntent(context)

        assertEquals(RuntimeControlActivity::class.java.name, intent.component?.className)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
    }
}
