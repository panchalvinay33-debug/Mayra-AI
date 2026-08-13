package ai.mayra.app.assistant

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class MayraVoiceSessionStateTest {
    @Test
    fun `listening-like states are bounded to active recognition phases`() {
        assertFalse(MayraVoiceSessionState.Idle.isListeningLike())
        assertTrue(MayraVoiceSessionState.Preparing.isListeningLike())
        assertTrue(MayraVoiceSessionState.Listening.isListeningLike())
        assertTrue(MayraVoiceSessionState.Processing.isListeningLike())
        assertTrue(MayraVoiceSessionState.Partial("hello").isListeningLike())
        assertFalse(MayraVoiceSessionState.Heard("hello").isListeningLike())
        assertFalse(MayraVoiceSessionState.Error("failed").isListeningLike())
    }

    @Test
    fun `heard state exposes transcript without pretending it was answered`() {
        assertEquals("Heard: namaste Mayra", MayraVoiceSessionState.Heard("namaste Mayra").primaryText())
    }

    @Test
    fun `permission and offline failures are explicit`() {
        assertEquals("Microphone permission needed", MayraVoiceSessionState.PermissionRequired.primaryText())
        assertEquals("On-device speech unavailable", MayraVoiceSessionState.OnDeviceUnavailable.primaryText())
    }

    @Test
    fun `blank partial stays in listening state`() {
        assertEquals("Listening…", MayraVoiceSessionState.Partial("").primaryText())
    }
}
