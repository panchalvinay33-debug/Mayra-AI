package ai.mayra.app.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.mayra.app.brain.AIManager
import ai.mayra.app.core.MayraMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val aiManager: AIManager = AIManager()
) : ViewModel() {
    private val conversationId = "main-chat"
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) = _uiState.update { it.copy(input = value, error = null) }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        val userMessage = MayraMessage(text, MayraMessage.Sender.USER)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                input = "",
                isThinking = true,
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
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isThinking = false,
                        error = response.errorMessage ?: response.text
                    )
                }
            }
        }
    }

    fun clearConversation() {
        aiManager.clearConversation(conversationId)
        _uiState.update { ChatUiState() }
    }
}
