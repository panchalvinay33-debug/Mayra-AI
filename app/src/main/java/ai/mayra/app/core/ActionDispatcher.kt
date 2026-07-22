package ai.mayra.app.core

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

    suspend fun dispatch(intent: AssistantIntent): String? = when (intent) {
        is AssistantIntent.OpenApp -> executor.openApp(intent.appName).toReply(
            successMessage = "Opening ${intent.appName}."
        )

        is AssistantIntent.CallContact -> executor.callContact(intent.contact).toReply(
            successMessage = "Calling ${intent.contact}."
        )

        is AssistantIntent.ComposeMessage -> executor.sendMessage(
            recipient = intent.recipient,
            message = intent.message
        ).toReply(
            successMessage = if (intent.message.isNullOrBlank()) {
                "Opening a message for ${intent.recipient}."
            } else {
                "Message prepared for ${intent.recipient}."
            }
        )

        is AssistantIntent.CreateReminder -> executor.createReminder(intent.request).toReply(
            successMessage = "Reminder created: ${intent.request}."
        )

        else -> null
    }

    private fun ActionExecutionResult.toReply(successMessage: String): String = when (this) {
        ActionExecutionResult.Success -> successMessage
        is ActionExecutionResult.ConfirmationRequired -> message
        is ActionExecutionResult.NotSupported -> reason
        is ActionExecutionResult.Failure -> "I couldn't complete that action: $error"
    }
}
