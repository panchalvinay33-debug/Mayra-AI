package ai.mayra.app.document

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import android.content.Context
import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.util.Locale

/** Content kept locally for document search. Nothing is uploaded by this layer. */
data class IndexedDocumentContent(
    val uri: String,
    val text: String,
    val indexedAt: Long,
    val truncated: Boolean = false
)

data class DocumentSearchHit(
    val document: MayraDocument,
    val score: Int,
    val snippet: String,
    val matchedContent: Boolean
)

sealed interface DocumentExtractionResult {
    data class Success(val text: String, val truncated: Boolean) : DocumentExtractionResult
    data class Unsupported(val reason: String) : DocumentExtractionResult
    data class Failure(val reason: String) : DocumentExtractionResult
}

class MayraDocumentTextExtractor(private val context: Context) {
    fun extract(document: MayraDocument): DocumentExtractionResult {
        val mimeType = document.mimeType.lowercase(Locale.ROOT)
        val name = document.name.lowercase(Locale.ROOT)
        val textCompatible = mimeType.startsWith("text/") ||
            name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".csv") ||
            name.endsWith(".json") || name.endsWith(".xml") || name.endsWith(".log")

        if (!textCompatible) {
            return DocumentExtractionResult.Unsupported(
                "Text extraction is currently available for plain-text documents. PDF and DOC parsing require a dedicated parser milestone."
            )
        }

        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(document.uri))?.bufferedReader()?.use { reader ->
                val buffer = CharArray(BUFFER_SIZE)
                val output = StringBuilder()
                var truncated = false
                while (true) {
                    val read = reader.read(buffer)
                    if (read <= 0) break
                    val remaining = MAX_INDEXED_CHARS - output.length
                    if (remaining <= 0) {
                        truncated = true
                        break
                    }
                    output.append(buffer, 0, minOf(read, remaining))
                    if (output.length >= MAX_INDEXED_CHARS) {
                        truncated = reader.read() >= 0
                        break
                    }
                }
                DocumentExtractionResult.Success(
                    text = normalizeDocumentText(output.toString()),
                    truncated = truncated
                )
            } ?: DocumentExtractionResult.Failure("The document stream could not be opened.")
        }.getOrElse {
            DocumentExtractionResult.Failure(it.message ?: "Text extraction failed.")
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8_192
        const val MAX_INDEXED_CHARS = 500_000
    }
}

class MayraDocumentContentStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun get(uri: String): IndexedDocumentContent? {
        val encoded = preferences.getString(key(uri), null) ?: return null
        val split = encoded.indexOf(SEPARATOR)
        if (split <= 0) return null
        val header = encoded.substring(0, split).split(',')
        if (header.size != 2) return null
        return runCatching {
            IndexedDocumentContent(
                uri = uri,
                text = encoded.substring(split + 1),
                indexedAt = header[0].toLong(),
                truncated = header[1].toBooleanStrictOrNull() ?: false
            )
        }.getOrNull()
    }

    fun put(uri: String, text: String, truncated: Boolean) {
        val normalized = normalizeDocumentText(text)
        preferences.edit().putString(
            key(uri),
            "${System.currentTimeMillis()},$truncated$SEPARATOR$normalized"
        ).apply()
    }

    fun remove(uri: String) {
        preferences.edit().remove(key(uri)).apply()
    }

    /** Removes indexes whose documents no longer exist in the Mayra Library. */
    fun removeExcept(uris: Set<String>): Int {
        val retainedKeys = uris.mapTo(mutableSetOf(), ::key)
        val orphanedKeys = preferences.all.keys.filterNot(retainedKeys::contains)
        if (orphanedKeys.isEmpty()) return 0

        preferences.edit().apply {
            orphanedKeys.forEach(::remove)
        }.apply()
        return orphanedKeys.size
    }

    private fun key(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private companion object {
        const val FILE_NAME = "mayra_document_content"
        const val SEPARATOR = '\u001D'
    }
}

class MayraDocumentSearch(
    private val documentStore: MayraDocumentStore,
    private val contentStore: MayraDocumentContentStore
) {
    fun search(query: String, limit: Int = 5): List<DocumentSearchHit> {
        val documents = documentStore.list()
        val indexed = documents.associateWith { contentStore.get(it.uri)?.text.orEmpty() }
        return DocumentSearchEngine.search(documents, indexed, query, limit)
    }
}

