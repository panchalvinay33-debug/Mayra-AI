package ai.mayra.app.core.device

/**
 * Probes the current runtime before device tools are exposed to the planner.
 * Probes are deliberately platform-neutral so they can be tested on the JVM.
 */
class DeviceCapabilityDetector(
    private val probes: Map<DeviceCapability, () -> Boolean>
) {
    data class Detection(
        val supported: Set<DeviceCapability>,
        val unsupported: Set<DeviceCapability>,
        val failures: Map<DeviceCapability, String>
    ) {
        fun supports(capability: DeviceCapability): Boolean = capability in supported
    }

    fun detect(): Detection {
        val supported = linkedSetOf<DeviceCapability>()
        val unsupported = linkedSetOf<DeviceCapability>()
        val failures = linkedMapOf<DeviceCapability, String>()

        DeviceCapability.entries.forEach { capability ->
            val probe = probes[capability]
            if (probe == null) {
                unsupported += capability
                return@forEach
            }

            try {
                if (probe()) supported += capability else unsupported += capability
            } catch (error: Throwable) {
                unsupported += capability
                failures[capability] = error::class.simpleName ?: "Throwable"
            }
        }

        return Detection(
            supported = supported,
            unsupported = unsupported,
            failures = failures
        )
    }

    fun refresh(registry: AndroidCapabilityRegistry): Detection {
        val detection = detect()
        registry.clear()
        registry.registerAll(detection.supported)
        return detection
    }
}
