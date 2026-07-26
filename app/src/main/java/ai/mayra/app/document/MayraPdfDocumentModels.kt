package ai.mayra.app.document

/**
 * Common document extraction contract used by Mayra workspace.
 * Real PDF/OCR providers can plug into this without changing workspace flow.
 */
data class MayraDocumentPageText(
    val pageNumber: Int,
    val text: String,
    val confidence: Double = 1.0
)

data class MayraPdfExtractionResult(
    val uri: String,
    val pages: List<MayraDocumentPageText>,
    val totalPages: Int,
    val searchableText: String,
    val warnings: List<String> = emptyList()
)

data class MayraDocumentSource(
    val uri: String,
    val fileName: String?,
    val mimeType: String?,
    val modifiedTime: Long?
)
