package ai.mayra.app.core.device

class AndroidCapabilityRegistry(
    initialCapabilities: Set<DeviceCapability> = emptySet()
) {
    private val capabilities = linkedSetOf<DeviceCapability>().apply {
        addAll(initialCapabilities)
    }

    @Synchronized
    fun register(capability: DeviceCapability): Boolean = capabilities.add(capability)

    @Synchronized
    fun registerAll(values: Iterable<DeviceCapability>): Int {
        var added = 0
        values.forEach { if (capabilities.add(it)) added++ }
        return added
    }

    @Synchronized
    fun unregister(capability: DeviceCapability): Boolean = capabilities.remove(capability)

    @Synchronized
    fun supports(capability: DeviceCapability): Boolean = capability in capabilities

    @Synchronized
    fun snapshot(): Set<DeviceCapability> = capabilities.toSet()

    @Synchronized
    fun clear() = capabilities.clear()
}
