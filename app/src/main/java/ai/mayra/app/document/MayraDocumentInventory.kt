package ai.mayra.app.document

/** Read-only snapshot of the local library. No document content is opened while building it. */
data class DocumentLibraryInventory(
    val totalDocuments: Int,
    val indexedDocuments: Int,
    val needsIndexing: Int,
    val readyFormatDocuments: Int,
    val plannedFormatDocuments: Int,
    val unknownFormatDocuments: Int,
    val formatCounts: Map<String, Int>,
    val currentIndexes: Int = 0,
    val legacyIndexes: Int = 0,
    val staleSourceIndexes: Int = 0,
    val staleParserIndexes: Int = 0
) {
    val staleIndexes: Int get() = staleSourceIndexes + staleParserIndexes
    val fullyIndexed: Boolean
        get() = totalDocuments > 0 && indexedDocuments == totalDocuments && staleIndexes == 0 && legacyIndexes == 0

    fun userMessage(): String = buildString {
        append("$totalDocuments saved document${if (totalDocuments == 1) "" else "s"}; ")
        append("$indexedDocuments indexed")
        if (currentIndexes > 0) append(", $currentIndexes current")
        if (needsIndexing > 0) append(", $needsIndexing need indexing")
        if (legacyIndexes > 0) append(", $legacyIndexes legacy index")
        if (staleIndexes > 0) append(", $staleIndexes stale")
        if (plannedFormatDocuments > 0) append(", $plannedFormatDocuments need a future parser")
        if (unknownFormatDocuments > 0) append(", $unknownFormatDocuments unknown format")
        append('.')
    }
}

object MayraDocumentInventory {
    fun build(
        documents: List<MayraDocument>,
        indexedUris: Set<String>,
        indexStates: Map<String, DocumentIndexState> = emptyMap()
    ): DocumentLibraryInventory {
        var ready = 0
        var planned = 0
        var unknown = 0
        var current = 0
        var legacy = 0
        var staleSource = 0
        var staleParser = 0
        val counts = linkedMapOf<String, Int>()

        documents.forEach { document ->
            val capability = MayraDocumentParserCatalog.capabilityFor(document)
            val key = capability?.id ?: "unknown"
            counts[key] = counts.getOrDefault(key, 0) + 1
            when (capability?.state) {
                ParserCapabilityState.READY -> ready++
                ParserCapabilityState.FOUNDATION_ONLY,
                ParserCapabilityState.PLANNED -> planned++
                null -> unknown++
            }

            when (indexStates[document.uri]) {
                DocumentIndexState.CURRENT -> current++
                DocumentIndexState.LEGACY -> legacy++
                DocumentIndexState.STALE_SOURCE -> staleSource++
                DocumentIndexState.STALE_PARSER -> staleParser++
                else -> Unit
            }
        }

        val indexed = documents.count { it.uri in indexedUris }
        return DocumentLibraryInventory(
            totalDocuments = documents.size,
            indexedDocuments = indexed,
            needsIndexing = (documents.size - indexed).coerceAtLeast(0),
            readyFormatDocuments = ready,
            plannedFormatDocuments = planned,
            unknownFormatDocuments = unknown,
            formatCounts = counts.toMap(),
            currentIndexes = current,
            legacyIndexes = legacy,
            staleSourceIndexes = staleSource,
            staleParserIndexes = staleParser
        )
    }
}
