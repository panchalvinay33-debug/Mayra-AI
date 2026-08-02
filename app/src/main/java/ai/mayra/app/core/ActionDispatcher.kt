package ai.mayra.app.core

import java.util.Locale

/**
 * Routes structured assistant intents to the device-action boundary and converts execution
 * outcomes into safe, user-facing replies.
 *
 * Chat-only intents stay inside [LocalCommandEngine]. Sensitive operations remain confirmation
 * gated by the [ActionExecutor] implementation.
 */
class ActionDispatcher(
    private val executor: ActionExecutor = DefaultActionExecutor()
) {

    suspend fun dispatch(intent: AssistantIntent): String? {
        if (intent is AssistantIntent.Chat) {
            val normalized = intent.message.trim().lowercase(Locale.ROOT)
            if (normalized in confirmationWords) {
                return executor.confirmPending().toReply("Action completed.")
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
                successMessage = "Opening the dialer for ${intent.contact}. Review the number and tap Call when ready."
            )

            is AssistantIntent.ComposeMessage -> executor.sendMessage(
                recipient = intent.recipient,
                message = intent.message
            ).toReply(
                successMessage = if (intent.message.isNullOrBlank()) {
                    "Opening a message for ${intent.recipient}."
                } else {
                    "Message prepared for ${intent.recipient}. Review it and tap Send when ready."
                }
            )

            is AssistantIntent.CreateReminder -> executor.createReminder(intent.request).toReply(
                successMessage = "Reminder scheduled: ${intent.request}."
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
