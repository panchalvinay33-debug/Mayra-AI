package ai.mayra.app.core

import java.text.Normalizer
import java.util.Locale

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
    OCR,
    LEGACY_DOC
}

data class MayraTypedRoutingDecision(
    val outcome: MayraRoutingOutcome,
    val confidence: Int,
    val reason: String,
    val requiredCapability: MayraRequiredCapability,
    val requiresConfirmation: Boolean,
    val matchedSignals: List<String>
) {
    init {
        require(confidence in 0..100) { "Routing confidence must be between 0 and 100." }
        require(reason.isNotBlank()) { "Routing decisions require an auditable reason." }
        if (outcome != MayraRoutingOutcome.ACT) {
            require(!requiresConfirmation) { "Only action routes may require confirmation." }
        }
    }
}

/**
 * Conservative typed router placed in front of provider and action execution.
 *
 * It does not execute anything. It only declares the next runtime category, required capability,
 * confidence, reason and confirmation boundary. Existing MayraQueryRouter remains available as a
 * compatibility adapter for current document/delegate consumers.
 */
object MayraTypedQueryRouter {
    fun route(message: String): MayraTypedRoutingDecision {
        val normalized = normalize(message)
        if (normalized.isBlank()) {
            return decision(
                outcome = MayraRoutingOutcome.CLARIFY,
                confidence = 100,
                reason = "No usable request was provided.",
                capability = MayraRequiredCapability.CORE_ASSISTANT
            )
        }

        val documentSignals = DOCUMENT_MARKERS.filter { normalized.containsMarker(it) }
        val retrievalSignals = RETRIEVAL_MARKERS.filter { normalized.containsMarker(it) }
        val questionSignals = QUESTION_MARKERS.filter { normalized.containsMarker(it) }
        val actionSignals = ACTION_MARKERS.filter { normalized.startsWithMarker(it) }
        val destructiveSignals = DESTRUCTIVE_ACTION_MARKERS.filter { normalized.containsMarker(it) }
        val ocrSignals = OCR_MARKERS.filter { normalized.containsMarker(it) }
        val legacyDocSignals = LEGACY_DOC_MARKERS.filter { normalized.containsMarker(it) }

        if (ocrSignals.isNotEmpty()) {
            return decision(
                outcome = MayraRoutingOutcome.UNSUPPORTED,
                confidence = 98,
                reason = "The request requires on-device OCR, which is a deferred capability.",
                capability = MayraRequiredCapability.OCR,
                signals = ocrSignals + documentSignals
            )
        }

        if (legacyDocSignals.isNotEmpty()) {
            return decision(
                outcome = MayraRoutingOutcome.UNSUPPORTED,
                confidence = 98,
                reason = "Legacy binary DOC parsing is not available; DOCX is supported.",
                capability = MayraRequiredCapability.LEGACY_DOC,
                signals = legacyDocSignals + documentSignals
            )
        }

        if (actionSignals.isNotEmpty() && retrievalSignals.isEmpty()) {
            val destructive = destructiveSignals.isNotEmpty()
            return decision(
                outcome = MayraRoutingOutcome.ACT,
                confidence = if (destructive) 96 else 90,
                reason = if (destructive) {
                    "The request asks Mayra to change or delete device data and must be confirmed before execution."
                } else {
                    "The request asks Mayra to perform a device-side action."
                },
                capability = MayraRequiredCapability.DEVICE_ACTIONS,
                confirmation = destructive,
                signals = actionSignals + destructiveSignals + documentSignals
            )
        }

        if (documentSignals.isNotEmpty() && (retrievalSignals.isNotEmpty() || questionSignals.isNotEmpty() || normalized.endsWith("?"))) {
            return decision(
                outcome = MayraRoutingOutcome.RETRIEVE,
                confidence = if (retrievalSignals.isNotEmpty()) 96 else 88,
                reason = "The answer depends on current evidence from the private local document library.",
                capability = MayraRequiredCapability.DOCUMENT_LIBRARY,
                signals = documentSignals + retrievalSignals + questionSignals
            )
        }

        if (documentSignals.isNotEmpty()) {
            return decision(
                outcome = MayraRoutingOutcome.CLARIFY,
                confidence = 72,
                reason = "A document was mentioned, but the requested operation is not clear enough to retrieve or act safely.",
                capability = MayraRequiredCapability.DOCUMENT_LIBRARY,
                signals = documentSignals
            )
        }

        return decision(
            outcome = MayraRoutingOutcome.ANSWER,
            confidence = 82,
            reason = "The request can remain with the normal assistant response path and does not require a local capability.",
            capability = MayraRequiredCapability.CORE_ASSISTANT,
            signals = questionSignals
        )
    }

    private fun decision(
        outcome: MayraRoutingOutcome,
        confidence: Int,
        reason: String,
        capability: MayraRequiredCapability,
        confirmation: Boolean = false,
        signals: List<String> = emptyList()
    ) = MayraTypedRoutingDecision(
        outcome = outcome,
        confidence = confidence,
        reason = reason,
        requiredCapability = capability,
        requiresConfirmation = confirmation,
        matchedSignals = signals.distinct()
    )

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .trim()

    private fun String.containsMarker(marker: String): Boolean = markerRegex(marker).containsMatchIn(this)

    private fun String.startsWithMarker(marker: String): Boolean = markerRegex(marker, anchored = true).containsMatchIn(this)

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

    private val RETRIEVAL_MARKERS = listOf(
        "search", "find", "look for", "summarize", "summary", "answer from", "inside",
        "in my", "from my", "indexed", "invoice", "payment terms", "खोज", "खोजो",
        "ढूंढ", "ढूँढ", "सारांश", "इसमें", "मेरी फाइल", "मेरी फ़ाइल"
    )

    private val QUESTION_MARKERS = listOf(
        "what", "when", "where", "who", "which", "how", "why",
        "क्या", "कब", "कहाँ", "कौन", "कैसे", "क्यों", "बताओ"
    )

    private val ACTION_MARKERS = listOf(
        "open", "launch", "share", "delete", "rename", "move", "send", "call", "remind",
        "खोलो", "शेयर", "हटाओ", "डिलीट", "नाम बदलो", "भेजो", "कॉल", "याद दिलाओ"
    )

    private val DESTRUCTIVE_ACTION_MARKERS = listOf(
        "delete", "remove", "erase", "rename", "move", "हटाओ", "डिलीट", "मिटाओ", "नाम बदलो"
    )

    private val OCR_MARKERS = listOf(
        "scan this pdf", "scanned pdf", "image pdf", "read this image", "ocr",
        "स्कैन पीडीएफ", "स्कैन की हुई पीडीएफ", "फोटो से टेक्स्ट", "तस्वीर से टेक्स्ट"
    )

    private val LEGACY_DOC_MARKERS = listOf(
        "legacy doc", "binary doc", ".doc file", "old word file", "पुरानी doc फाइल", "पुरानी वर्ड फाइल"
    )
}
