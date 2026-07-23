package ai.mayra.app.vision

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import java.security.MessageDigest


data class AndroidVisionCapabilities(
    val cameraPermissionGranted: Boolean,
    val cameraAvailable: Boolean,
    val galleryPickerAvailable: Boolean,
    val filePickerAvailable: Boolean,
    val supportedMimeTypes: Set<String>
) {
    val cameraReady: Boolean get() = cameraPermissionGranted && cameraAvailable
}

class AndroidVisionCapabilityReader(private val context: Context) {
    fun snapshot(): AndroidVisionCapabilities {
        val packageManager = context.packageManager
        return AndroidVisionCapabilities(
            cameraPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
            cameraAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            galleryPickerAvailable = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).resolveActivity(packageManager) != null,
            filePickerAvailable = imageFilePickerIntent().resolveActivity(packageManager) != null,
            supportedMimeTypes = MayraVisionRuntime.SUPPORTED_MIME_TYPES
        )
    }

    companion object {
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA

        fun galleryPickerIntent(): Intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }

        fun imageFilePickerIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_MIME_TYPES, MayraVisionRuntime.SUPPORTED_MIME_TYPES.toTypedArray())
        }

        fun cameraCaptureIntent(outputUri: Uri): Intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

data class AndroidImageMetadata(
    val displayName: String?,
    val mimeType: String,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    val fingerprint: String?,
    val persistablePermissionTaken: Boolean
)

class AndroidVisionAssetFactory(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun fromUri(
        uri: Uri,
        source: VisionAssetSource,
        sensitive: Boolean = false,
        takePersistablePermission: Boolean = source in setOf(VisionAssetSource.FILE_PICKER, VisionAssetSource.GALLERY)
    ): Result<VisionAsset> = runCatching {
        require(uri.scheme in setOf(ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE)) { "Unsupported image URI scheme" }
        val permissionTaken = if (takePersistablePermission && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            }.getOrDefault(false)
        } else false
        val metadata = inspect(uri, permissionTaken)
        require(metadata.mimeType.startsWith("image/")) { "Selected file is not an image" }
        require(metadata.mimeType in MayraVisionRuntime.SUPPORTED_MIME_TYPES || metadata.mimeType == "image/*") {
            "Unsupported image format"
        }
        VisionAsset(
            uri = uri.toString(),
            mimeType = metadata.mimeType,
            displayName = metadata.displayName,
            width = metadata.width,
            height = metadata.height,
            sizeBytes = metadata.sizeBytes,
            source = source,
            fingerprint = metadata.fingerprint,
            sensitive = sensitive,
            metadata = buildMap {
                put("persistablePermission", metadata.persistablePermissionTaken.toString())
                uri.authority?.let { put("authority", it.take(120)) }
            }
        )
    }

    fun inspect(uri: Uri, permissionTaken: Boolean = false): AndroidImageMetadata {
        var name: String? = null
        var size: Long? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    name = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME)
                    size = cursor.longOrNull(OpenableColumns.SIZE)
                }
            }
        } else {
            name = uri.lastPathSegment
        }

        val mime = resolver.getType(uri)?.lowercase() ?: inferMimeType(name)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
        val width = bounds.outWidth.takeIf { it > 0 }
        val height = bounds.outHeight.takeIf { it > 0 }
        val fingerprint = runCatching { hashPrefix(uri) }.getOrNull()
        return AndroidImageMetadata(name, mime, size, width, height, fingerprint, permissionTaken)
    }

    private fun hashPrefix(uri: Uri, maximumBytes: Int = 256 * 1024): String {
        val digest = MessageDigest.getInstance("SHA-256")
        resolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (total < maximumBytes) {
                val read = stream.read(buffer, 0, minOf(buffer.size, maximumBytes - total))
                if (read <= 0) break
                digest.update(buffer, 0, read)
                total += read
            }
        } ?: error("Image cannot be opened")
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun inferMimeType(name: String?): String = when (name?.substringAfterLast('.', "")?.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        "heif" -> "image/heif"
        else -> "image/*"
    }

    private fun Cursor.stringOrNull(column: String): String? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.longOrNull(column: String): Long? {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index).takeIf { it >= 0 } else null
    }
}

/** Placeholder provider that reports capability honestly until an OCR/model implementation is installed. */
class UnavailableVisionProvider(
    override val descriptor: VisionProviderDescriptor = VisionProviderDescriptor(
        id = "vision.unavailable",
        displayName = "Vision provider not installed",
        kind = VisionProviderKind.ON_DEVICE,
        supportedTasks = VisionTask.entries.toSet(),
        requiresNetwork = false,
        sendsImageOffDevice = false,
        maxImageBytes = 20L * 1024 * 1024,
        priority = -100
    )
) : MayraVisionProvider {
    override suspend fun analyze(request: VisionRequest): VisionProviderResult = VisionProviderResult.Unsupported(
        "No OCR or image-analysis model is installed yet"
    )
}
