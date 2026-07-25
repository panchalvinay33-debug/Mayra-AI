package ai.mayra.app.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraFileIndexPolicyTest {
    @Test
    fun `search ranks matching bill name and folder`() {
        val snapshot = MayraFileIndexSnapshot(files = listOf(
            file("content://one", "XYZ_Invoice_June.pdf", "Documents/Bills", 200L),
            file("content://two", "family_photo.jpg", "Pictures", 300L),
            file("content://three", "XYZ quotation.pdf", "Downloads", 100L)
        ))

        val results = snapshot.search("XYZ bill")

        assertEquals(2, results.size)
        assertEquals("XYZ_Invoice_June.pdf", results.first().displayName)
    }

    @Test
    fun `sensitive and private Android paths are excluded`() {
        assertFalse(MayraFilePrivacyPolicy.isAllowed("otp.txt", "Documents", "text/plain"))
        assertFalse(MayraFilePrivacyPolicy.isAllowed("cache.pdf", "Android/data/com.bank", "application/pdf"))
        assertTrue(MayraFilePrivacyPolicy.isAllowed("invoice.pdf", "Documents/Bills", "application/pdf"))
    }

    @Test
    fun `excluded records never appear in search`() {
        val snapshot = MayraFileIndexSnapshot(files = listOf(
            file("content://one", "XYZ bill.pdf", "Documents", 10L).copy(state = MayraIndexState.EXCLUDED)
        ))
        assertTrue(snapshot.search("XYZ").isEmpty())
    }

    private fun file(uri: String, name: String, folder: String, modified: Long) = MayraIndexedFile(
        uri = uri,
        displayName = name,
        mimeType = "application/pdf",
        sizeBytes = 100,
        modifiedAt = modified,
        sourceKind = MayraIndexedSourceKind.SAF_TREE,
        relativeLocation = folder,
        fingerprint = "$uri-$modified"
    )
}
