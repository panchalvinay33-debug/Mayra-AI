package ai.mayra.app.document

/**
 * OCR contract models. The actual OCR provider can be swapped later
 * without changing workspace or bill analysis flows.
 */
data class MayraOcrWord(
    val text: String,
    val confidence: Double,
    val page: Int = 1
)

data class MayraOcrResult(
    val sourceId: String,
    val words: List<MayraOcrWord>,
    val fullText: String,
    val averageConfidence: Double,
    val completed: Boolean,
    val warnings: List<String> = emptyList()
)

enum class MayraOcrState {
    NOT_STARTED,
    PROCESSING,
    COMPLETED,
    NEEDS_REVIEW,
    FAILED
}
