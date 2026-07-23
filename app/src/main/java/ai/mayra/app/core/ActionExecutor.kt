package ai.mayra.app.core

/**
 * Central execution boundary for device actions.
 * Android-specific implementations execute these actions and may keep one pending confirmation.
 */
interface ActionExecutor {
    suspend fun openApp(packageOrName: String): ActionExecutionResult
    suspend fun callContact(name: String): ActionExecutionResult
    suspend fun sendMessage(recipient: String, message: String?): ActionExecutionResult
    suspend fun createReminder(request: String): ActionExecutionResult

    suspend fun confirmPending(): ActionExecutionResult =
        ActionExecutionResult.NotSupported("There is no action waiting for confirmation.")

    suspend fun rejectPending(): ActionExecutionResult =
        ActionExecutionResult.NotSupported("There is no action waiting for confirmation.")
}
