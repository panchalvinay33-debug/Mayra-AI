package ai.mayra.app.document

/**
 * Deterministic pipeline coordinator for document intelligence.
 * Providers for PDF extraction and OCR can be added behind this boundary.
 */
class MayraDocumentAnalysisPipeline(
    private val billParser: MayraBillParser = MayraBillParser()
) {
    fun analyseTextDocument(
        source: MayraDocumentSource,
        extractedText: String
    ): MayraDocumentAnalysisResult {
        val bill = billParser.parse(extractedText)
        return MayraDocumentAnalysisResult(
            source = source,
            bill = bill,
            extractedText = extractedText.take(MAX_PREVIEW),
            readyForExport = bill.confidence >= MIN_EXPORT_CONFIDENCE
        )
    }

    companion object {
        private const val MAX_PREVIEW = 50000
        private const val MIN_EXPORT_CONFIDENCE = 0.5
    }
}

data class MayraDocumentAnalysisResult(
    val source: MayraDocumentSource,
    val bill: MayraBillRecord,
    val extractedText: String,
    val readyForExport: Boolean
)
