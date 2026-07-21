package ai.mayra.app.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.mayra.app.core.LocalMayraAssistant
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val assistant: MayraAssistant = LocalMayraAssistant()
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) = _uiState.update { it.copy(input = value, error = null) }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        val userMessage = MayraMessage(text, MayraMessage.Sender.USER)
        _uiState.update { it.copy(messages = it.messages + userMessage, input = "", isThinking = true) }

        viewModelScope.launch {
            assistant.reply(text)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + MayraMessage(reply, MayraMessage.Sender.MAYRA),
                            isThinking = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isThinking = false, error = error.message ?: "Something went wrong") }
                }
        }
    }
}
