package ai.mayra.app.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.mayra.app.brain.AIManager
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.voice.VoiceSessionManager
import ai.mayra.app.voice.VoiceSessionResult
import ai.mayra.app.voice.VoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val aiManager: AIManager = AIManager(),
    private val voiceSessionManager: VoiceSessionManager = VoiceSessionManager()
) : ViewModel() {
    private val conversationId = "main-chat"
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) = _uiState.update { it.copy(input = value, error = null) }

    fun sendMessage() {
        submitMessage(_uiState.value.input)
    }

    fun startVoiceListening() {
        updateVoiceState(voiceSessionManager.startListening())
    }

    fun stopVoiceListening() {
        updateVoiceState(voiceSessionManager.stopListening())
    }

    /**
     * Entry point for Android SpeechRecognizer final results.
     */
    fun onVoiceTranscript(rawText: String) {
        updateVoiceState(VoiceState.Processing)

        when (val result = voiceSessionManager.processTranscript(rawText)) {
            is VoiceSessionResult.Command -> submitMessage(result.text)
            is VoiceSessionResult.Ignored -> {
                _uiState.update {
                    it.copy(
                        voiceState = voiceSessionManager.currentState(),
                        error = result.reason
                    )
                }
            }
        }
    }

    fun onVoiceError(message: String) {
        val errorState = voiceSessionManager.fail(message)
        _uiState.update { it.copy(voiceState = errorState, error = message) }
    }

    fun onSpeechStarted() {
        updateVoiceState(voiceSessionManager.beginSpeaking())
    }

    fun onSpeechFinished() {
        updateVoiceState(voiceSessionManager.finishSpeaking())
    }

    private fun submitMessage(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        val userMessage = MayraMessage(text, MayraMessage.Sender.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isThinking = true,
                voiceState = VoiceState.Processing,
                error = null
            )
        }

        viewModelScope.launch {
            val response = aiManager.replyTo(
                message = text,
                conversationId = conversationId,
                systemPrompt = "You are Mayra, a warm and practical personal AI assistant."
            )

            if (response.isSuccess) {
                _uiState.update {
                    it.copy(
                        messages = it.messages + MayraMessage(
                            response.text,
                            MayraMessage.Sender.MAYRA
                        ),
                        isThinking = false,
                        voiceState = VoiceState.Idle,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isThinking = false,
                        voiceState = VoiceState.Error(
                            response.errorMessage ?: response.text
                        ),
                        error = response.errorMessage ?: response.text
                    )
                }
            }
        }
    }

    private fun updateVoiceState(state: VoiceState) {
        _uiState.update { it.copy(voiceState = state, error = null) }
    }

    fun clearConversation() {
        aiManager.clearConversation(conversationId)
        voiceSessionManager.stopListening()
        _uiState.update { ChatUiState() }
    }
}
