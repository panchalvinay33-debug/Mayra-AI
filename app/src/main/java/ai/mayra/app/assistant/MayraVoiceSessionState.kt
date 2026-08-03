package ai.mayra.app.assistant

sealed interface MayraVoiceSessionState {
    data object Idle : MayraVoiceSessionState
    data object PermissionRequired : MayraVoiceSessionState
    data object OnDeviceUnavailable : MayraVoiceSessionState
    data object Preparing : MayraVoiceSessionState
    data object Listening : MayraVoiceSessionState
    data object Processing : MayraVoiceSessionState
    data class Partial(val text: String) : MayraVoiceSessionState
    data class Heard(val text: String) : MayraVoiceSessionState
    data class Error(val reason: String) : MayraVoiceSessionState

    fun primaryText(): String = when (this) {
        Idle -> "Ready"
        PermissionRequired -> "Microphone permission needed"
        OnDeviceUnavailable -> "On-device speech unavailable"
        Preparing -> "Preparing voice…"
        Listening -> "Listening…"
        Processing -> "Understanding…"
        is Partial -> text.ifBlank { "Listening…" }
        is Heard -> "Heard: $text"
        is Error -> reason
    }

    fun isListeningLike(): Boolean = when (this) {
        Preparing, Listening, Processing, is Partial -> true
        else -> false
    }
}
