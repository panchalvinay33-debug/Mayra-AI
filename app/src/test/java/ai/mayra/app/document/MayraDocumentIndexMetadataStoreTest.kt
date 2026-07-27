package ai.mayra.app.document

import ai.mayra.app.TestMayraApplication
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = TestMayraApplication::class)
class MayraDocumentIndexMetadataStoreTest {
    private val context = ApplicationProvider.getApplicationContext<TestMayraApplication>()
    private lateinit var store: MayraDocumentIndexMetadataStore

    @Before
    fun setUp() {
        context.getSharedPreferences("mayra_document_index_metadata", 0).edit().clear().commit()
        store = MayraDocumentIndexMetadataStore(context)
    }

    @Test
    fun recordsAndReadsReadyParserFingerprint() {
        val document = document("content://docs/report", "report.pdf", "application/pdf", 2_048)

        assertTrue(store.record(document, recordedAt = 99L))
        assertEquals(
            DocumentIndexFingerprint("pdf", 1, 2_048, 99L),
            store.get(document.uri)
        )
        assertEquals(DocumentIndexState.CURRENT, store.state(document, hasIndexedContent = true))
    }

    @Test
    fun plannedParserIsNotRecorded() {
        val document = document("content://docs/old", "old.doc", "application/msword", 100)

        assertFalse(store.record(document))
        assertNull(store.get(document.uri))
        assertEquals(DocumentIndexState.UNSUPPORTED, store.state(document, hasIndexedContent = false))
    }

    @Test
    fun changedDocumentSizeBecomesStale() {
        val original = document("content://docs/report", "report.pdf", "application/pdf", 100)
        store.record(original, recordedAt = 1L)

        assertEquals(
            DocumentIndexState.STALE_SOURCE,
            store.state(original.copy(sizeBytes = 101), hasIndexedContent = true)
        )
    }

    @Test
    fun removeExceptDeletesOnlyOrphanedFingerprint() {
        val kept = document("content://docs/kept", "kept.txt", "text/plain", 10)
        val orphan = document("content://docs/orphan", "orphan.pdf", "application/pdf", 20)
        store.record(kept)
        store.record(orphan)

        assertEquals(1, store.removeExcept(setOf(kept.uri)))
        assertTrue(store.get(kept.uri) != null)
        assertNull(store.get(orphan.uri))
    }

    private fun document(uri: String, name: String, mime: String, size: Long) = MayraDocument(
        uri = uri,
        name = name,
        mimeType = mime,
        sizeBytes = size,
        addedAt = 1L
    )
}
