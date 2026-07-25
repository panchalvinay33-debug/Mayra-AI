package ai.mayra.app.file

import android.net.Uri
import java.util.Locale

enum class MayraIndexedSourceKind { MEDIASTORE_IMAGE, MEDIASTORE_DOWNLOAD, SAF_TREE, SAF_DOCUMENT }
enum class MayraIndexState { METADATA_ONLY, TEXT_PENDING, INDEXED, FAILED, EXCLUDED }

data class MayraIndexedFile(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val sourceKind: MayraIndexedSourceKind,
    val relativeLocation: String? = null,
    val grantRootUri: String? = null,
    val fingerprint: String,
    val state: MayraIndexState = MayraIndexState.METADATA_ONLY,
    val extractedText: String? = null,
    val indexedAt: Long = System.currentTimeMillis(),
    val failure: String? = null
) {
    init {
        require(Uri.parse(uri).scheme in setOf("content", "file")) { "Only local Android file URIs may be indexed." }
        require(displayName.isNotBlank()) { "Indexed file requires a display name." }
        require(sizeBytes >= 0L) { "Indexed file size cannot be negative." }
        require(grantRootUri == null || Uri.parse(grantRootUri).scheme == "content") {
            "SAF grant roots must be content URIs."
        }
    }

    fun searchableText(): String = listOfNotNull(displayName, relativeLocation, extractedText)
        .joinToString(" ")
        .lowercase(Locale.ROOT)
}

data class MayraFileGrant(
    val treeUri: String,
    val label: String,
    val grantedAt: Long,
    val lastScanAt: Long = 0L,
    val enabled: Boolean = true
)

data class MayraFileIndexSnapshot(
    val files: List<MayraIndexedFile> = emptyList(),
    val grants: List<MayraFileGrant> = emptyList(),
    val generation: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun search(query: String, limit: Int = 20): List<MayraIndexedFile> {
        val tokens = query.lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 }
            .distinct()
        if (tokens.isEmpty()) return emptyList()
        return files.asSequence()
            .filter { it.state != MayraIndexState.EXCLUDED }
            .map { file -> file to tokens.count { token -> token in file.searchableText() } }
            .filter { (_, score) -> score > 0 }
            .sortedWith(compareByDescending<Pair<MayraIndexedFile, Int>> { it.second }
                .thenByDescending { it.first.modifiedAt })
            .take(limit.coerceIn(1, 100))
            .map { it.first }
            .toList()
    }
}

object MayraFileIndexReconciler {
    fun reconcile(
        existing: List<MayraIndexedFile>,
        scanned: List<MayraIndexedFile>,
        authoritativeKinds: Set<MayraIndexedSourceKind>
    ): List<MayraIndexedFile> {
        val existingByUri = existing.associateBy { it.uri }
        val retained = existing.filter { it.sourceKind !in authoritativeKinds }.associateBy { it.uri }.toMutableMap()
        scanned.distinctBy { it.uri }.forEach { candidate ->
            val previous = existingByUri[candidate.uri]
            retained[candidate.uri] = if (previous != null && previous.fingerprint == candidate.fingerprint) {
                candidate.copy(
                    state = previous.state,
                    extractedText = previous.extractedText,
                    indexedAt = previous.indexedAt,
                    failure = previous.failure
                )
            } else candidate
        }
        return retained.values.toList()
    }
}

object MayraFilePrivacyPolicy {
    private val blockedFragments = listOf("/android/data/", "/android/obb/", "otp", "password", "pin", "banking")

    fun isAllowed(displayName: String, relativeLocation: String?, mimeType: String?): Boolean {
        val combined = "${relativeLocation.orEmpty()}/${displayName}".lowercase(Locale.ROOT)
        if (blockedFragments.any(combined::contains)) return false
        val type = mimeType.orEmpty().lowercase(Locale.ROOT)
        return type.startsWith("image/") || type == "application/pdf" || type.startsWith("text/") ||
            type.contains("spreadsheet") || type.contains("wordprocessing") || type.contains("csv") || type.isBlank()
    }
}
