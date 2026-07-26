package ai.mayra.app.core

import java.text.Normalizer
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
        val normalized = normalize(message)
        if (normalized.isBlank()) return delegate()

        val documentSignals = DOCUMENT_MARKERS.filter { normalized.containsMarker(it) }
        if (documentSignals.isEmpty()) return delegate()

        val insightSignals = DOCUMENT_INSIGHT_MARKERS.filter { normalized.containsMarker(it) }
        val questionSignals = QUESTION_MARKERS.filter { marker ->
            normalized.startsWithMarker(marker) || normalized.containsMarker(marker)
        }
        val deviceSignals = DEVICE_ACTION_MARKERS.filter(normalized::startsWith)

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

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .trim()

    private fun String.containsMarker(marker: String): Boolean = markerRegex(marker).containsMatchIn(this)

    private fun String.startsWithMarker(marker: String): Boolean = markerRegex(marker, anchored = true)
        .containsMatchIn(this)

    private fun markerRegex(marker: String, anchored: Boolean = false): Regex {
        val escaped = normalize(marker)
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .joinToString("\\s+") { Regex.escape(it) }
        val prefix = if (anchored) "^" else "(?<![\\p{L}\\p{M}\\p{N}_-])"
        return Regex("$prefix$escaped(?![\\p{L}\\p{M}\\p{N}_-])")
    }

    private val DOCUMENT_MARKERS = listOf(
        "document", "documents", "doc", "docs", "pdf", "file", "files", "library",
        "note", "notes", "दस्तावेज", "फाइल", "फ़ाइल", "पीडीएफ", "नोट", "नोट्स"
    )

    private val DOCUMENT_INSIGHT_MARKERS = listOf(
        "search", "find", "look for", "summarize", "summary", "answer from",
        "inside", "in my", "from my", "indexed", "invoice", "payment terms",
        "खोज", "खोजो", "ढूंढ", "ढूँढ", "सारांश", "इसमें", "मेरी फाइल", "मेरी फ़ाइल"
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
