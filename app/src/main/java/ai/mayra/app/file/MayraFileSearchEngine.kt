package ai.mayra.app.file

import ai.mayra.app.workspace.MayraSourceReference
import android.content.Context

data class MayraFileSearchResult(
    val query: String,
    val matches: List<MayraIndexedFile>,
    val sourceReferences: List<MayraSourceReference>,
    val indexGeneration: Long,
    val indexedAt: Long
) {
    val found: Boolean get() = matches.isNotEmpty()
}

class MayraFileSearchEngine(context: Context) {
    private val store = MayraEncryptedFileIndexStore(context.applicationContext)

    fun search(query: String, limit: Int = 12): MayraFileSearchResult {
        val safeQuery = query.trim().replace(Regex("\\s+"), " ").take(240)
        require(safeQuery.isNotBlank()) { "File search query cannot be blank." }
        val snapshot = store.read()
        val matches = snapshot.search(safeQuery, limit)
        return MayraFileSearchResult(
            query = safeQuery,
            matches = matches,
            sourceReferences = matches.map { file ->
                MayraSourceReference(
                    uri = file.uri,
                    displayName = file.displayName,
                    confidence = metadataConfidence(safeQuery, file),
                    excerpt = file.relativeLocation?.let { "Folder: $it" }
                )
            },
            indexGeneration = snapshot.generation,
            indexedAt = snapshot.updatedAt
        )
    }

    private fun metadataConfidence(query: String, file: MayraIndexedFile): Double {
        val tokens = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 }.distinct()
        if (tokens.isEmpty()) return 0.0
        val text = file.searchableText()
        return (tokens.count(text::contains).toDouble() / tokens.size).coerceIn(0.0, 1.0)
    }
}
