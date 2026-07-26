package ai.mayra.app.document

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class MayraDocumentContentStoreTest {
    private lateinit var context: Context
    private lateinit var store: MayraDocumentContentStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_document_content", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        store = MayraDocumentContentStore(context)
    }

    @Test
    fun removeExceptKeepsActiveIndexesAndDeletesOrphans() {
        val retainedUri = "content://documents/active"
        val orphanedUri = "content://documents/removed"
        store.put(retainedUri, "active document text", truncated = false)
        store.put(orphanedUri, "orphaned document text", truncated = true)

        val removed = store.removeExcept(setOf(retainedUri))

        assertEquals(1, removed)
        assertNotNull(store.get(retainedUri))
        assertNull(store.get(orphanedUri))
    }

    @Test
    fun removeExceptIsIdempotent() {
        val uri = "content://documents/active"
        store.put(uri, "active document text", truncated = false)

        assertEquals(0, store.removeExcept(setOf(uri)))
        assertEquals(0, store.removeExcept(setOf(uri)))
        assertNotNull(store.get(uri))
    }
}