/** Pure deterministic search engine, kept Android-free so it can be unit-tested. */
object DocumentSearchEngine {
    fun search(
        documents: List<MayraDocument>,
        indexedText: Map<MayraDocument, String>,
        query: String,
        limit: Int = 5
    ): List<DocumentSearchHit> {
        val terms = tokenizeQuery(query)
        if (terms.isEmpty()) return emptyList()
        val phrase = terms.takeIf { it.size > 1 }?.joinToString(" ")

        return documents.mapNotNull { document ->
            val name = document.name.lowercase(Locale.ROOT)
            val mime = document.mimeType.lowercase(Locale.ROOT)
            val originalContent = indexedText[document].orEmpty()
            val normalizedContent = originalContent.lowercase(Locale.ROOT)
            var score = 0
            var matchedContent = false
            var matchedTerms = 0

            terms.forEach { term ->
                var termMatched = false
                if (name == term) {
                    score += 14
                    termMatched = true
                }
                if (name.contains(term)) {
                    score += 8
                    termMatched = true
                }
                if (mime.contains(term)) {
                    score += 2
                    termMatched = true
                }
                val occurrences = normalizedContent.countWholeTermOccurrences(term).coerceAtMost(8)
                if (occurrences > 0) {
                    score += 3 + occurrences
                    matchedContent = true
                    termMatched = true
                }
                if (termMatched) matchedTerms++
            }

            if (phrase != null) {
                if (name.contains(phrase)) score += 12
                if (normalizedContent.contains(phrase)) {
                    score += 6
                    matchedContent = true
                }
            }
            if (matchedTerms == terms.size && terms.size > 1) score += 4

            if (score == 0) null else DocumentSearchHit(
                document = document,
                score = score,
                snippet = if (matchedContent) contentSnippet(originalContent, terms, phrase) else document.name,
                matchedContent = matchedContent
            )
        }.sortedWith(
            compareByDescending<DocumentSearchHit> { it.score }
                .thenByDescending { it.document.lastOpenedAt }
                .thenByDescending { it.document.addedAt }
        ).take(limit.coerceIn(1, 20))
    }

    fun tokenizeQuery(query: String): List<String> = query
        .lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{M}\\p{N}_-]+"))
        .asSequence()
        .map(String::trim)
        .filter { it.length >= 2 }
        .filterNot { it in STOP_WORDS }
        .distinct()
        .take(12)
        .toList()

    private fun contentSnippet(content: String, terms: List<String>, phrase: String?): String {
        val normalized = content.lowercase(Locale.ROOT)
        val phraseIndex = phrase?.let(normalized::indexOf)?.takeIf { it >= 0 }
        val termIndex = terms.mapNotNull { normalized.firstWholeTermIndex(it) }.minOrNull()
        val firstIndex = phraseIndex ?: termIndex ?: 0
        val start = (firstIndex - 70).coerceAtLeast(0)
        val end = (firstIndex + 180).coerceAtMost(content.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < content.length) "…" else ""
        return prefix + content.substring(start, end).replace(Regex("\\s+"), " ").trim() + suffix
    }

    private fun String.countWholeTermOccurrences(term: String): Int =
        wholeTermRegex(term).findAll(this).count()

    private fun String.firstWholeTermIndex(term: String): Int? =
        wholeTermRegex(term).find(this)?.range?.first

    private fun wholeTermRegex(term: String): Regex = Regex(
        "(?<![\\p{L}\\p{M}\\p{N}_-])${Regex.escape(term)}(?![\\p{L}\\p{M}\\p{N}_-])"
    )

    private val STOP_WORDS = setOf(
        "the", "and", "for", "with", "from", "this", "that", "my", "your", "our",
        "in", "to", "of", "on", "about", "file", "files", "document", "documents",
        "library", "mayra", "please", "show", "find", "tell", "mera", "meri", "mere",
        "mein", "me", "hai", "ka", "ki", "ke", "ko", "kya", "batao", "dikhao",
        "karo", "kar", "karna", "do", "de", "search", "मेरा", "मेरी", "मेरे", "में",
        "को", "का", "की", "के", "क्या", "बताओ", "दिखाओ", "खोजो", "ढूंढो", "ढूँढो",
        "फाइल", "फ़ाइल", "दस्तावेज", "दस्तावेज़", "पीडीएफ"
    )
}

/**
 * Adds local document lookup to the existing assistant without replacing command handling.
 * Queries must clearly mention the document/library context to avoid hijacking normal chat.
 */
class DocumentAwareMayraAssistant(
    private val delegate: MayraAssistant,
    context: Context
) : MayraAssistant {
    private val documentStore = MayraDocumentStore(context)
    private val search = MayraDocumentSearch(documentStore, MayraDocumentContentStore(context))

    override suspend fun reply(
        message: String,
        conversation: List<MayraMessage>
    ): Result<String> {
        if (!looksLikeDocumentQuery(message)) return delegate.reply(message, conversation)

        return runCatching {
            val documents = documentStore.list()
            if (documents.isEmpty()) {
                return@runCatching "Your Mayra Library is empty. Open Mayra Library and add a document first."
            }

            val hits = search.search(message, limit = 5)
            if (hits.isEmpty()) {
                return@runCatching "I checked ${documents.size} document${if (documents.size == 1) "" else "s"} in your local Mayra Library, but found no matching title or indexed text. Plain-text files can be indexed now; PDF and DOC text extraction is a later parser milestone."
            }

            buildString {
                append("I found ${hits.size} local document match${if (hits.size == 1) "" else "es"}:\n")
                hits.forEachIndexed { index, hit ->
                    append("\n${index + 1}. ${hit.document.name}")
                    if (hit.matchedContent && hit.snippet.isNotBlank()) append("\n   ${hit.snippet}")
                }
                append("\n\nThese results came from your on-device Mayra Library.")
            }
        }
    }

    private fun looksLikeDocumentQuery(message: String): Boolean {
        val normalized = message.lowercase(Locale.ROOT)
        return listOf(
            "document", "documents", "pdf", "file", "files", "library",
            "दस्तावेज", "फाइल", "फ़ाइल", "पीडीएफ"
        ).any(normalized::contains)
    }
}

internal fun normalizeDocumentText(value: String): String = value
    .replace("\u0000", "")
    .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()
