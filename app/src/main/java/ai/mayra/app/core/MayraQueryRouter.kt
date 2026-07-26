package ai.mayra.app.core

import java.util.Locale

enum class MayraQueryRoute {
    DOCUMENTS,
    DELEGATE
}

data class MayraRoutingDecision(
    val route: MayraQueryRoute,
    val confidence: Int,
    val matchedSignals: List<String>
)

/**
 * Local-first query router for Mayra's assistant pipeline.
 *
 * This first milestone deliberately routes only explicit document-intelligence requests.
 * Everything else stays with the existing assistant so device commands and normal chat do
 * not regress. Memory, connected services and web providers can be added as new routes later.
 */
object MayraQueryRouter {
    fun route(message: String): MayraRoutingDecision {
        val normalized = message.lowercase(Locale.ROOT).trim()
        if (normalized.isBlank()) return delegate()

        val documentSignals = DOCUMENT_MARKERS.filter(normalized::contains)
        if (documentSignals.isEmpty()) return delegate()

        val insightSignals = DOCUMENT_INSIGHT_MARKERS.filter(normalized::contains)
        val questionSignals = QUESTION_MARKERS.filter { marker ->
            normalized.startsWith(marker) || normalized.contains(" $marker ")
        }
        val deviceSignals = DEVICE_ACTION_MARKERS.filter { marker -> normalized.startsWith(marker) }

        if (deviceSignals.isNotEmpty() && insightSignals.isEmpty() && questionSignals.isEmpty()) {
            return MayraRoutingDecision(
                route = MayraQueryRoute.DELEGATE,
                confidence = 90,
                matchedSignals = documentSignals + deviceSignals
            )
        }

        val confidence = when {
            insightSignals.isNotEmpty() -> 95
            questionSignals.isNotEmpty() || normalized.endsWith("?") -> 85
            else -> 65
        }
        return MayraRoutingDecision(
            route = MayraQueryRoute.DOCUMENTS,
            confidence = confidence,
            matchedSignals = (documentSignals + insightSignals + questionSignals).distinct()
        )
    }

    private fun delegate() = MayraRoutingDecision(
        route = MayraQueryRoute.DELEGATE,
        confidence = 100,
        matchedSignals = emptyList()
    )

    private val DOCUMENT_MARKERS = listOf(
        "document", "documents", "pdf", "file", "files", "library",
        "दस्तावेज", "फाइल", "फ़ाइल", "पीडीएफ"
    )

    private val DOCUMENT_INSIGHT_MARKERS = listOf(
        "search", "find", "look for", "summarize", "summary", "answer from",
        "inside", "in my", "from my", "indexed", "invoice", "payment terms",
        "खोज", "ढूंढ", "ढूँढ", "सारांश", "इसमें", "मेरी फाइल", "मेरी फ़ाइल"
    )

    private val QUESTION_MARKERS = listOf(
        "what", "when", "where", "who", "which", "how", "why",
        "क्या", "कब", "कहाँ", "कौन", "कैसे", "क्यों", "बताओ"
    )

    private val DEVICE_ACTION_MARKERS = listOf(
        "open ", "launch ", "share ", "delete ", "rename ", "move ",
        "खोलो ", "शेयर ", "हटाओ ", "नाम बदलो "
    )
}
