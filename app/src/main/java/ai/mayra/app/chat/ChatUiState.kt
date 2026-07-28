package ai.mayra.app.chat

import ai.mayra.app.core.MayraMessage

data class ChatUiState(
    val messages: List<MayraMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    val error: String? = null,
    val pendingConfirmation: PendingChatConfirmation? = null
)
