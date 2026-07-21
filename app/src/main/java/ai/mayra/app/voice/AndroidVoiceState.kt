package ai.mayra.app.voice

/**
 * Android framework-facing voice state. Kept separate from the app-level
 * [VoiceState] state machine so partial transcripts can be represented safely.
 */
data class AndroidVoiceState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val isFinalTranscript: Boolean = false,
    val isSpeaking: Boolean = false,
    val error: String? = null
)
