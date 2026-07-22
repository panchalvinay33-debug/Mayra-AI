package ai.mayra.app.core.device

data class DeviceActionPolicyDecision(
    val allowed: Boolean,
    val status: DeviceActionStatus,
    val reason: String? = null
)

class DeviceActionPolicy(
    private val requiredPermissions: Map<DeviceCapability, Set<String>> = defaultPermissions(),
    private val confirmationRequired: Set<DeviceCapability> = setOf(
        DeviceCapability.APP_LAUNCH,
        DeviceCapability.OPEN_SETTINGS,
        DeviceCapability.SHOW_NOTIFICATION
    )
) {
    fun evaluate(action: DeviceAction, context: DeviceActionContext): DeviceActionPolicyDecision {
        val missing = requiredPermissions[action.capability].orEmpty() - context.grantedPermissions
        if (missing.isNotEmpty()) {
            return DeviceActionPolicyDecision(
                allowed = false,
                status = DeviceActionStatus.PERMISSION_DENIED,
                reason = "Missing permissions: ${missing.sorted().joinToString()}"
            )
        }

        if (action.capability in confirmationRequired && !context.confirmed) {
            return DeviceActionPolicyDecision(
                allowed = false,
                status = DeviceActionStatus.CONFIRMATION_REQUIRED,
                reason = "User confirmation is required."
            )
        }

        return DeviceActionPolicyDecision(true, DeviceActionStatus.SUCCESS)
    }

    companion object {
        fun defaultPermissions(): Map<DeviceCapability, Set<String>> = mapOf(
            DeviceCapability.SHOW_NOTIFICATION to setOf("android.permission.POST_NOTIFICATIONS")
        )
    }
}
