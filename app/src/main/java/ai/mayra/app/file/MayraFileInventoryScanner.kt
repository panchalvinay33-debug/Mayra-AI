package ai.mayra.app.file

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import java.security.MessageDigest

class MayraFileInventoryScanner(context: Context) {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    fun scanMediaStore(): List<MayraIndexedFile> = buildList {
        addAll(scanCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MayraIndexedSourceKind.MEDIASTORE_IMAGE))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addAll(scanCollection(MediaStore.Downloads.EXTERNAL_CONTENT_URI, MayraIndexedSourceKind.MEDIASTORE_DOWNLOAD))
        }
    }.distinctBy { it.uri }

    fun scanTree(treeUri: Uri, maxFiles: Int = MAX_TREE_FILES): List<MayraIndexedFile> {
        val root = DocumentFile.fromTreeUri(appContext, treeUri) ?: return emptyList()
        val output = ArrayList<MayraIndexedFile>()
        val queue = ArrayDeque<Pair<DocumentFile, String>>()
        queue.add(root to root.name.orEmpty())
        val boundedLimit = maxFiles.coerceIn(1, MAX_TREE_FILES)
        while (queue.isNotEmpty() && output.size < boundedLimit) {
            val (node, path) = queue.removeFirst()
            val children = runCatching { node.listFiles().toList() }.getOrDefault(emptyList())
            children.forEach { child ->
                if (output.size >= boundedLimit) return@forEach
                val nextPath = listOf(path, child.name).filter(String::isNotBlank).joinToString("/")
                if (child.isDirectory) queue.add(child to nextPath)
                else toIndexedFile(child.uri, MayraIndexedSourceKind.SAF_TREE, nextPath)?.let(output::add)
            }
        }
        return output
    }

    fun inspectDocument(uri: Uri): MayraIndexedFile? =
        toIndexedFile(uri, MayraIndexedSourceKind.SAF_DOCUMENT, null)

    private fun scanCollection(collection: Uri, kind: MayraIndexedSourceKind): List<MayraIndexedFile> {
        val baseProjection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            baseProjection += MediaStore.MediaColumns.RELATIVE_PATH
        }
        return runCatching {
            resolver.query(
                collection,
                baseProjection.toTypedArray(),
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
                val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                } else -1
                buildList {
                    while (cursor.moveToNext() && size < MAX_MEDIASTORE_FILES) {
                        val uri = Uri.withAppendedPath(collection, cursor.getLong(idColumn).toString())
                        val name = cursor.getString(nameColumn).orEmpty().ifBlank { "Unnamed file" }
                        val mime = cursor.getString(typeColumn)
                        val bytes = cursor.getLong(sizeColumn).coerceAtLeast(0L)
                        val modified = cursor.getLong(modifiedColumn).coerceAtLeast(0L) * 1000L
                        val relative = pathColumn.takeIf { it >= 0 }?.let(cursor::getString)
                        if (MayraFilePrivacyPolicy.isAllowed(name, relative, mime)) {
                            add(buildFile(uri, name, mime, bytes, modified, kind, relative))
                        }
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun toIndexedFile(uri: Uri, kind: MayraIndexedSourceKind, relative: String?): MayraIndexedFile? = runCatching {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
            val name = nameColumn.takeIf { it >= 0 }?.let(cursor::getString).orEmpty().ifBlank { "Unnamed file" }
            val size = sizeColumn.takeIf { it >= 0 }?.let(cursor::getLong)?.coerceAtLeast(0L) ?: 0L
            val mime = resolver.getType(uri)
            if (!MayraFilePrivacyPolicy.isAllowed(name, relative, mime)) return@use null
            buildFile(uri, name, mime, size, 0L, kind, relative)
        }
    }.getOrNull()

    private fun buildFile(
        uri: Uri,
        name: String,
        mime: String?,
        bytes: Long,
        modified: Long,
        kind: MayraIndexedSourceKind,
        relative: String?
    ): MayraIndexedFile = MayraIndexedFile(
        uri = uri.toString(),
        displayName = name.take(MAX_NAME),
        mimeType = mime?.take(MAX_MIME),
        sizeBytes = bytes,
        modifiedAt = modified,
        sourceKind = kind,
        relativeLocation = relative?.take(MAX_LOCATION),
        fingerprint = sha256("${uri}|$bytes|$modified|${mime.orEmpty()}")
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MAX_MEDIASTORE_FILES = 10_000
        const val MAX_TREE_FILES = 10_000
        const val MAX_NAME = 300
        const val MAX_MIME = 160
        const val MAX_LOCATION = 700
    }
}
