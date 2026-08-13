package ai.mayra.app.chat

import ai.mayra.app.MayraRuntime
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraAssistantResponse
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.MayraStructuredAssistant
import ai.mayra.app.memory.MayraMemoryChatController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(
    private val assistant: MayraAssistant = MayraRuntime.assistant,
    private val runtimeBridge: MayraChatRuntimeBridge? =
        MayraRuntime.typedRuntime.takeIf { MayraRuntime.typedRuntimeInstalled }?.let {
            MayraChatRuntimeBridge(
                runtime = it.runtime,
                memoryChat = MayraRuntime.personalMemory.takeIf { MayraRuntime.personalMemoryInstalled }
                    ?.let(::MayraMemoryChatController)
            )
        }
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ChatUiState(pendingMemoryApproval = runtimeBridge?.restoredMemoryApproval())
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) = _uiState.update { it.copy(input = value, error = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun clearConversation() {
        if (_uiState.value.isThinking || hasPendingApproval()) return
        _uiState.value = ChatUiState()
    }

    fun sendMessage() = sendText(_uiState.value.input)

    /**
     * Sends a trusted UI preset through the exact same typed runtime, confirmation and memory-safety
     * pipeline as manually entered chat text. This avoids Compose state races from updateInput()+send.
     */
    fun sendPreset(text: String) = sendText(text)

    private fun sendText(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty() || _uiState.value.isThinking || hasPendingApproval()) return
        val userMessage = MayraMessage(text, MayraMessage.Sender.USER)
        val conversation = _uiState.value.messages + userMessage
        _uiState.update { it.copy(messages = conversation, input = "", isThinking = true, error = null) }

        viewModelScope.launch {
            when (val bridgeResult = withContext(Dispatchers.Default) {
                runtimeBridge?.dispatch(text) ?: MayraChatBridgeResult.DelegateToAssistant
            }) {
                MayraChatBridgeResult.DelegateToAssistant -> replyWithAssistant(text, conversation)
                is MayraChatBridgeResult.Reply -> appendMayraReply(MayraAssistantResponse(bridgeResult.text))
                is MayraChatBridgeResult.NeedsConfirmation -> _uiState.update {
                    it.copy(
                        messages = it.messages + MayraMessage(bridgeResult.pending.prompt, MayraMessage.Sender.MAYRA),
                        isThinking = false,
                        pendingConfirmation = bridgeResult.pending
                    )
                }
                is MayraChatBridgeResult.NeedsMemoryApproval -> _uiState.update {
                    it.copy(
                        messages = it.messages + MayraMessage(
                            if (bridgeResult.pending.previousValue == null) "Please review this memory before saving it."
                            else "This changes an existing memory. Please compare both values before replacing it.",
                            MayraMessage.Sender.MAYRA
                        ),
                        isThinking = false,
                        pendingMemoryApproval = bridgeResult.pending
                    )
                }
            }
        }
    }

    fun confirmPendingAction() {
        val pending = _uiState.value.pendingConfirmation ?: return
        val bridge = runtimeBridge ?: return
        if (_uiState.value.isThinking) return
        _uiState.update { it.copy(isThinking = true, error = null) }
        viewModelScope.launch {
            val reply = withContext(Dispatchers.Default) { bridge.confirm(pending).text }
            _uiState.update {
                it.copy(
                    messages = it.messages + MayraMessage(reply, MayraMessage.Sender.MAYRA),
                    isThinking = false,
                    pendingConfirmation = null
                )
            }
        }
    }

    fun cancelPendingAction() {
        if (_uiState.value.pendingConfirmation == null || _uiState.value.isThinking) return
        _uiState.update {
            it.copy(
                messages = it.messages + MayraMessage("Action cancelled.", MayraMessage.Sender.MAYRA),
                pendingConfirmation = null,
                error = null
            )
        }
    }

    fun savePendingMemory() {
        val pending = _uiState.value.pendingMemoryApproval ?: return
        val bridge = runtimeBridge ?: return
        if (_uiState.value.isThinking) return
        _uiState.update { it.copy(isThinking = true, error = null) }
        viewModelScope.launch {
            val reply = withContext(Dispatchers.Default) { bridge.approveMemory(pending).text }
            _uiState.update {
                it.copy(
                    messages = it.messages + MayraMessage(reply, MayraMessage.Sender.MAYRA),
                    isThinking = false,
                    pendingMemoryApproval = null
                )
            }
        }
    }

    fun cancelPendingMemory() {
        val pending = _uiState.value.pendingMemoryApproval ?: return
        val bridge = runtimeBridge ?: return
        if (_uiState.value.isThinking) return
        val reply = bridge.cancelMemory(pending).text
        _uiState.update {
            it.copy(
                messages = it.messages + MayraMessage(reply, MayraMessage.Sender.MAYRA),
                pendingMemoryApproval = null,
                error = null
            )
        }
    }

    private fun hasPendingApproval(): Boolean =
        _uiState.value.pendingConfirmation != null || _uiState.value.pendingMemoryApproval != null

    private suspend fun replyWithAssistant(text: String, conversation: List<MayraMessage>) {
        val result = when (val current = assistant) {
            is MayraStructuredAssistant -> current.replyStructured(text, conversation)
            else -> current.reply(text, conversation).map(::MayraAssistantResponse)
        }
        result.onSuccess(::appendMayraReply).onFailure { error ->
            _uiState.update { it.copy(isThinking = false, error = error.message ?: "Something went wrong") }
        }
    }

    private fun appendMayraReply(reply: MayraAssistantResponse) {
        val normalized = reply.normalized()
        _uiState.update {
            it.copy(
                messages = it.messages + MayraMessage(
                    text = normalized.text,
                    sender = MayraMessage.Sender.MAYRA,
                    usedPersonalMemoryKeys = normalized.usedPersonalMemoryKeys
                ),
                isThinking = false,
                pendingConfirmation = null,
                pendingMemoryApproval = null
            )
        }
    }

    companion object {
        fun factory(assistant: MayraAssistant, runtimeBridge: MayraChatRuntimeBridge? = null): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(ChatViewModel::class.java)) { "Unsupported ViewModel: ${modelClass.name}" }
                    return ChatViewModel(assistant, runtimeBridge) as T
                }
            }
    }
}