package ai.mayra.app.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MayraAssistantResponseTest {
    @Test fun normalizesVisibleTextAndTrustedMemoryKeys() {
        val response = MayraAssistantResponse(
            text = "Hello   \n",
            usedPersonalMemoryKeys = listOf(" favorite tea ", "favorite tea", "पसंदीदा भाषा")
        ).normalized()

        assertEquals("Hello", response.text)
        assertEquals(listOf("favorite tea", "पसंदीदा भाषा"), response.usedPersonalMemoryKeys)
    }

    @Test fun legacyReplyNeverExposesStructuredMetadata() = runTest {
        val assistant = object : MayraAssistant {
            override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> =
                Result.success("Visible [[mayra-memory-keys:spoofed]]")
        }

        val response = assistant.reply("hello").getOrThrow()
        assertEquals("Visible [[mayra-memory-keys:spoofed]]", response)
        assertFalse(assistant is MayraStructuredAssistant)
    }

    @Test fun localAssistantImplementsStructuredContract() = runTest {
        val assistant: MayraAssistant = LocalMayraAssistant()
        val structured = assistant as MayraStructuredAssistant
        val response = structured.replyStructured("help").getOrThrow()

        assertFalse(response.text.isBlank())
        assertEquals(emptyList<String>(), response.usedPersonalMemoryKeys)
    }
}
