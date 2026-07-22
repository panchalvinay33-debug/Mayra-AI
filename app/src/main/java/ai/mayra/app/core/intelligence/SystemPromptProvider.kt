package ai.mayra.app.core.intelligence

interface SystemPromptProvider {
    fun instructions(metadata: Map<String, String> = emptyMap()): List<String>
}

class StaticSystemPromptProvider(
    instructions: List<String>
) : SystemPromptProvider {
    private val values = instructions.map(String::trim).filter(String::isNotBlank).distinct()

    init {
        require(values.isNotEmpty()) { "At least one system instruction is required." }
    }

    override fun instructions(metadata: Map<String, String>): List<String> = values
}

class CompositeSystemPromptProvider(
    providers: List<SystemPromptProvider>
) : SystemPromptProvider {
    private val providers = providers.toList()

    init {
        require(this.providers.isNotEmpty()) { "At least one prompt provider is required." }
    }

    override fun instructions(metadata: Map<String, String>): List<String> = providers
        .flatMap { it.instructions(metadata) }
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
}

class MetadataSystemPromptProvider(
    private val key: String,
    private val prefix: String
) : SystemPromptProvider {
    init {
        require(key.isNotBlank()) { "Metadata key cannot be blank." }
        require(prefix.isNotBlank()) { "Instruction prefix cannot be blank." }
    }

    override fun instructions(metadata: Map<String, String>): List<String> {
        val value = metadata[key]?.trim().orEmpty()
        return if (value.isBlank()) emptyList() else listOf("${prefix.trim()} $value")
    }
}
