package ai.mayra.app.document

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.MayraQueryRoute
import ai.mayra.app.core.MayraQueryRouter
import android.content.Context
import java.util.Locale

/**
 * Offline document assistant that uses Mayra's local-first router before running deterministic
 * document search, summaries, or grounded answers. Existing device-command behavior remains
 * delegated when the router does not select the document route.
 */
class DocumentInsightAwareMayraAssistant(
    private val delegate: MayraAssistant,
    context: Context
) : MayraAssistant {
    private val documentStore = MayraDocumentStore(context)
    private val contentStore = MayraDocumentContentStore(context)
    private val search = MayraDocumentSearch(documentStore, contentStore)
    private val insights = MayraDocumentInsights(documentStore, contentStore)

    override suspend fun reply(
        message: String,
        conversation: List<MayraMessage>
    ): Result<String> {
        val routing = MayraQueryRouter.route(message)
        if (routing.route != MayraQueryRoute.DOCUMENTS) {
            return delegate.reply(message, conversation)
        }

        return runCatching {
            val documents = documentStore.list()
            if (documents.isEmpty()) {
                return@runCatching "Your Mayra Library is empty. Open Mayra Library and add a document first."
            }

            val hits = search.search(message, limit = 5)
            val intent = DocumentInsightEngine.detectIntent(message)
            when (intent) {
                DocumentQueryIntent.SEARCH -> searchReply(documents.size, hits)
                DocumentQueryIntent.SUMMARY -> summaryReply(message, documents, hits)
                DocumentQueryIntent.QUESTION -> answerReply(message, documents, hits)
            }
        }
    }

    private fun searchReply(totalDocuments: Int, hits: List<DocumentSearchHit>): String {
        if (hits.isEmpty()) {
            return noMatchReply(totalDocuments)
        }
        return buildString {
            append("I found ${hits.size} local document match${if (hits.size == 1) "" else "es"}:\n")
            hits.forEachIndexed { index, hit ->
                append("\n${index + 1}. ${hit.document.name}")
                if (hit.matchedContent && hit.snippet.isNotBlank()) append("\n   ${hit.snippet}")
            }
            append("\n\nThese results came from your on-device Mayra Library.")
        }
    }

    private fun summaryReply(
        message: String,
        documents: List<MayraDocument>,
        hits: List<DocumentSearchHit>
    ): String {
        val target = hits.firstOrNull()?.document
            ?: bestNamedDocument(message, documents)
            ?: documents.singleOrNull()
            ?: return "Please include the document name or a topic so I know which local document to summarize."

        val summary = insights.summarize(target)
            ?: return "${target.name} is in your library, but it has no searchable text index yet. Re-index it from Mayra Library. PDF and DOC page parsing still require the dedicated parser milestone."

        return buildString {
            append("Local summary of ${target.name}:\n\n")
            append(summary)
            append("\n\nThis is an extractive summary built only from the text indexed on your device.")
        }
    }

    private fun answerReply(
        message: String,
        documents: List<MayraDocument>,
        hits: List<DocumentSearchHit>
    ): String {
        val candidates = when {
            hits.isNotEmpty() -> hits.map { it.document }
            documents.size <= 5 -> documents
            else -> documents.take(5)
        }
        val answers = insights.answer(message, candidates).take(3)
        if (answers.isEmpty()) return noMatchReply(documents.size)

        return buildString {
            append("I found this in your local documents:\n")
            answers.forEachIndexed { index, answer ->
                append("\n${index + 1}. ${answer.document.name} (${answer.confidence}% term coverage)\n")
                append(answer.answer)
            }
            append("\n\nI used only indexed on-device text. I did not fill gaps with guesses.")
        }
    }

    private fun bestNamedDocument(message: String, documents: List<MayraDocument>): MayraDocument? {
        val normalized = message.lowercase(Locale.ROOT)
        return documents
            .filter { document ->
                val baseName = document.name.substringBeforeLast('.').lowercase(Locale.ROOT)
                baseName.length >= 3 && normalized.contains(baseName)
            }
            .maxByOrNull { it.name.length }
    }

    private fun noMatchReply(totalDocuments: Int): String =
        "I checked $totalDocuments local document${if (totalDocuments == 1) "" else "s"}, but found no grounded answer in the indexed text. Try a document name or more specific keywords. Plain-text compatible files can be indexed now; reliable PDF and DOC parsing is the next parser milestone."
}
