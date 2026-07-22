package ai.mayra.app.core.device

import ai.mayra.app.core.intelligence.MayraTool
import ai.mayra.app.core.intelligence.ToolExecutionStatus
import ai.mayra.app.core.intelligence.ToolInvocation
import ai.mayra.app.core.intelligence.ToolManifest
import ai.mayra.app.core.intelligence.ToolResult

class DeviceToolAdapter(
    override val manifest: ToolManifest,
    private val actionFactory: (Map<String, String>) -> DeviceAction,
    private val handler: DeviceActionHandler
) : MayraTool {

    override suspend fun execute(invocation: ToolInvocation): ToolResult {
        val action = try {
            actionFactory(invocation.arguments)
        } catch (error: IllegalArgumentException) {
            return ToolResult(
                toolId = manifest.id,
                status = ToolExecutionStatus.FAILED,
                errorCode = "invalid_device_action",
                metadata = mapOf("reason" to (error.message ?: "Invalid device action arguments."))
            )
        }

        val result = handler.execute(
            action = action,
            context = DeviceActionContext(
                sessionId = invocation.context.sessionId,
                grantedPermissions = invocation.context.grantedPermissions,
                confirmed = invocation.context.metadata[CONFIRMED_METADATA_KEY].toBoolean(),
                metadata = invocation.context.metadata
            )
        )

        return ToolResult(
            toolId = manifest.id,
            status = result.status.toToolStatus(),
            output = result.message,
            errorCode = result.errorCode ?: result.status.defaultErrorCode(),
            metadata = result.metadata + mapOf("deviceCapability" to action.capability.name)
        )
    }

    private fun DeviceActionStatus.toToolStatus(): ToolExecutionStatus = when (this) {
        DeviceActionStatus.SUCCESS -> ToolExecutionStatus.SUCCESS
        DeviceActionStatus.PERMISSION_DENIED,
        DeviceActionStatus.CONFIRMATION_REQUIRED -> ToolExecutionStatus.DENIED
        DeviceActionStatus.UNSUPPORTED,
        DeviceActionStatus.INVALID_REQUEST,
        DeviceActionStatus.FAILED -> ToolExecutionStatus.FAILED
    }

    private fun DeviceActionStatus.defaultErrorCode(): String? = when (this) {
        DeviceActionStatus.SUCCESS -> null
        DeviceActionStatus.UNSUPPORTED -> "device_action_unsupported"
        DeviceActionStatus.INVALID_REQUEST -> "invalid_device_action"
        DeviceActionStatus.PERMISSION_DENIED -> "device_permission_denied"
        DeviceActionStatus.CONFIRMATION_REQUIRED -> "confirmation_required"
        DeviceActionStatus.FAILED -> "device_action_failed"
    }

    companion object {
        const val CONFIRMED_METADATA_KEY: String = "tool.confirmed"
    }
}
