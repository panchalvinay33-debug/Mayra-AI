package ai.mayra.app.document

import ai.mayra.app.file.MayraFileSearchEngine
import ai.mayra.app.workspace.MayraSourceReference
import android.content.Context
import android.net.Uri

data class MayraDocumentAnalysisResult(
    val source: MayraSourceReference?,
    val bill: MayraBillRecord?,
    val summary: String,
    val verified: Boolean,
    val needsPdfOrOcrTool: Boolean
)

class MayraDocumentAnalysisEngine(context: Context) {
    private val fileSearch = MayraFileSearchEngine(context)
    private val textExtractor = MayraLocalTextExtractor(context)
    private val billParser = MayraBillParser()

    fun analyse(query: String): MayraDocumentAnalysisResult {
        val search = fileSearch.search(query, limit = 8)
        if (!search.found) {
            return MayraDocumentAnalysisResult(
                source = null,
                bill = null,
                summary = "No authorized indexed source matched the request.",
                verified = false,
                needsPdfOrOcrTool = false
            )
        }

        val candidates = search.matches.zip(search.sourceReferences)
        candidates.forEach { (file, reference) ->
            val extracted = runCatching {
                textExtractor.extract(Uri.parse(file.uri), file.mimeType)
            }.getOrNull()
            if (extracted != null && extracted.text.isNotBlank()) {
                val bill = billParser.parse(extracted.text)
                val summary = buildString {
                    append("Source verified: ${file.displayName}.")
                    bill.billDate?.let { append(" Bill date $it.") }
                    bill.invoiceNumber?.let { append(" Invoice $it.") }
                    bill.total?.let { append(" Total ${it.stripTrailingZeros().toPlainString()}.") }
                    if (bill.items.isNotEmpty()) append(" ${bill.items.size} line item(s) parsed.")
                    if (extracted.truncated) append(" Text was truncated at the safety limit.")
                }
                return MayraDocumentAnalysisResult(
                    source = reference.copy(excerpt = summary.take(500), confidence = bill.confidence),
                    bill = bill,
                    summary = summary,
                    verified = bill.confidence >= MIN_VERIFIED_CONFIDENCE,
                    needsPdfOrOcrTool = false
                )
            }
        }

        val first = candidates.first()
        return MayraDocumentAnalysisResult(
            source = first.second,
            bill = null,
            summary = "Source file found: ${first.first.displayName}. PDF text extraction or image OCR is still required.",
            verified = false,
            needsPdfOrOcrTool = true
        )
    }

    private companion object { const val MIN_VERIFIED_CONFIDENCE = 0.6 }
}
