package ai.mayra.app.document

/** Result of a verified local index commit. */
sealed interface DocumentIndexCommitResult {
    data class Success(val content: IndexedDocumentContent) : DocumentIndexCommitResult
    data class Failure(val reason: String) : DocumentIndexCommitResult
}

/**
 * Coordinates content and provenance writes as one logical transaction.
 *
 * SharedPreferences cannot atomically commit across two files, so every step is synchronously
 * verified and any partial state is rolled back. Current-only evidence policy then guarantees
 * that an interrupted write is never exposed to search, summaries or grounded Q&A.
 */
class MayraDocumentIndexCoordinator(
    private val contentStore: MayraDocumentContentStore,
    private val metadataStore: MayraDocumentIndexMetadataStore
) {
    @Synchronized
    fun commit(
        document: MayraDocument,
        text: String,
        truncated: Boolean
    ): DocumentIndexCommitResult {
        val normalized = normalizeDocumentText(text)
        if (normalized.isBlank()) {
            remove(document.uri)
            return DocumentIndexCommitResult.Failure("The extracted document text was blank.")
        }

        return runCatching {
            contentStore.put(document.uri, normalized, truncated)
            val stored = contentStore.get(document.uri)
                ?: error("The local text index could not be verified after writing.")
            check(stored.text == normalized && stored.truncated == truncated) {
                "The local text index did not match the committed content."
            }
            check(metadataStore.record(document)) {
                "The parser fingerprint could not be committed."
            }
            check(metadataStore.state(document, hasIndexedContent = true) == DocumentIndexState.CURRENT) {
                "The committed index did not pass freshness verification."
            }
            DocumentIndexCommitResult.Success(stored)
        }.getOrElse { error ->
            remove(document.uri)
            DocumentIndexCommitResult.Failure(
                error.message ?: "The document index transaction failed and was rolled back."
            )
        }
    }

    @Synchronized
    fun remove(uri: String) {
        contentStore.remove(uri)
        metadataStore.remove(uri)
    }
}
