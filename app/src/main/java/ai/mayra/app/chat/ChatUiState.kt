package ai.mayra.app.chat

import ai.mayra.app.core.MayraMessage
import ai.mayra.app.voice.VoiceState

data class ChatUiState(
    val messages: List<MayraMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    val voiceState: VoiceState = VoiceState.Idle,
    val error: String? = null
)
