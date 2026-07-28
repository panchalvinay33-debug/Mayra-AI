package ai.mayra.app.core

import java.text.Normalizer
import java.util.Locale

enum class MayraQueryRoute {
    DOCUMENTS,
    DELEGATE
}

enum class MayraRoutingOutcome {
    ANSWER,
    RETRIEVE,
    ACT,
    CLARIFY,
    UNSUPPORTED
}

enum class MayraRequiredCapability {
    CORE_ASSISTANT,
    DOCUMENT_LIBRARY,
    DEVICE_ACTIONS,
    DOCUMENT_OCR,
    LEGACY_DOC_PARSER
}

data class MayraRoutingDecision(
    val route: MayraQueryRoute,
    val confidence: Int,
    val matchedSignals: List<String>,
    val outcome: MayraRoutingOutcome = MayraRoutingOutcome.ANSWER,
    val reason: String = "Existing assistant should handle this request.",
    val requiredCapability: MayraRequiredCapability = MayraRequiredCapability.CORE_ASSISTANT,
    val requiresConfirmation: Boolean = false
) {
    init {
        require(confidence in 0..100) { "Routing confidence must be between 0 and 100." }
        require(reason.isNotBlank()) { "Routing decisions require an explicit reason." }
        require(!requiresConfirmation || outcome == MayraRoutingOutcome.ACT) {
            "Only action outcomes may require confirmation."
        }
    }
}

/**
 * Deterministic, local-first query router for Mayra's assistant pipeline.
 *
 * The compatibility route keeps existing document/delegate consumers stable while the typed
 * outcome expresses what the runtime should do next. Provider eligibility and action execution
 * remain separate boundaries; this class only classifies intent.
 */
object MayraQueryRouter {
    fun route(message: String): MayraRoutingDecision {
        val normalized = normalize(message)
        if (normalized.isBlank()) {
            return MayraRoutingDecision(
                route = MayraQueryRoute.DELEGATE,
                confidence = 100,
                matchedSignals = emptyList(),
                outcome = MayraRoutingOutcome.CLARIFY,
                reason = "No usable request text was provided.",
                requiredCapability = MayraRequiredCapability.CORE_ASSISTANT
            )
        }

        val documentSignals = DOCUMENT_MARKERS.filter { normalized.containsMarker(it) }
        val unsupportedOcrSignals = OCR_MARKERS.filter { normalized.containsMarker(it) }
        val unsupportedLegacyDocSignals = LEGACY_DOC_MARKERS.filter { normalized.containsMarker(it) }

        if (documentSignals.isNotEmpty() && unsupportedOcrSignals.isNotEmpty()) {
            return MayraRoutingDecision(
                route = MayraQueryRoute.DELEGATE,
                confidence = 98,
                matchedSignals = (documentSignals + unsupportedOcrSignals).distinct(),
                outcome = MayraRoutingOutcome.UNSUPPORTED,
                reason = "The request needs on-device OCR, which is a deferred capability.",
                requiredCapability = MayraRequiredCapability.DOCUMENT_OCR
            )
        }
        if (unsupportedLegacyDocSignals.isNotEmpty()) {
            return MayraRoutingDecision(
                route = MayraQueryRoute.DELEGATE,
                confidence = 98,
                matchedSignals = unsupportedLegacyDocSignals,
                outcome = MayraRoutingOutcome.UNSUPPORTED,
                reason = "Legacy binary DOC parsing is not available; convert the file to DOCX.",
                requiredCapability = MayraRequiredCapability.LEGACY_DOC_PARSER
            )
        }

        val actionSignals = ACTION_MARKERS.filter { normalized.startsWithMarker(it) }
        val insightSignals = DOCUMENT_INSIGHT_MARKERS.filter { normalized.containsMarker(it) }
        val questionSignals = QUESTION_MARKERS.filter {
            normalized.startsWithMarker(it) || normalized.containsMarker(it)
        }

        if (actionSignals.isNotEmpty() && insightSignals.isEmpty() && questionSignals.isEmpty()) {
            return MayraRoutingDecision(
                route = MayraQueryRoute.DELEGATE,
                confidence = 90,
                matchedSignals = (documentSignals + actionSignals).distinct(),
                outcome = MayraRoutingOutcome.ACT,
                reason = "The request asks Mayra to change device or file state.",
                requiredCapability = MayraRequiredCapability.DEVICE_ACTIONS,
                requiresConfirmation = DESTRUCTIVE_ACTION_MARKERS.any { normalized.startsWithMarker(it) }
            )
        }

        if (documentSignals.isNotEmpty()) {
            val confidence = when {
                insightSignals.isNotEmpty() -> 95
                questionSignals.isNotEmpty() || normalized.endsWith("?") -> 85
                else -> 65
            }
            return MayraRoutingDecision(
                route = MayraQueryRoute.DOCUMENTS,
                confidence = confidence,
                matchedSignals = (documentSignals + insightSignals + questionSignals).distinct(),
                outcome = MayraRoutingOutcome.RETRIEVE,
                reason = "The request explicitly needs evidence from the private document library.",
                requiredCapability = MayraRequiredCapability.DOCUMENT_LIBRARY
            )
        }

        return MayraRoutingDecision(
            route = MayraQueryRoute.DELEGATE,
            confidence = 100,
            matchedSignals = emptyList(),
            outcome = MayraRoutingOutcome.ANSWER,
            reason = "No retrieval or action signal was found; use the existing assistant response path.",
            requiredCapability = MayraRequiredCapability.CORE_ASSISTANT
        )
    }

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

    private val ACTION_MARKERS = listOf(
        "open", "launch", "share", "delete", "rename", "move",
        "खोलो", "शेयर", "हटाओ", "नाम बदलो"
    )

    private val DESTRUCTIVE_ACTION_MARKERS = listOf(
        "delete", "rename", "move", "share", "हटाओ", "नाम बदलो", "शेयर"
    )

    private val OCR_MARKERS = listOf(
        "scanned", "scan text", "ocr", "photo text", "image text",
        "स्कैन", "फोटो से टेक्स्ट", "तस्वीर से टेक्स्ट"
    )

    private val LEGACY_DOC_MARKERS = listOf(
        "legacy doc", "binary doc", ".doc file", "old word file", "पुरानी doc फाइल"
    )
}