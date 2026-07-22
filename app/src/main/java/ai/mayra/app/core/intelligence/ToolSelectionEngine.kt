package ai.mayra.app.core.intelligence

data class ToolSelection(
    val manifest: ToolManifest,
    val score: Int,
    val matchedTerms: Set<String>
)

class ToolSelectionEngine(
    private val registry: ToolRegistry
) {
    fun select(
        userInput: String,
        requiredTags: Set<String> = emptySet(),
        limit: Int = 3
    ): List<ToolSelection> {
        require(userInput.isNotBlank()) { "User input cannot be blank." }
        require(limit > 0) { "Selection limit must be positive." }
        val terms = tokenize(userInput)
        return registry.discover(userInput, requiredTags, limit * 3)
            .map { manifest ->
                val idTerms = tokenize(manifest.id)
                val nameTerms = tokenize(manifest.displayName)
                val descriptionTerms = tokenize(manifest.description)
                val tagTerms = manifest.tags.flatMap(::tokenize).toSet()
                val matched = terms.intersect(idTerms + nameTerms + descriptionTerms + tagTerms)
                val score = matched.sumOf { term ->
                    when {
                        term in idTerms -> 10
                        term in nameTerms -> 8
                        term in tagTerms -> 5
                        else -> 2
                    }
                } + if (manifest.riskLevel == ToolRiskLevel.LOW) 1 else 0
                ToolSelection(manifest, score, matched)
            }
            .filter { it.score > 0 }
            .sortedWith(compareByDescending<ToolSelection> { it.score }.thenBy { it.manifest.id })
            .take(limit)
    }

    private fun tokenize(value: String): Set<String> = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter { it.length > 1 }
        .toSet()
}
