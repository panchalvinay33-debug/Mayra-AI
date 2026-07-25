package ai.mayra.app.file

import android.content.Context
import android.content.Intent
import android.net.Uri

class MayraFileGrantRegistry(context: Context) {
    private val appContext = context.applicationContext
    private val indexStore = MayraEncryptedFileIndexStore(appContext)

    fun register(treeUri: Uri, label: String): MayraFileGrant {
        require(treeUri.scheme == "content") { "Only content tree URIs are supported." }
        val resolver = appContext.contentResolver
        val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION
        val writeFlag = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { resolver.takePersistableUriPermission(treeUri, readFlag or writeFlag) }
            .recoverCatching { resolver.takePersistableUriPermission(treeUri, readFlag) }
            .getOrThrow()
        require(hasPersistedReadAccess(treeUri.toString())) { "The selected folder did not grant persistent read access." }

        val now = System.currentTimeMillis()
        val grant = MayraFileGrant(
            treeUri = treeUri.toString(),
            label = label.trim().take(120).ifBlank { "Selected folder" },
            grantedAt = now
        )
        val current = indexStore.read()
        val next = (current.grants.filterNot { it.treeUri == grant.treeUri } + grant).takeLast(MAX_GRANTS)
        indexStore.write(current.copy(grants = next, generation = current.generation + 1, updatedAt = now))
        return grant
    }

    fun list(): List<MayraFileGrant> = indexStore.read().grants.filter { it.enabled }

    fun remove(treeUri: String) {
        val uri = Uri.parse(treeUri)
        val permission = appContext.contentResolver.persistedUriPermissions.firstOrNull { it.uri == uri }
        if (permission != null) {
            var flags = 0
            if (permission.isReadPermission) flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            if (permission.isWritePermission) flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            if (flags != 0) runCatching { appContext.contentResolver.releasePersistableUriPermission(uri, flags) }
        }
        val current = indexStore.read()
        val removedGrant = current.grants.firstOrNull { it.treeUri == treeUri }
        val cutoff = removedGrant?.grantedAt ?: Long.MAX_VALUE
        indexStore.write(current.copy(
            grants = current.grants.filterNot { it.treeUri == treeUri },
            files = current.files.filterNot {
                it.sourceKind == MayraIndexedSourceKind.SAF_TREE && it.indexedAt >= cutoff &&
                    (it.relativeLocation?.startsWith(removedGrant?.label.orEmpty(), ignoreCase = true) == true)
            },
            generation = current.generation + 1,
            updatedAt = System.currentTimeMillis()
        ))
    }

    fun hasPersistedReadAccess(treeUri: String): Boolean = appContext.contentResolver.persistedUriPermissions
        .any { it.uri.toString() == treeUri && it.isReadPermission }

    private companion object { const val MAX_GRANTS = 64 }
}
