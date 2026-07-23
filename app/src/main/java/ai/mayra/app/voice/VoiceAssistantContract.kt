package ai.mayra.app.voice

interface VoiceAssistantContract {
    fun startListening()
    fun stopListening()
    fun speak(text: String, listenAfter: Boolean = false)
    fun interruptSpeech(resumeListening: Boolean = true)
    fun setContinuousMode(enabled: Boolean)
    fun release()
}

enum class VoiceTransportState {
    IDLE,
    PREPARING,
    LISTENING,
    PROCESSING,
    SPEAKING,
    INTERRUPTED,
    ERROR,
    UNAVAILABLE
}

data class VoiceState(
    val transportState: VoiceTransportState = VoiceTransportState.IDLE,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val transcript: String = "",
    val partialTranscript: String = "",
    val isFinalTranscript: Boolean = false,
    val recognitionConfidence: Double = 0.0,
    val rmsDb: Float = 0f,
    val continuousMode: Boolean = false,
    val speechAvailable: Boolean = true,
    val ttsReady: Boolean = false,
    val lastUtteranceId: String? = null,
    val error: String? = null,
    val recoverableError: Boolean = false
)
