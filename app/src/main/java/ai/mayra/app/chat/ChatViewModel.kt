package ai.mayra.app.chat

import ai.mayra.app.MayraRuntime
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val assistant: MayraAssistant = MayraRuntime.assistant
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) = _uiState.update { it.copy(input = value, error = null) }

    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun clearConversation() {
        if (_uiState.value.isThinking) return
        _uiState.value = ChatUiState()
    }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.isThinking) return

        val userMessage = MayraMessage(text, MayraMessage.Sender.USER)
        val conversation = _uiState.value.messages + userMessage
        _uiState.update {
            it.copy(
                messages = conversation,
                input = "",
                isThinking = true,
                error = null
            )
        }

        viewModelScope.launch {
            assistant.reply(text, conversation)
                .onSuccess { reply ->
                    _uiState.update {
                        it.copy(
                            messages = it.messages + MayraMessage(reply, MayraMessage.Sender.MAYRA),
                            isThinking = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isThinking = false,
                            error = error.message ?: "Something went wrong"
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(assistant: MayraAssistant): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                        "Unsupported ViewModel: ${modelClass.name}"
                    }
                    return ChatViewModel(assistant) as T
                }
            }
    }
}
