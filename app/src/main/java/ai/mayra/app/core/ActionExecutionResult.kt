package ai.mayra.app.core

import ai.mayra.app.core.actions.DevicePermission

sealed interface ActionExecutionResult {
    data object Success : ActionExecutionResult
    data class ConfirmationRequired(val message: String) : ActionExecutionResult
    data class PermissionRequired(
        val message: String,
        val permissions: Set<DevicePermission>
    ) : ActionExecutionResult
    data class NotSupported(val reason: String) : ActionExecutionResult
    data class Failure(val error: String) : ActionExecutionResult
}
