package ai.mayra.app.core.device

/**
 * Guards platform execution with the latest capability registry and runtime permission state.
 */
class CapabilityAwareDeviceActionHandler(
    private val delegate: DeviceActionHandler,
    private val capabilities: AndroidCapabilityRegistry,
    private val permissionResolver: RuntimePermissionResolver
) : DeviceActionHandler {
    override suspend fun execute(
        action: DeviceAction,
        context: DeviceActionContext
    ): DeviceActionResult {
        if (!capabilities.supports(action.capability)) {
            return DeviceActionResult(
                status = DeviceActionStatus.UNSUPPORTED,
                message = "Capability ${action.capability.name} is unavailable.",
                errorCode = "capability_unavailable",
                metadata = mapOf("capability" to action.capability.name)
            )
        }

        val permission = permissionResolver.resolve(
            capability = action.capability,
            grantedPermissions = context.grantedPermissions
        )
        if (!permission.isGranted) {
            return DeviceActionResult(
                status = DeviceActionStatus.PERMISSION_DENIED,
                message = "Required runtime permission is missing.",
                errorCode = "runtime_permission_missing",
                metadata = mapOf(
                    "capability" to action.capability.name,
                    "missingPermissions" to permission.missing.sorted().joinToString(",")
                )
            )
        }

        return delegate.execute(action, context)
    }
}
