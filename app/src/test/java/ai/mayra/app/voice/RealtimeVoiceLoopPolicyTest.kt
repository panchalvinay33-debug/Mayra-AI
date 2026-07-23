package ai.mayra.app.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeVoiceLoopPolicyTest {
    @Test
    fun `partial transcript is never submitted`() {
        val policy = RealtimeVoiceLoopPolicy()
        val decision = policy.onVoiceState(
            VoiceState(transcript = "mummy ko", partialTranscript = "mummy ko", isFinalTranscript = false),
            assistantBusy = false,
            now = 1_000L
        )

        assertNull(decision.submitTranscript)
        assertEquals("not_final", decision.ignoreReason)
    }

    @Test
    fun `final transcript is submitted once`() {
        val policy = RealtimeVoiceLoopPolicy()
        val voice = VoiceState(transcript = "Mummy ko call karo", isFinalTranscript = true)

        val first = policy.onVoiceState(voice, assistantBusy = false, now = 1_000L)
        val duplicate = policy.onVoiceState(voice, assistantBusy = false, now = 2_000L)

        assertEquals("Mummy ko call karo", first.submitTranscript)
        assertTrue(first.stopListening)
        assertNull(duplicate.submitTranscript)
        assertEquals("duplicate_transcript", duplicate.ignoreReason)
    }

    @Test
    fun `normalized duplicate punctuation is suppressed`() {
        val policy = RealtimeVoiceLoopPolicy()

        policy.onVoiceState(
            VoiceState(transcript = "Reminder lagao!", isFinalTranscript = true),
            assistantBusy = false,
            now = 1_000L
        )
        val duplicate = policy.onVoiceState(
            VoiceState(transcript = " reminder   lagao ", isFinalTranscript = true),
            assistantBusy = false,
            now = 3_000L
        )

        assertEquals("duplicate_transcript", duplicate.ignoreReason)
    }

    @Test
    fun `same transcript is accepted after duplicate window`() {
        val policy = RealtimeVoiceLoopPolicy(duplicateWindowMs = 5_000L)
        val voice = VoiceState(transcript = "Weather batao", isFinalTranscript = true)

        policy.onVoiceState(voice, assistantBusy = false, now = 1_000L)
        val later = policy.onVoiceState(voice, assistantBusy = false, now = 7_000L)

        assertEquals("Weather batao", later.submitTranscript)
    }

    @Test
    fun `assistant busy blocks transcript submission`() {
        val policy = RealtimeVoiceLoopPolicy()
        val decision = policy.onVoiceState(
            VoiceState(transcript = "Mayra suno", isFinalTranscript = true),
            assistantBusy = true,
            now = 1_000L
        )

        assertNull(decision.submitTranscript)
        assertEquals("assistant_busy", decision.ignoreReason)
    }

    @Test
    fun `response is spoken once and continues listening`() {
        val policy = RealtimeVoiceLoopPolicy()

        val first = policy.onAssistantResponse("Ji, bolo.", "message-1", continuousMode = true)
        val duplicate = policy.onAssistantResponse("Ji, bolo.", "message-1", continuousMode = true)

        assertEquals("Ji, bolo.", first.speakResponse)
        assertTrue(first.listenAfterSpeech)
        assertNull(duplicate.speakResponse)
        assertEquals("response_already_spoken", duplicate.ignoreReason)
    }

    @Test
    fun `manual mode speaks without reopening microphone`() {
        val policy = RealtimeVoiceLoopPolicy()
        val decision = policy.onAssistantResponse("Theek hai.", "message-2", continuousMode = false)

        assertEquals("Theek hai.", decision.speakResponse)
        assertFalse(decision.listenAfterSpeech)
    }

    @Test
    fun `assistant failure restarts only continuous conversation`() {
        val policy = RealtimeVoiceLoopPolicy()

        assertTrue(policy.onAssistantFailure(true).startListening)
        assertFalse(policy.onAssistantFailure(false).startListening)
    }

    @Test
    fun `reset allows same response key and transcript again`() {
        val policy = RealtimeVoiceLoopPolicy()
        val voice = VoiceState(transcript = "Hello Mayra", isFinalTranscript = true)

        policy.onVoiceState(voice, assistantBusy = false, now = 1_000L)
        policy.onAssistantResponse("Hello", "one", continuousMode = true)
        policy.reset()

        assertEquals("Hello Mayra", policy.onVoiceState(voice, false, 2_000L).submitTranscript)
        assertEquals("Hello", policy.onAssistantResponse("Hello", "one", true).speakResponse)
    }
}
