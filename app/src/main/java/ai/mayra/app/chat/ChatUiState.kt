package ai.mayra.app.chat

import ai.mayra.app.core.MayraMessage
import ai.mayra.app.memory.PendingMemoryApproval

data class ChatUiState(
    val messages: List<MayraMessage> = emptyList(),
    val input: String = "",
    val isThinking: Boolean = false,
    val error: String? = null,
    val pendingConfirmation: PendingChatConfirmation? = null,
    val pendingMemoryApproval: PendingMemoryApproval? = null
)
