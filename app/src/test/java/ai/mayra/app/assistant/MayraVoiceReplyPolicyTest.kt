package ai.mayra.app.assistant

import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraVoiceReplyPolicyTest {
    @Test
    fun `greeting gets a natural offline reply`() {
        val reply = MayraVoiceReplyPolicy.replyFor("Mayra namaste")
        assertTrue(reply.text.contains("Namaste"))
        assertFalse(reply.containsPrivateContent)
    }

    @Test
    fun `time answer is deterministic with supplied clock`() {
        val reply = MayraVoiceReplyPolicy.replyFor("time kya hua", LocalTime.of(7, 5))
        assertTrue(reply.text.contains("7:05 AM"))
    }

    @Test
    fun `app request is understood but not claimed executed`() {
        val reply = MayraVoiceReplyPolicy.replyFor("open WhatsApp")
        assertTrue(reply.text.contains("confirmation"))
        assertFalse(reply.text.contains("khol diya"))
    }

    @Test
    fun `unknown transcript is marked private`() {
        val reply = MayraVoiceReplyPolicy.replyFor("mera secret note")
        assertTrue(reply.containsPrivateContent)
        assertTrue(reply.text.contains("mera secret note"))
    }
}
