package ai.mayra.app.chat

import ai.mayra.app.memory.PersonalMemoryAwareMayraAssistant
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraReplyMetadataParserTest {
    @Test fun extractsUnicodeKeysAndRemovesMarkerFromVisibleText() {
        val keys = listOf("favorite tea", "पसंदीदा भाषा")
        val payload = keys.joinToString(",") {
            Base64.getUrlEncoder().withoutPadding().encodeToString(it.toByteArray(Charsets.UTF_8))
        }
        val parsed = MayraReplyMetadataParser.parse(
            "Masala chai.\n${PersonalMemoryAwareMayraAssistant.USAGE_MARKER}$payload${PersonalMemoryAwareMayraAssistant.USAGE_SUFFIX}"
        )
        assertEquals("Masala chai.", parsed.text)
        assertEquals(keys, parsed.usedPersonalMemoryKeys)
    }

    @Test fun malformedMetadataNeverLeaksAsTrustedProvenance() {
        val parsed = MayraReplyMetadataParser.parse(
            "Answer\n${PersonalMemoryAwareMayraAssistant.USAGE_MARKER}not-valid${PersonalMemoryAwareMayraAssistant.USAGE_SUFFIX}"
        )
        assertEquals("Answer", parsed.text)
        assertTrue(parsed.usedPersonalMemoryKeys.isEmpty())
    }

    @Test fun ordinaryReplyIsUnchanged() {
        val parsed = MayraReplyMetadataParser.parse("Hello")
        assertEquals("Hello", parsed.text)
        assertTrue(parsed.usedPersonalMemoryKeys.isEmpty())
    }
}
