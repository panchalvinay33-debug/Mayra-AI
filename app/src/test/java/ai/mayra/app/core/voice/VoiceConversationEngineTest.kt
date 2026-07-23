package ai.mayra.app.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceConversationEngineTest {
    private val engine = VoiceConversationEngine()

    @Test
    fun completeTurnMovesThroughListeningProcessingSpeakingAndIdle() {
        val result = engine.run(
            events = listOf(
                VoiceConversationEvent.StartListening(timestamp = 1L),
                VoiceConversationEvent.PartialTranscript("  remind   me ", timestamp = 2L),
                VoiceConversationEvent.FinalTranscript(" remind me tomorrow ", timestamp = 3L),
                VoiceConversationEvent.ResponseReady(" Reminder created ", timestamp = 4L),
                VoiceConversationEvent.SpeechFinished(timestamp = 5L)
            )
        )

        assertEquals(VoiceConversationState.IDLE, result.state)
        assertEquals(1L, result.turnId)
        assertEquals("remind me tomorrow", result.finalTranscript)
        assertEquals("Reminder created", result.responseText)
        assertEquals(5L, result.updatedAt)
        assertNull(result.errorMessage)
    }

    @Test
    fun partialTranscriptIsNormalized() {
        val listening = engine.reduce(
            VoiceConversationSnapshot(),
            VoiceConversationEvent.StartListening(timestamp = 1L)
        )
        val result = engine.reduce(
            listening,
            VoiceConversationEvent.PartialTranscript("  call\n\tShiv   now ", timestamp = 2L)
        )

        assertEquals("call Shiv now", result.partialTranscript)
    }

    @Test(expected = IllegalArgumentException::class)
    fun finalTranscriptCannotArriveWhileIdle() {
        engine.reduce(
            VoiceConversationSnapshot(),
            VoiceConversationEvent.FinalTranscript("hello", timestamp = 1L)
        )
    }

    @Test
    fun failureCanRecoverIntoNewTurn() {
        val failed = engine.reduce(
            VoiceConversationSnapshot(),
            VoiceConversationEvent.Fail(" microphone unavailable ", timestamp = 1L)
        )
        val recovered = engine.reduce(
            failed,
            VoiceConversationEvent.StartListening(timestamp = 2L)
        )

        assertEquals(VoiceConversationState.ERROR, failed.state)
        assertEquals("microphone unavailable", failed.errorMessage)
        assertEquals(VoiceConversationState.LISTENING, recovered.state)
        assertEquals(1L, recovered.turnId)
        assertNull(recovered.errorMessage)
    }

    @Test
    fun pauseAndResumeReturnToIdle() {
        val paused = engine.reduce(
            VoiceConversationSnapshot(),
            VoiceConversationEvent.Pause(timestamp = 1L)
        )
        val resumed = engine.reduce(
            paused,
            VoiceConversationEvent.Resume(timestamp = 2L)
        )

        assertEquals(VoiceConversationState.PAUSED, paused.state)
        assertEquals(VoiceConversationState.IDLE, resumed.state)
    }

    @Test
    fun resetKeepsTurnCounterButClearsConversationData() {
        val active = VoiceConversationSnapshot(
            state = VoiceConversationState.ERROR,
            turnId = 7L,
            finalTranscript = "old request",
            responseText = "old response",
            errorMessage = "old error"
        )
        val reset = engine.reduce(active, VoiceConversationEvent.Reset(timestamp = 9L))

        assertEquals(VoiceConversationState.IDLE, reset.state)
        assertEquals(7L, reset.turnId)
        assertNull(reset.finalTranscript)
        assertNull(reset.responseText)
        assertNull(reset.errorMessage)
        assertEquals(9L, reset.updatedAt)
    }

    @Test
    fun configuredLengthLimitsAreEnforced() {
        val limitedEngine = VoiceConversationEngine(
            maxTranscriptLength = 5,
            maxResponseLength = 4
        )
        val result = limitedEngine.run(
            events = listOf(
                VoiceConversationEvent.StartListening(timestamp = 1L),
                VoiceConversationEvent.FinalTranscript("123456789", timestamp = 2L),
                VoiceConversationEvent.ResponseReady("abcdef", timestamp = 3L)
            )
        )

        assertEquals("12345", result.finalTranscript)
        assertEquals("abcd", result.responseText)
        assertTrue(result.state == VoiceConversationState.SPEAKING)
    }
}
