package ai.mayra.app.chat

import ai.mayra.app.core.MayraQueryRouter
import ai.mayra.app.core.MayraRoutingOutcome
import ai.mayra.app.core.MayraRoutingRuntime
import ai.mayra.app.core.MayraRoutingRuntimeResult
import ai.mayra.app.memory.MayraMemoryChatController
import ai.mayra.app.memory.MayraMemoryChatResult
import ai.mayra.app.memory.PendingMemoryApproval

/** Pending approval retained by ChatViewModel across configuration changes. */
data class PendingChatConfirmation(
    val message: String,
    val token: String,
    val prompt: String,
    val expiresAtEpochMillis: Long
) {
    init {
        require(message.isNotBlank())
        require(token.isNotBlank())
        require(prompt.isNotBlank())
        require(expiresAtEpochMillis > 0L)
    }

    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        nowEpochMillis >= expiresAtEpochMillis
}

sealed interface MayraChatBridgeResult {
    data object DelegateToAssistant : MayraChatBridgeResult
    data class Reply(val text: String) : MayraChatBridgeResult
    data class NeedsConfirmation(val pending: PendingChatConfirmation) : MayraChatBridgeResult
    data class NeedsMemoryApproval(val pending: PendingMemoryApproval) : MayraChatBridgeResult
}

/**
 * Non-blocking chat boundary that preserves the stable suspend assistant for conversational answers.
 * Deterministic owner-memory commands run before model delegation. Typed runtime owns retrieval,
 * actions, clarification and unsupported outcomes.
 */
class MayraChatRuntimeBridge(
    private val runtime: MayraRoutingRuntime,
    private val memoryChat: MayraMemoryChatController? = null
) {
    fun dispatch(message: String): MayraChatBridgeResult {
        val normalized = message.trim()
        if (normalized.isEmpty()) return MayraChatBridgeResult.Reply("Please tell Mayra what you need.")

        when (val memoryResult = memoryChat?.handle(normalized)) {
            is MayraMemoryChatResult.Reply -> return MayraChatBridgeResult.Reply(memoryResult.text)
            is MayraMemoryChatResult.NeedsApproval -> return MayraChatBridgeResult.NeedsMemoryApproval(memoryResult.pending)
            MayraMemoryChatResult.NotHandled, null -> Unit
        }

        if (MayraQueryRouter.route(normalized).outcome == MayraRoutingOutcome.ANSWER) {
            return MayraChatBridgeResult.DelegateToAssistant
        }
        return runtime.dispatch(normalized).toChatResult(normalized)
    }

    fun confirm(pending: PendingChatConfirmation): MayraChatBridgeResult.Reply =
        if (pending.isExpired()) {
            MayraChatBridgeResult.Reply("That confirmation expired. Please request the action again so Mayra can create a fresh approval.")
        } else {
            MayraChatBridgeResult.Reply(
                runtime.confirmAndDispatch(pending.message, pending.token).userText()
            )
        }

    fun approveMemory(pending: PendingMemoryApproval): MayraChatBridgeResult.Reply =
        MayraChatBridgeResult.Reply(
            memoryChat?.approve(pending.proposalId)?.text ?: "Personal memory is unavailable."
        )

    fun cancelMemory(pending: PendingMemoryApproval): MayraChatBridgeResult.Reply =
        MayraChatBridgeResult.Reply(
            memoryChat?.cancel(pending.proposalId)?.text ?: "Personal memory is unavailable."
        )

    fun restoredMemoryApproval(): PendingMemoryApproval? = memoryChat?.restoreLatestPending()

    private fun MayraRoutingRuntimeResult.toChatResult(message: String): MayraChatBridgeResult = when (this) {
        is MayraRoutingRuntimeResult.ConfirmationRequired -> {
            val tokenValue = token?.value
            val tokenExpiry = token?.expiresAt?.toEpochMilli()
            if (tokenValue.isNullOrBlank() || tokenExpiry == null) {
                MayraChatBridgeResult.Reply("Mayra could not create a valid confirmation. Please try again.")
            } else {
                MayraChatBridgeResult.NeedsConfirmation(
                    PendingChatConfirmation(
                        message = message,
                        token = tokenValue,
                        prompt = prompt,
                        expiresAtEpochMillis = tokenExpiry
                    )
                )
            }
        }
        else -> MayraChatBridgeResult.Reply(userText())
    }
}

fun MayraRoutingRuntimeResult.userText(): String = when (this) {
    is MayraRoutingRuntimeResult.Executed -> output
    is MayraRoutingRuntimeResult.ConfirmationRequired -> prompt
    is MayraRoutingRuntimeResult.ClarificationRequired -> prompt
    is MayraRoutingRuntimeResult.Blocked -> reason
    is MayraRoutingRuntimeResult.DuplicateBlocked -> reason
    is MayraRoutingRuntimeResult.Failed -> reason
}
