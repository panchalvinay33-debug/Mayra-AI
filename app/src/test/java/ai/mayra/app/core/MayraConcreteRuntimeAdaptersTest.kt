package ai.mayra.app.core

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraConcreteRuntimeAdaptersTest {
    private lateinit var context: Context

    @Before fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_documents", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_document_content", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_document_index_metadata", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun answerAdapterNormalizesBlankProviderOutput() {
        val handlers = MayraConcreteRuntimeAdapters.create(context, MayraAnswerProvider { "  " })
        val output = handlers.answer!!.handle("hello", MayraQueryRouter.route("hello"))
        assertTrue(output.contains("could not produce"))
    }

    @Test fun emptyDocumentLibraryReturnsDeterministicGroundedMessage() {
        val handlers = MayraConcreteRuntimeAdapters.create(context, MayraAnswerProvider { "answer" })
        val output = handlers.retrieve!!.handle("Search my documents", MayraQueryRouter.route("Search my documents"))
        assertTrue(output.contains("Library is empty"))
    }

    @Test fun actionAdapterDelegatesOnlyWhenExplicitlyProvided() {
        val withoutAction = MayraConcreteRuntimeAdapters.create(context, MayraAnswerProvider { "answer" })
        assertEquals(null, withoutAction.act)

        val withAction = MayraConcreteRuntimeAdapters.create(
            context,
            MayraAnswerProvider { "answer" },
            MayraDeviceActionExecutor { message, _ -> "done:$message" }
        )
        assertEquals("done:Open file manager", withAction.act!!.handle("Open file manager", MayraQueryRouter.route("Open file manager")))
    }
}
