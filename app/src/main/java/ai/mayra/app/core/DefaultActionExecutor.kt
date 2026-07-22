package ai.mayra.app.core

class DefaultActionExecutor : ActionExecutor {
    override fun openApp(appName: String)=ActionExecutionResult.NotSupported("App launching not connected yet")
    override fun callContact(contact: String)=ActionExecutionResult.ConfirmationRequired("Confirm call to $contact")
    override fun sendMessage(recipient: String, message: String?)=ActionExecutionResult.ConfirmationRequired("Confirm message to $recipient")
    override fun createReminder(request: String)=ActionExecutionResult.NotSupported("Reminder scheduler not connected yet")
}
