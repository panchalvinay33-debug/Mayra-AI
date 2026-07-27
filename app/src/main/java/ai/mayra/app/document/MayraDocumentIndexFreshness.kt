package ai.mayra.app.document

import android.content.Context
import android.util.Base64
import java.security.MessageDigest

data class DocumentIndexFingerprint(
    val parserId: String,
    val parserVersion: Int,
    val sourceSizeBytes: Long,
    val recordedAt: Long
)

enum class DocumentIndexState {
    CURRENT,
    MISSING,
    LEGACY,
    STALE_SOURCE,
    STALE_PARSER,
    UNSUPPORTED
}

/** Version changes intentionally invalidate indexes produced by older parser behavior. */
object MayraDocumentParserVersions {
    private val versions = mapOf(
        "plain-text" to 1,
        "pdf" to 1,
        "docx" to 1
    )

    fun parserFor(document: MayraDocument): Pair<String, Int>? {
        val capability = MayraDocumentParserCatalog.capabilityFor(document) ?: return null
        if (capability.state != ParserCapabilityState.READY) return null
        val version = versions[capability.id] ?: return null
        return capability.id to version
    }
}

/** Pure evaluator used by UI, maintenance and tests. */
object MayraDocumentIndexFreshness {
    fun evaluate(
        document: MayraDocument,
        hasIndexedContent: Boolean,
        fingerprint: DocumentIndexFingerprint?
    ): DocumentIndexState {
        val parser = MayraDocumentParserVersions.parserFor(document)
            ?: return DocumentIndexState.UNSUPPORTED
        if (!hasIndexedContent) return DocumentIndexState.MISSING
        if (fingerprint == null) return DocumentIndexState.LEGACY
        if (fingerprint.parserId != parser.first || fingerprint.parserVersion != parser.second) {
            return DocumentIndexState.STALE_PARSER
        }
        if (
            document.sizeBytes >= 0 &&
            fingerprint.sourceSizeBytes >= 0 &&
            document.sizeBytes != fingerprint.sourceSizeBytes
        ) {
            return DocumentIndexState.STALE_SOURCE
        }
        return DocumentIndexState.CURRENT
    }
}

/** Stores only index provenance metadata; document text remains in MayraDocumentContentStore. */
class MayraDocumentIndexMetadataStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun get(uri: String): DocumentIndexFingerprint? {
        val raw = preferences.getString(key(uri), null) ?: return null
        val parts = raw.split(SEPARATOR)
        if (parts.size != 4) return null
        return runCatching {
            DocumentIndexFingerprint(
                parserId = parts[0],
                parserVersion = parts[1].toInt(),
                sourceSizeBytes = parts[2].toLong(),
                recordedAt = parts[3].toLong()
            )
        }.getOrNull()
    }

    fun record(document: MayraDocument, recordedAt: Long = System.currentTimeMillis()): Boolean {
        val parser = MayraDocumentParserVersions.parserFor(document) ?: return false
        val encoded = listOf(
            parser.first,
            parser.second,
            document.sizeBytes,
            recordedAt
        ).joinToString(SEPARATOR)
        preferences.edit().putString(key(document.uri), encoded).apply()
        return true
    }

    fun remove(uri: String) {
        preferences.edit().remove(key(uri)).apply()
    }

    fun removeExcept(uris: Set<String>): Int {
        val retainedKeys = uris.mapTo(mutableSetOf(), ::key)
        val orphaned = preferences.all.keys.filterNot(retainedKeys::contains)
        if (orphaned.isEmpty()) return 0
        preferences.edit().apply { orphaned.forEach(::remove) }.apply()
        return orphaned.size
    }

    fun state(document: MayraDocument, hasIndexedContent: Boolean): DocumentIndexState =
        MayraDocumentIndexFreshness.evaluate(document, hasIndexedContent, get(document.uri))

    private fun key(uri: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray())
        return Base64.encodeToString(digest, Base64.NO_WRAP or Base64.URL_SAFE)
    }

    private companion object {
        const val FILE_NAME = "mayra_document_index_metadata"
        const val SEPARATOR = "\u001F"
    }
}
