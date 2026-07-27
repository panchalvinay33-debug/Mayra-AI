package ai.mayra.app.document

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraCurrentIndexPolicyTest {
    private lateinit var context: Context
    private lateinit var contentStore: MayraDocumentContentStore
    private lateinit var metadataStore: MayraDocumentIndexMetadataStore
    private lateinit var policy: MayraCurrentIndexPolicy

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_document_content", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mayra_document_index_metadata", Context.MODE_PRIVATE).edit().clear().commit()
        contentStore = MayraDocumentContentStore(context)
        metadataStore = MayraDocumentIndexMetadataStore(context)
        policy = MayraCurrentIndexPolicy(contentStore, metadataStore)
    }

    @Test
    fun currentFingerprintAllowsEvidence() {
        val document = document(size = 100)
        contentStore.put(document.uri, "Payment is due within thirty days.", false)
        metadataStore.record(document, recordedAt = 10)

        assertEquals(DocumentIndexState.CURRENT, policy.state(document))
        assertTrue(policy.currentText(document).contains("thirty days"))
    }

    @Test
    fun legacyIndexIsBlockedUntilRefresh() {
        val document = document(size = 100)
        contentStore.put(document.uri, "Old searchable evidence must not leak.", false)

        assertEquals(DocumentIndexState.LEGACY, policy.state(document))
        assertTrue(policy.currentText(document).isEmpty())
    }

    @Test
    fun changedSourceSizeBlocksOldEvidence() {
        val original = document(size = 100)
        contentStore.put(original.uri, "Outdated source content.", false)
        metadataStore.record(original, recordedAt = 10)
        val changed = original.copy(sizeBytes = 200)

        assertEquals(DocumentIndexState.STALE_SOURCE, policy.state(changed))
        assertTrue(policy.currentText(changed).isEmpty())
    }

    @Test
    fun missingAndUnsupportedDocumentsAreBlocked() {
        val missing = document(size = 100)
        val unsupported = missing.copy(name = "legacy.doc", mimeType = "application/msword")

        assertEquals(DocumentIndexState.MISSING, policy.state(missing))
        assertEquals(DocumentIndexState.UNSUPPORTED, policy.state(unsupported))
        assertNull(policy.currentContent(missing))
        assertNull(policy.currentContent(unsupported))
    }

    @Test
    fun groundedInsightsIgnoreLegacyText() {
        val document = document(size = 100)
        contentStore.put(document.uri, "The secret payment code is BLUE-42 and must remain grounded.", false)
        val insights = MayraDocumentInsights(
            documentStore = MayraDocumentStore(context),
            contentStore = contentStore,
            currentIndexPolicy = policy
        )

        assertNull(insights.summarize(document))
        assertTrue(insights.answer("What is the payment code?", listOf(document)).isEmpty())
    }

    private fun document(size: Long) = MayraDocument(
        uri = "content://docs/current-policy",
        name = "invoice.txt",
        mimeType = "text/plain",
        sizeBytes = size,
        addedAt = 1L
    )
}
