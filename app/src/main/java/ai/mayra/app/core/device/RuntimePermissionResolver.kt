package ai.mayra.app.core.device

/** Maps device capabilities to runtime permissions without coupling core logic to Android APIs. */
class RuntimePermissionResolver(
    private val requiredPermissions: Map<DeviceCapability, Set<String>> = emptyMap()
) {
    data class Resolution(
        val capability: DeviceCapability,
        val required: Set<String>,
        val granted: Set<String>,
        val missing: Set<String>
    ) {
        val isGranted: Boolean get() = missing.isEmpty()
    }

    fun resolve(capability: DeviceCapability, grantedPermissions: Set<String>): Resolution {
        val required = requiredPermissions[capability].orEmpty()
        val granted = required.intersect(grantedPermissions)
        return Resolution(
            capability = capability,
            required = required,
            granted = granted,
            missing = required - grantedPermissions
        )
    }

    fun canExecute(capability: DeviceCapability, grantedPermissions: Set<String>): Boolean =
        resolve(capability, grantedPermissions).isGranted

    companion object {
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

        fun android(apiLevel: Int): RuntimePermissionResolver {
            val requirements = buildMap {
                if (apiLevel >= 33) {
                    put(DeviceCapability.SHOW_NOTIFICATION, setOf(POST_NOTIFICATIONS))
                }
            }
            return RuntimePermissionResolver(requirements)
        }
    }
}
