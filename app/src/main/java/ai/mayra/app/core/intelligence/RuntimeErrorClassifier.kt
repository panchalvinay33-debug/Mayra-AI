package ai.mayra.app.core.intelligence

enum class RuntimeErrorCategory {
    VALIDATION,
    PERMISSION,
    TIMEOUT,
    NETWORK,
    TOOL,
    CANCELLATION,
    NOT_FOUND,
    INTERNAL
}

data class ClassifiedRuntimeError(
    val category: RuntimeErrorCategory,
    val code: String,
    val retryable: Boolean,
    val userMessage: String
)

class RuntimeErrorClassifier(
    private val retryableCodes: Set<String> = setOf(
        "temporary_failure",
        "timeout",
        "network_unavailable",
        "network_error",
        "tool_execution_failed"
    )
) {
    fun classify(result: ToolResult): ClassifiedRuntimeError? {
        if (result.status == ToolExecutionStatus.SUCCESS) return null

        val code = result.errorCode?.trim()?.takeIf { it.isNotEmpty() }
            ?: defaultCode(result.status)
        val category = categoryFor(result.status, code)
        return ClassifiedRuntimeError(
            category = category,
            code = code,
            retryable = code in retryableCodes && category !in nonRetryableCategories,
            userMessage = messageFor(category)
        )
    }

    fun classify(error: Throwable): ClassifiedRuntimeError {
        val simpleName = error::class.simpleName.orEmpty()
        val message = error.message.orEmpty().lowercase()
        val category = when {
            simpleName.contains("Cancellation", ignoreCase = true) -> RuntimeErrorCategory.CANCELLATION
            simpleName.contains("Timeout", ignoreCase = true) || "timeout" in message -> RuntimeErrorCategory.TIMEOUT
            simpleName.contains("UnknownHost", ignoreCase = true) ||
                simpleName.contains("Connect", ignoreCase = true) ||
                "network" in message -> RuntimeErrorCategory.NETWORK
            error is IllegalArgumentException -> RuntimeErrorCategory.VALIDATION
            error is SecurityException -> RuntimeErrorCategory.PERMISSION
            else -> RuntimeErrorCategory.INTERNAL
        }
        val code = when (category) {
            RuntimeErrorCategory.CANCELLATION -> "cancelled"
            RuntimeErrorCategory.TIMEOUT -> "timeout"
            RuntimeErrorCategory.NETWORK -> "network_error"
            RuntimeErrorCategory.VALIDATION -> "invalid_request"
            RuntimeErrorCategory.PERMISSION -> "permission_denied"
            else -> "internal_error"
        }
        return ClassifiedRuntimeError(
            category = category,
            code = code,
            retryable = category in setOf(RuntimeErrorCategory.TIMEOUT, RuntimeErrorCategory.NETWORK),
            userMessage = messageFor(category)
        )
    }

    private fun defaultCode(status: ToolExecutionStatus): String = when (status) {
        ToolExecutionStatus.SUCCESS -> "success"
        ToolExecutionStatus.DENIED -> "permission_denied"
        ToolExecutionStatus.NOT_FOUND -> "tool_not_found"
        ToolExecutionStatus.FAILED -> "tool_execution_failed"
    }

    private fun categoryFor(status: ToolExecutionStatus, code: String): RuntimeErrorCategory = when {
        status == ToolExecutionStatus.NOT_FOUND || code == "tool_not_found" -> RuntimeErrorCategory.NOT_FOUND
        status == ToolExecutionStatus.DENIED || code in permissionCodes -> RuntimeErrorCategory.PERMISSION
        code in validationCodes -> RuntimeErrorCategory.VALIDATION
        code in timeoutCodes -> RuntimeErrorCategory.TIMEOUT
        code in cancellationCodes -> RuntimeErrorCategory.CANCELLATION
        code in networkCodes -> RuntimeErrorCategory.NETWORK
        status == ToolExecutionStatus.FAILED -> RuntimeErrorCategory.TOOL
        else -> RuntimeErrorCategory.INTERNAL
    }

    private fun messageFor(category: RuntimeErrorCategory): String = when (category) {
        RuntimeErrorCategory.VALIDATION -> "The request is missing or contains invalid information."
        RuntimeErrorCategory.PERMISSION -> "Permission or confirmation is required before this action can run."
        RuntimeErrorCategory.TIMEOUT -> "The action took too long and was stopped."
        RuntimeErrorCategory.NETWORK -> "The action could not reach the required service."
        RuntimeErrorCategory.TOOL -> "The requested action could not be completed."
        RuntimeErrorCategory.CANCELLATION -> "The action was cancelled."
        RuntimeErrorCategory.NOT_FOUND -> "The requested capability is not available."
        RuntimeErrorCategory.INTERNAL -> "An unexpected runtime error occurred."
    }

    companion object {
        private val validationCodes = setOf("unknown_argument", "missing_required_argument", "invalid_request")
        private val permissionCodes = setOf("permission_denied", "confirmation_required", "missing_permission")
        private val timeoutCodes = setOf("timeout", "tool_timeout")
        private val cancellationCodes = setOf("cancelled", "cancellation_requested")
        private val networkCodes = setOf("network_error", "network_unavailable", "temporary_failure")
        private val nonRetryableCategories = setOf(
            RuntimeErrorCategory.VALIDATION,
            RuntimeErrorCategory.PERMISSION,
            RuntimeErrorCategory.CANCELLATION,
            RuntimeErrorCategory.NOT_FOUND
        )
    }
}
