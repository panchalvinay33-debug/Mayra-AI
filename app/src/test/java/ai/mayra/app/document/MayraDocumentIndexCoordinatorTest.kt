package ai.mayra.app.document

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraDocumentIndexCoordinatorTest {
    private lateinit var context: Context
    private lateinit var contentStore: MayraDocumentContentStore
    private lateinit var metadataStore: MayraDocumentIndexMetadataStore
    private lateinit var coordinator: MayraDocumentIndexCoordinator

    private val pdf = MayraDocument(
        uri = "content://documents/transaction.pdf",
        name = "transaction.pdf",
        mimeType = "application/pdf",
        sizeBytes = 2_048,
        addedAt = 1L
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_document_content", Context.MODE_PRIVATE)
            .edit().clear().commit()
        context.getSharedPreferences("mayra_document_index_metadata", Context.MODE_PRIVATE)
            .edit().clear().commit()
        contentStore = MayraDocumentContentStore(context)
        metadataStore = MayraDocumentIndexMetadataStore(context)
        coordinator = MayraDocumentIndexCoordinator(contentStore, metadataStore)
    }

    @Test
    fun successfulCommitWritesVerifiedCurrentIndex() {
        val result = coordinator.commit(pdf, "Payment terms are thirty days.", truncated = false)

        assertTrue(result is DocumentIndexCommitResult.Success)
        assertNotNull(contentStore.get(pdf.uri))
        assertNotNull(metadataStore.get(pdf.uri))
        assertEquals(
            DocumentIndexState.CURRENT,
            metadataStore.state(pdf, hasIndexedContent = true)
        )
    }

    @Test
    fun blankCommitRemovesAnyPreviousPartialState() {
        contentStore.put(pdf.uri, "old partial text", truncated = false)
        metadataStore.record(pdf)

        val result = coordinator.commit(pdf, "   \n\t ", truncated = false)

        assertTrue(result is DocumentIndexCommitResult.Failure)
        assertNull(contentStore.get(pdf.uri))
        assertNull(metadataStore.get(pdf.uri))
    }

    @Test
    fun unsupportedDocumentCannotLeaveSearchablePartialState() {
        val legacyDoc = pdf.copy(
            uri = "content://documents/old.doc",
            name = "old.doc",
            mimeType = "application/msword"
        )
        contentStore.put(legacyDoc.uri, "stale binary-doc text", truncated = false)

        val result = coordinator.commit(legacyDoc, "stale binary-doc text", truncated = false)

        assertTrue(result is DocumentIndexCommitResult.Failure)
        assertNull(contentStore.get(legacyDoc.uri))
        assertNull(metadataStore.get(legacyDoc.uri))
    }

    @Test
    fun explicitRemoveDeletesContentAndFingerprintTogether() {
        assertTrue(coordinator.commit(pdf, "Indexed local evidence.", false) is DocumentIndexCommitResult.Success)

        coordinator.remove(pdf.uri)

        assertNull(contentStore.get(pdf.uri))
        assertNull(metadataStore.get(pdf.uri))
    }
}
