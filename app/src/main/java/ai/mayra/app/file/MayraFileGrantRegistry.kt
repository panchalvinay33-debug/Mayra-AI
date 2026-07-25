package ai.mayra.app.file

import android.content.Context
import android.content.Intent
import android.net.Uri

class MayraFileGrantRegistry(context: Context) {
    private val appContext = context.applicationContext
    private val indexStore = MayraEncryptedFileIndexStore(appContext)

    fun register(treeUri: Uri, label: String): MayraFileGrant {
        require(treeUri.scheme == "content") { "Only content tree URIs are supported." }
        appContext.contentResolver.takePersistableUriPermission(
            treeUri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        val now = System.currentTimeMillis()
        val grant = MayraFileGrant(treeUri.toString(), label.trim().take(120).ifBlank { "Selected folder" }, now)
        val current = indexStore.read()
        val next = (current.grants.filterNot { it.treeUri == grant.treeUri } + grant).takeLast(MAX_GRANTS)
        indexStore.write(current.copy(grants = next, generation = current.generation + 1, updatedAt = now))
        return grant
    }

    fun list(): List<MayraFileGrant> = indexStore.read().grants.filter { it.enabled }

    fun remove(treeUri: String) {
        val uri = Uri.parse(treeUri)
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        val current = indexStore.read()
        indexStore.write(current.copy(
            grants = current.grants.filterNot { it.treeUri == treeUri },
            files = current.files.filterNot { it.sourceKind == MayraIndexedSourceKind.SAF_TREE && it.uri.startsWith(treeUri) },
            generation = current.generation + 1,
            updatedAt = System.currentTimeMillis()
        ))
    }

    fun hasPersistedReadAccess(treeUri: String): Boolean = appContext.contentResolver.persistedUriPermissions
        .any { it.uri.toString() == treeUri && it.isReadPermission }

    private companion object { const val MAX_GRANTS = 64 }
}
