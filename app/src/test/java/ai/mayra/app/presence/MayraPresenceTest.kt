package ai.mayra.app.presence

import ai.mayra.app.voice.VoiceState
import ai.mayra.app.voice.VoiceTransportState
import kotlin.test.Test
import kotlin.test.assertEquals

class MayraPresenceTest {
    @Test
    fun attention_has_highest_priority() {
        val state = mayraPresenceState(
            assistantThinking = true,
            voiceState = VoiceState(isListening = true),
            pendingAttention = true
        )

        assertEquals(MayraPresenceState.NEEDS_ATTENTION, state)
    }

    @Test
    fun speaking_beats_listening_and_thinking() {
        val state = mayraPresenceState(
            assistantThinking = true,
            voiceState = VoiceState(isListening = true, isSpeaking = true)
        )

        assertEquals(MayraPresenceState.SPEAKING, state)
    }

    @Test
    fun listening_maps_to_listening_presence() {
        assertEquals(
            MayraPresenceState.LISTENING,
            mayraPresenceState(false, VoiceState(isListening = true))
        )
    }

    @Test
    fun processing_maps_to_understanding_presence() {
        assertEquals(
            MayraPresenceState.UNDERSTANDING,
            mayraPresenceState(
                assistantThinking = false,
                voiceState = VoiceState(transportState = VoiceTransportState.PROCESSING)
            )
        )
    }

    @Test
    fun assistant_busy_maps_to_thinking_presence() {
        assertEquals(
            MayraPresenceState.THINKING,
            mayraPresenceState(true, VoiceState())
        )
    }

    @Test
    fun unavailable_online_provider_remains_offline_ready() {
        assertEquals(
            MayraPresenceState.OFFLINE,
            mayraPresenceState(false, VoiceState(), onlineProviderReady = false)
        )
    }

    @Test
    fun proactive_greeting_uses_name_and_day_part() {
        assertEquals(
            "Good morning, Vinay. I’m awake and ready.",
            proactiveGreeting("  Vinay  ", 8)
        )
        assertEquals(
            "Good evening. I’m awake and ready.",
            proactiveGreeting("", 19)
        )
    }
}
