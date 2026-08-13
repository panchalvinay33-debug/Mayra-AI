package ai.mayra.app.context

import android.content.Context
import ai.mayra.app.document.DocumentIndexState
import ai.mayra.app.document.MayraDocumentContentStore
import ai.mayra.app.document.MayraDocumentIndexMetadataStore
import ai.mayra.app.document.MayraDocumentStore
import ai.mayra.app.memory.AndroidMayraPersonalMemoryStore
import java.time.LocalDateTime

/**
 * Privacy-safe J6 knowledge readiness. Raw personal memory and document content never cross this
 * boundary; only bounded counts and index-health state are exposed.
 */
data class KnowledgeContextSnapshot(
    val capturedAt: LocalDateTime,
    val memory: ContextValue<MemoryAggregate> = ContextValue.Unavailable,
    val documents: ContextValue<DocumentAggregate> = ContextValue.Unavailable
)

data class MemoryAggregate(val savedCount: Int) {
    init { require(savedCount >= 0) }
}

data class DocumentAggregate(
    val savedCount: Int,
    val currentIndexedCount: Int,
    val needsAttentionCount: Int
) {
    init {
        require(savedCount >= 0)
        require(currentIndexedCount in 0..savedCount)
        require(needsAttentionCount in 0..savedCount)
        require(currentIndexedCount + needsAttentionCount <= savedCount)
    }
}

fun collectKnowledgeContext(
    context: Context,
    capturedAt: LocalDateTime = LocalDateTime.now()
): KnowledgeContextSnapshot {
    val appContext = context.applicationContext

    val memory = runCatching {
        val count = AndroidMayraPersonalMemoryStore(appContext).all().size
        ContextValue.Available(MemoryAggregate(count), ContextSource.MEMORY)
    }.getOrElse { ContextValue.Unavailable }

    val documents = runCatching {
        val store = MayraDocumentStore(appContext)
        val contentStore = MayraDocumentContentStore(appContext)
        val metadataStore = MayraDocumentIndexMetadataStore(appContext)
        val items = store.list()
        var current = 0
        var needsAttention = 0

        items.forEach { document ->
            val hasContent = contentStore.get(document.uri) != null
            when (metadataStore.state(document, hasContent)) {
                DocumentIndexState.CURRENT -> current++
                DocumentIndexState.MISSING,
                DocumentIndexState.LEGACY,
                DocumentIndexState.STALE_SOURCE,
                DocumentIndexState.STALE_PARSER,
                DocumentIndexState.UNSUPPORTED -> needsAttention++
            }
        }

        ContextValue.Available(
            DocumentAggregate(
                savedCount = items.size,
                currentIndexedCount = current,
                needsAttentionCount = needsAttention
            ),
            ContextSource.DOCUMENT_LIBRARY
        )
    }.getOrElse { ContextValue.Unavailable }

    return KnowledgeContextSnapshot(capturedAt, memory, documents)
}

fun KnowledgeContextSnapshot.summaryLines(): List<String> = buildList {
    when (val value = memory) {
        is ContextValue.Available -> add("Memory · ${value.value.savedCount} saved")
        ContextValue.NotGranted -> add("Memory · not enabled")
        ContextValue.Unavailable -> add("Memory · unavailable")
    }
    when (val value = documents) {
        is ContextValue.Available -> {
            val docs = value.value
            add(
                when {
                    docs.savedCount == 0 -> "Library · no documents"
                    docs.needsAttentionCount > 0 ->
                        "Library · ${docs.currentIndexedCount}/${docs.savedCount} current · ${docs.needsAttentionCount} need attention"
                    else -> "Library · ${docs.currentIndexedCount}/${docs.savedCount} current"
                }
            )
        }
        ContextValue.NotGranted -> add("Library · not enabled")
        ContextValue.Unavailable -> add("Library · unavailable")
    }
}
