package ai.mayra.app.core.intelligence

class ToolRegistry {
    private val tools = linkedMapOf<String, MayraTool>()
    private val disabled = linkedSetOf<String>()

    @Synchronized
    fun register(tool: MayraTool, replace: Boolean = false): ToolManifest {
        val id = tool.manifest.id
        require(replace || id !in tools) { "Tool is already registered: $id" }
        tools[id] = tool
        if (!tool.manifest.enabledByDefault) disabled += id
        return tool.manifest
    }

    @Synchronized
    fun unregister(toolId: String): Boolean {
        disabled.remove(toolId)
        return tools.remove(toolId) != null
    }

    @Synchronized
    fun enable(toolId: String): Boolean {
        if (toolId !in tools) return false
        return disabled.remove(toolId)
    }

    @Synchronized
    fun disable(toolId: String): Boolean {
        if (toolId !in tools) return false
        return disabled.add(toolId)
    }

    @Synchronized
    fun resolve(toolId: String, includeDisabled: Boolean = false): MayraTool? {
        val normalized = toolId.trim().lowercase()
        if (!includeDisabled && normalized in disabled) return null
        return tools[normalized]
    }

    @Synchronized
    fun manifests(includeDisabled: Boolean = false): List<ToolManifest> = tools.values
        .asSequence()
        .filter { includeDisabled || it.manifest.id !in disabled }
        .map { it.manifest }
        .sortedBy { it.id }
        .toList()

    @Synchronized
    fun discover(
        query: String,
        requiredTags: Set<String> = emptySet(),
        limit: Int = 10
    ): List<ToolManifest> {
        require(limit > 0) { "Discovery limit must be positive." }
        val terms = tokenize(query)
        val normalizedTags = requiredTags.map(String::lowercase).toSet()
        return tools.values
            .asSequence()
            .filter { it.manifest.id !in disabled }
            .map { it.manifest }
            .filter { manifest -> normalizedTags.all { it in manifest.tags.map(String::lowercase) } }
            .map { manifest -> manifest to score(manifest, terms) }
            .filter { terms.isEmpty() || it.second > 0 }
            .sortedWith(compareByDescending<Pair<ToolManifest, Int>> { it.second }.thenBy { it.first.id })
            .take(limit)
            .map { it.first }
            .toList()
    }

    private fun score(manifest: ToolManifest, terms: Set<String>): Int {
        if (terms.isEmpty()) return 1
        val id = tokenize(manifest.id)
        val name = tokenize(manifest.displayName)
        val description = tokenize(manifest.description)
        val tags = manifest.tags.flatMap(::tokenize).toSet()
        return terms.sumOf { term ->
            when {
                term in id -> 8
                term in name -> 6
                term in tags -> 4
                term in description -> 2
                else -> 0
            }
        }
    }

    private fun tokenize(value: String): Set<String> = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
        .toSet()
}
