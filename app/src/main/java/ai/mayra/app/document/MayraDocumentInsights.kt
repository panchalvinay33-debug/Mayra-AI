package ai.mayra.app.document

import java.text.Normalizer
import java.util.Locale

/** A grounded answer produced only from locally indexed document text. */
data class DocumentAnswer(
    val document: MayraDocument,
    val answer: String,
    val evidence: List<String>,
    val confidence: Int
)

enum class DocumentQueryIntent {
    SEARCH,
    SUMMARY,
    QUESTION
}

/** Pure deterministic document insight engine. It never invents content outside the supplied text. */
object DocumentInsightEngine {
    fun detectIntent(message: String): DocumentQueryIntent {
        val normalized = normalizeForMatching(message)
        return when {
            SUMMARY_MARKERS.any(normalized::contains) -> DocumentQueryIntent.SUMMARY
            QUESTION_MARKERS.any(normalized::contains) || normalized.trim().endsWith("?") ->
                DocumentQueryIntent.QUESTION
            else -> DocumentQueryIntent.SEARCH
        }
    }

    fun summarize(text: String, maxSentences: Int = 4): String {
        val sentences = splitSentences(text)
        if (sentences.isEmpty()) return ""
        if (sentences.size <= maxSentences) return sentences.joinToString(" ")

        val frequencies = wordFrequencies(sentences.joinToString(" "))
        return sentences.mapIndexed { index, sentence ->
            val words = meaningfulTokens(sentence)
            val relevance = words.sumOf { frequencies[it] ?: 0 }
            val positionBonus = when (index) {
                0 -> 5
                1 -> 3
                else -> 0
            }
            RankedSentence(index, sentence, relevance + positionBonus)
        }
            .sortedWith(compareByDescending<RankedSentence> { it.score }.thenBy { it.index })
            .take(maxSentences.coerceIn(1, 8))
            .sortedBy { it.index }
            .joinToString(" ") { it.text }
    }

    fun answer(question: String, text: String, maxEvidence: Int = 3): Pair<String, List<String>>? {
        val queryTerms = meaningfulTokens(question).distinct()
        if (queryTerms.isEmpty()) return null
        val meaningfulPhrase = queryTerms.takeIf { it.size > 1 }?.joinToString(" ")

        val ranked = splitSentences(text).mapIndexedNotNull { index, sentence ->
            val normalized = normalizeForMatching(sentence)
            val matchedTerms = queryTerms.filter { normalized.containsWholeTerm(it) }
            if (matchedTerms.isEmpty()) return@mapIndexedNotNull null

            val exactPhraseBonus = if (
                meaningfulPhrase != null && normalized.contains(meaningfulPhrase)
            ) 8 else 0
            val coverageBonus = if (matchedTerms.size == queryTerms.size && queryTerms.size > 1) 5 else 0
            val density = matchedTerms.size * 5
            val occurrenceScore = matchedTerms.sumOf {
                normalized.countWholeTermOccurrences(it).coerceAtMost(4)
            }
            RankedSentence(index, sentence, density + occurrenceScore + exactPhraseBonus + coverageBonus)
        }
            .sortedWith(compareByDescending<RankedSentence> { it.score }.thenBy { it.index })
            .take(maxEvidence.coerceIn(1, 5))

        if (ranked.isEmpty()) return null
        val evidence = ranked.sortedBy { it.index }.map { it.text }
        return evidence.joinToString(" ") to evidence
    }

    fun confidence(question: String, evidence: List<String>): Int {
        val terms = meaningfulTokens(question).distinct()
        if (terms.isEmpty() || evidence.isEmpty()) return 0
        val combined = normalizeForMatching(evidence.joinToString(" "))
        val matched = terms.count { combined.containsWholeTerm(it) }
        return ((matched.toDouble() / terms.size) * 100).toInt().coerceIn(1, 100)
    }

    private fun splitSentences(text: String): List<String> = normalizeDocumentText(text)
        .split(Regex("(?<=[.!?।])\\s+|\\n+"))
        .asSequence()
        .map { it.trim() }
        .filter { it.length >= 20 }
        .map { it.take(MAX_SENTENCE_CHARS) }
        .take(MAX_SENTENCES)
        .toList()

    private fun wordFrequencies(text: String): Map<String, Int> = meaningfulTokens(text)
        .groupingBy { it }
        .eachCount()

    private fun meaningfulTokens(value: String): List<String> = normalizeForMatching(value)
        .split(Regex("[^\\p{L}\\p{M}\\p{N}_-]+"))
        .asSequence()
        .map(String::trim)
        .filter { it.length >= 3 }
        .filterNot { it in STOP_WORDS }
        .take(MAX_TOKENS)
        .toList()

    private fun normalizeForMatching(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)

    private fun String.containsWholeTerm(term: String): Boolean = wholeTermRegex(term).containsMatchIn(this)

    private fun String.countWholeTermOccurrences(term: String): Int = wholeTermRegex(term)
        .findAll(this)
        .count()

    private fun wholeTermRegex(term: String): Regex = Regex(
        "(?<![\\p{L}\\p{M}\\p{N}_-])${Regex.escape(term)}(?![\\p{L}\\p{M}\\p{N}_-])"
    )

    private data class RankedSentence(val index: Int, val text: String, val score: Int)

    private val SUMMARY_MARKERS = listOf(
        "summary", "summarize", "summarise", "short notes", "overview",
        "सारांश", "संक्षेप", "समरी"
    )

    private val QUESTION_MARKERS = listOf(
        "what", "when", "where", "who", "why", "how", "which", "tell me",
        "क्या", "कब", "कहाँ", "कौन", "क्यों", "कैसे", "कितना", "बताओ"
    )

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "from", "this", "that", "are", "was", "were",
        "have", "has", "had", "into", "about", "your", "documents", "document", "file",
        "files", "library", "mayra", "please", "tell", "show", "find", "summary",
        "summarize", "mera", "meri", "mere", "mein", "hai", "kya", "ka", "ki", "ke",
        "ko", "batao", "dikhao", "दस्तावेज", "फाइल", "फ़ाइल", "बताओ", "क्या",
        "मेरा", "मेरी", "मेरे", "में", "का", "की", "के", "को", "खोजो", "ढूंढो", "ढूँढो"
    )

    private const val MAX_SENTENCE_CHARS = 700
    private const val MAX_SENTENCES = 1_500
    private const val MAX_TOKENS = 30_000
}

class MayraDocumentInsights(
    private val documentStore: MayraDocumentStore,
    private val contentStore: MayraDocumentContentStore
) {
    fun summarize(document: MayraDocument): String? {
        val content = contentStore.get(document.uri)?.text.orEmpty()
        return DocumentInsightEngine.summarize(content).takeIf(String::isNotBlank)
    }

    fun answer(question: String, candidates: List<MayraDocument>): List<DocumentAnswer> = candidates
        .mapNotNull { document ->
            val content = contentStore.get(document.uri)?.text.orEmpty()
            val (answer, evidence) = DocumentInsightEngine.answer(question, content) ?: return@mapNotNull null
            DocumentAnswer(
                document = document,
                answer = answer,
                evidence = evidence,
                confidence = DocumentInsightEngine.confidence(question, evidence)
            )
        }
        .sortedByDescending { it.confidence }
}
