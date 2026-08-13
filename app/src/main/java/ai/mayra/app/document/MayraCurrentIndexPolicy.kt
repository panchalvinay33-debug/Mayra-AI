package ai.mayra.app.document

/**
 * Single source of truth for deciding whether locally stored text is safe to expose to search,
 * summaries and grounded Q&A. Legacy or stale text can remain on disk for repair, but is never
 * treated as current evidence.
 */
class MayraCurrentIndexPolicy(
    private val contentStore: MayraDocumentContentStore,
    private val metadataStore: MayraDocumentIndexMetadataStore
) {
    fun state(document: MayraDocument): DocumentIndexState {
        val content = contentStore.get(document.uri)
        return metadataStore.state(document, content != null)
    }

    fun currentContent(document: MayraDocument): IndexedDocumentContent? {
        val content = contentStore.get(document.uri) ?: return null
        return content.takeIf { metadataStore.state(document, hasIndexedContent = true) == DocumentIndexState.CURRENT }
    }

    fun currentText(document: MayraDocument): String = currentContent(document)?.text.orEmpty()

    fun currentDocuments(documents: List<MayraDocument>): List<MayraDocument> =
        documents.filter { currentContent(it) != null }
}
