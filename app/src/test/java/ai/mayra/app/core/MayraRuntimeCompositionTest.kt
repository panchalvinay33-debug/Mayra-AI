package ai.mayra.app.core

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MayraRuntimeCompositionTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences(
            MayraAndroidRuntimeComposition.ACTIVITY_PREFERENCES,
            Context.MODE_PRIVATE
        ).edit().clear().commit()
    }

    @Test
    fun answerAndCurrentDocumentHandlersAreRegistered() {
        val composition = MayraAndroidRuntimeComposition(
            context = context,
            answerProvider = MayraAnswerProvider { "answer:$it" },
            enableSafeFilePickerAction = false
        )

        val answer = composition.runtime.dispatch("Hello Mayra")
        assertTrue(answer is MayraRoutingRuntimeResult.Executed)
        assertEquals("answer:Hello Mayra", (answer as MayraRoutingRuntimeResult.Executed).output)

        val retrieval = composition.runtime.dispatch("Search my documents for Rahul")
        assertTrue(retrieval is MayraRoutingRuntimeResult.Executed)
        assertTrue((retrieval as MayraRoutingRuntimeResult.Executed).output.contains("Library is empty"))
        assertEquals(2, composition.activityLog.snapshot().size)
    }

    @Test
    fun safeFileManagerActionLaunchesSystemPickerWithoutPermission() {
        val composition = MayraAndroidRuntimeComposition(
            context = context,
            answerProvider = MayraAnswerProvider { "ok" }
        )

        val result = composition.runtime.dispatch("Open file manager")

        assertTrue(result is MayraRoutingRuntimeResult.Executed)
        val started = shadowOf(context as Application).nextStartedActivity
        assertNotNull(started)
        assertEquals(Intent.ACTION_OPEN_DOCUMENT, started.action)
        assertEquals("*/*", started.type)
        assertTrue(started.hasCategory(Intent.CATEGORY_OPENABLE))
    }

    @Test
    fun destructiveActionStillRequiresConfirmationAndIsNotSupportedBySafeExecutor() {
        val composition = MayraAndroidRuntimeComposition(
            context = context,
            answerProvider = MayraAnswerProvider { "ok" }
        )

        val pending = composition.runtime.dispatch("Delete file report.pdf")
        assertTrue(pending is MayraRoutingRuntimeResult.ConfirmationRequired)
        val confirmed = composition.runtime.confirmAndDispatch(
            message = "Delete file report.pdf",
            token = (pending as MayraRoutingRuntimeResult.ConfirmationRequired).token
        )
        assertTrue(confirmed is MayraRoutingRuntimeResult.Failed)
        assertTrue((confirmed as MayraRoutingRuntimeResult.Failed).reason.contains("not registered"))
    }
}
