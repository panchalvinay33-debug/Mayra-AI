package ai.mayra.app.document

import java.util.Locale

/** Aggregate result for a complete local-library maintenance pass. */
data class DocumentMaintenanceReport(
    val totalDocuments: Int,
    val indexed: Int,
    val unsupported: Int,
    val failed: Int,
    val blank: Int,
    val truncated: Int,
    val removedOrphanedIndexes: Int,
    val messages: List<String>
) {
    val completed: Int get() = indexed + unsupported + failed + blank
    val healthy: Boolean get() = failed == 0 && completed == totalDocuments

    fun userMessage(): String = buildString {
        append("Checked $totalDocuments document${if (totalDocuments == 1) "" else "s"}. ")
        append("Indexed $indexed")
        if (blank > 0) append(", no readable text $blank")
        if (unsupported > 0) append(", waiting for parser $unsupported")
        if (failed > 0) append(", failed $failed")
        if (truncated > 0) append(", safely limited $truncated")
        if (removedOrphanedIndexes > 0) append(", removed stale indexes $removedOrphanedIndexes")
        append('.')
    }
}

data class DocumentParserCapability(
    val id: String,
    val label: String,
    val extensions: Set<String>,
    val mimePrefixes: Set<String>,
    val state: ParserCapabilityState,
    val note: String
)

enum class ParserCapabilityState { READY, FOUNDATION_ONLY, PLANNED }

/** Central parser capability catalog used by UI and maintenance diagnostics. */
object MayraDocumentParserCatalog {
    val capabilities: List<DocumentParserCapability> = listOf(
        DocumentParserCapability(
            id = "plain-text",
            label = "Plain text",
            extensions = setOf("txt", "md", "csv", "json", "xml", "log"),
            mimePrefixes = setOf("text/", "application/json", "application/xml"),
            state = ParserCapabilityState.READY,
            note = "Local extraction, indexing, search, summaries and grounded Q&A are available."
        ),
        DocumentParserCapability(
            id = "pdf",
            label = "PDF",
            extensions = setOf("pdf"),
            mimePrefixes = setOf("application/pdf"),
            state = ParserCapabilityState.READY,
            note = "Text-based PDFs are parsed locally, up to 100 pages, 50 MB and 500,000 indexed characters. Scanned-image PDFs still require OCR."
        ),
        DocumentParserCapability(
            id = "office",
            label = "Word documents",
            extensions = setOf("doc", "docx"),
            mimePrefixes = setOf(
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            ),
            state = ParserCapabilityState.PLANNED,
            note = "Library access is ready; DOC/DOCX text parsing is not yet implemented."
        ),
        DocumentParserCapability(
            id = "ocr",
            label = "Scanned pages and images",
            extensions = setOf("png", "jpg", "jpeg", "webp"),
            mimePrefixes = setOf("image/"),
            state = ParserCapabilityState.PLANNED,
            note = "OCR requires a dedicated on-device recognition provider and device validation."
        )
    )

    fun capabilityFor(document: MayraDocument): DocumentParserCapability? {
        val extension = document.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = document.mimeType.lowercase(Locale.ROOT)
        return capabilities.firstOrNull { capability ->
            extension in capability.extensions || capability.mimePrefixes.any(mime::startsWith)
        }
    }

    fun statusText(document: MayraDocument): String {
        val capability = capabilityFor(document) ?: return "No parser capability is registered for this file type."
        return "${capability.label}: ${capability.note}"
    }
}

/** Coordinates safe repeatable indexing without putting maintenance logic in Compose UI. */
class MayraDocumentMaintenance(
    private val documentStore: MayraDocumentStore,
    private val contentStore: MayraDocumentContentStore,
    private val extractor: MayraDocumentTextExtractor
) {
    fun rebuildAll(): DocumentMaintenanceReport {
        val documents = documentStore.list()
        val removedOrphanedIndexes = contentStore.removeExcept(documents.mapTo(mutableSetOf()) { it.uri })
        var indexed = 0
        var unsupported = 0
        var failed = 0
        var blank = 0
        var truncated = 0
        val messages = mutableListOf<String>()

        documents.forEach { document ->
            when (val result = extractor.extract(document)) {
                is DocumentExtractionResult.Success -> {
                    if (result.text.isBlank()) {
                        contentStore.remove(document.uri)
                        blank++
                        messages += "${document.name}: no readable text; scanned PDFs may require OCR"
                    } else {
                        contentStore.put(document.uri, result.text, result.truncated)
                        indexed++
                        if (result.truncated) truncated++
                    }
                }
                is DocumentExtractionResult.Unsupported -> {
                    contentStore.remove(document.uri)
                    unsupported++
                    messages += "${document.name}: ${result.reason}"
                }
                is DocumentExtractionResult.Failure -> {
                    failed++
                    messages += "${document.name}: ${result.reason}"
                }
            }
        }

        return DocumentMaintenanceReport(
            totalDocuments = documents.size,
            indexed = indexed,
            unsupported = unsupported,
            failed = failed,
            blank = blank,
            truncated = truncated,
            removedOrphanedIndexes = removedOrphanedIndexes,
            messages = messages.take(20)
        )
    }
}