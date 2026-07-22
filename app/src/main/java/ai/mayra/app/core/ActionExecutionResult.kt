package ai.mayra.app.core

sealed interface ActionExecutionResult {
    data object Success: ActionExecutionResult
    data class ConfirmationRequired(val message:String): ActionExecutionResult
    data class NotSupported(val reason:String): ActionExecutionResult
    data class Failure(val error:String): ActionExecutionResult
}
