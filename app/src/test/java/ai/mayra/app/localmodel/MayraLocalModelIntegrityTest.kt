package ai.mayra.app.localmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MayraLocalModelIntegrityTest {
    @Test
    fun acceptsOnlyLitertlmFilenameSuffix() {
        assertTrue(MayraLocalModelIntegrity.isLiteRtLmName("Gemma3-1B.litertlm"))
        assertTrue(MayraLocalModelIntegrity.isLiteRtLmName("model.LITERTLM"))
        assertFalse(MayraLocalModelIntegrity.isLiteRtLmName("model.task"))
        assertFalse(MayraLocalModelIntegrity.isLiteRtLmName("model.litertlm.zip"))
        assertFalse(MayraLocalModelIntegrity.isLiteRtLmName(""))
    }

    @Test
    fun storageHeadroomRejectsTightOrInvalidCapacity() {
        val model = 557L * 1024L * 1024L
        val headroom = 256L * 1024L * 1024L
        assertTrue(MayraLocalModelIntegrity.hasEnoughStorage(model + headroom, model, headroom))
        assertFalse(MayraLocalModelIntegrity.hasEnoughStorage(model + headroom - 1L, model, headroom))
        assertFalse(MayraLocalModelIntegrity.hasEnoughStorage(-1L, model, headroom))
        assertFalse(MayraLocalModelIntegrity.hasEnoughStorage(model + headroom, 0L, headroom))
        assertFalse(MayraLocalModelIntegrity.hasEnoughStorage(Long.MAX_VALUE, Long.MAX_VALUE, 1L))
    }

    @Test
    fun sha256MatchesKnownVectorAndFile() {
        val bytes = "abc".toByteArray()
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        assertEquals(expected, MayraLocalModelIntegrity.digestHex(bytes))

        val file = File.createTempFile("mayra-model", ".litertlm")
        try {
            file.writeBytes(bytes)
            assertEquals(expected, MayraLocalModelIntegrity.sha256(file))
        } finally {
            file.delete()
        }
    }
}
