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
