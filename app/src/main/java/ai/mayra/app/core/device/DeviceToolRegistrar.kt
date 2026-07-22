package ai.mayra.app.core.device

import ai.mayra.app.core.intelligence.ToolManifest
import ai.mayra.app.core.intelligence.ToolRegistry

class DeviceToolRegistrar(
    private val registry: ToolRegistry,
    private val handler: DeviceActionHandler,
    private val capabilities: AndroidCapabilityRegistry
) {
    fun registerAvailable(replace: Boolean = false): List<ToolManifest> =
        DeviceToolCatalog.definitions()
            .filter { capabilities.supports(it.capability) }
            .map { definition ->
                registry.register(
                    tool = DeviceToolAdapter(
                        manifest = definition.manifest,
                        actionFactory = definition.actionFactory,
                        handler = handler
                    ),
                    replace = replace
                )
            }

    fun synchronize(): DeviceToolSynchronization {
        val definitions = DeviceToolCatalog.definitions()
        val availableIds = definitions
            .filter { capabilities.supports(it.capability) }
            .mapTo(linkedSetOf()) { it.manifest.id }

        val registeredIds = registry.manifests(includeDisabled = true)
            .mapTo(linkedSetOf()) { it.id }

        val removed = definitions
            .map { it.manifest.id }
            .filter { it in registeredIds && it !in availableIds }
            .count(registry::unregister)

        val added = definitions
            .filter { it.manifest.id in availableIds && it.manifest.id !in registeredIds }
            .map { definition ->
                registry.register(
                    DeviceToolAdapter(
                        manifest = definition.manifest,
                        actionFactory = definition.actionFactory,
                        handler = handler
                    )
                )
            }

        return DeviceToolSynchronization(
            added = added.map { it.id }.toSet(),
            removedCount = removed,
            active = availableIds
        )
    }
}

data class DeviceToolSynchronization(
    val added: Set<String>,
    val removedCount: Int,
    val active: Set<String>
)
