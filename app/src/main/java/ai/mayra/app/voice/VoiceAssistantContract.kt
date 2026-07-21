package ai.mayra.app.voice

interface VoiceAssistantContract {
    fun startListening()
    fun stopListening()
    fun speak(text: String)
    fun release()
}

data class VoiceState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val error: String? = null
)
