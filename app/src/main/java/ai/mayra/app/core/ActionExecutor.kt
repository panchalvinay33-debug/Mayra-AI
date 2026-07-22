package ai.mayra.app.core

/**
 * Central execution boundary for device actions.
 * Android-specific implementations will execute these actions.
 */
interface ActionExecutor {
    suspend fun openApp(packageOrName:String): ActionExecutionResult
    suspend fun callContact(name:String): ActionExecutionResult
    suspend fun sendMessage(recipient:String,message:String?): ActionExecutionResult
    suspend fun createReminder(request:String): ActionExecutionResult
}
