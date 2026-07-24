package ai.mayra.app.core

import ai.mayra.app.action.MayraActionRuntime
import java.util.Locale

/**
 * Routes structured assistant intents to the device-action boundary and converts execution
 * outcomes into safe, user-facing replies.
 *
 * Chat-only intents stay inside [LocalCommandEngine]. Sensitive operations remain confirmation
 * gated by the [ActionExecutor] implementation. Explicit Mayra stop/resume commands control the
 * shared action-runtime kill switch and are intentionally separate from confirmation rejection.
 */
class ActionDispatcher(
    private val executor: ActionExecutor = DefaultActionExecutor(),
    private val stopAllActions: () -> Unit = MayraActionRuntime::stopAll,
    private val resumeActions: () -> Unit = MayraActionRuntime::resume
) {

    suspend fun dispatch(intent: AssistantIntent): String? {
        if (intent is AssistantIntent.Chat) {
            val normalized = intent.message.trim().lowercase(Locale.ROOT)
            if (normalized in globalStopWords) {
                stopAllActions()
                return "All Mayra phone actions are stopped. Chat and phone awareness remain available."
            }
            if (normalized in globalResumeWords) {
                resumeActions()
                return "Mayra phone actions are enabled again."
            }
            if (normalized in confirmationWords) {
                return executor.confirmPending().toReply("Action handed to Android.")
            }
            if (normalized in rejectionWords) {
                return executor.rejectPending().toReply("Action cancelled.")
            }
        }

        return when (intent) {
            is AssistantIntent.OpenApp -> executor.openApp(intent.appName).toReply(
                successMessage = "Opening ${intent.appName}."
            )

            is AssistantIntent.CallContact -> executor.callContact(intent.contact).toReply(
                successMessage = "Call flow opened for ${intent.contact}. Connection is not claimed yet."
            )

            is AssistantIntent.ComposeMessage -> executor.sendMessage(
                recipient = intent.recipient,
                message = intent.message
            ).toReply(
                successMessage = if (intent.message.isNullOrBlank()) {
                    "Opening a message for ${intent.recipient}."
                } else {
                    "Message prepared for ${intent.recipient}. Review it before sending."
                }
            )

            is AssistantIntent.CreateReminder -> executor.createReminder(intent.request).toReply(
                successMessage = "Reminder creation opened: ${intent.request}. Saving remains visible to you."
            )

            else -> null
        }
    }

    private fun ActionExecutionResult.toReply(successMessage: String): String = when (this) {
        ActionExecutionResult.Success -> successMessage
        is ActionExecutionResult.ConfirmationRequired -> message
        is ActionExecutionResult.PermissionRequired -> message
        is ActionExecutionResult.NotSupported -> reason
        is ActionExecutionResult.Failure -> "I couldn't complete that action: $error"
    }

    private companion object {
        val globalStopWords = setOf(
            "mayra stop", "mayra stop actions", "stop mayra actions",
            "mayra ruk jao", "mayra sab action roko", "मायरा स्टॉप", "मायरा रुक जाओ"
        )
        val globalResumeWords = setOf(
            "mayra resume", "mayra resume actions", "resume mayra actions",
            "mayra actions chalu karo", "mayra phir se chalu", "मायरा फिर शुरू करो"
        )
        val confirmationWords = setOf(
            "yes", "yes please", "confirm", "continue", "ok", "okay",
            "haan", "ha", "han", "kar do", "हां", "हाँ", "ठीक है"
        )
        val rejectionWords = setOf(
            "no", "cancel", "stop", "not now", "nahi", "nahin", "mat karo",
            "नहीं", "रद्द करो"
        )
    }
}
