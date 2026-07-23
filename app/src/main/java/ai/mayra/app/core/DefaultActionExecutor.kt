package ai.mayra.app.core

class DefaultActionExecutor : ActionExecutor {
    override suspend fun openApp(packageOrName: String): ActionExecutionResult =
        ActionExecutionResult.NotSupported("App launching not connected yet")

    override suspend fun callContact(name: String): ActionExecutionResult =
        ActionExecutionResult.ConfirmationRequired("Confirm call to $name")

    override suspend fun sendMessage(
        recipient: String,
        message: String?
    ): ActionExecutionResult =
        ActionExecutionResult.ConfirmationRequired("Confirm message to $recipient")

    override suspend fun createReminder(request: String): ActionExecutionResult =
        ActionExecutionResult.NotSupported("Reminder scheduler not connected yet")
}
