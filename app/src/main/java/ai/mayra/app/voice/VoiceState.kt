package ai.mayra.app.voice

/**
 * UI-facing lifecycle for a voice interaction.
 */
sealed interface VoiceState {
    data object Idle : VoiceState
    data object Listening : VoiceState
    data object Processing : VoiceState
    data object Speaking : VoiceState
    data class Error(val message: String) : VoiceState
}
