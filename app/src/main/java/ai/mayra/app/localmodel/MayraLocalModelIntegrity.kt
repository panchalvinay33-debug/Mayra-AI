package ai.mayra.app.localmodel

import java.io.File
import java.security.MessageDigest

/**
 * Reusable trust boundary for owner-managed on-device model files.
 *
 * Runtime engines must consume only app-private model bytes that have passed the same filename,
 * size and SHA-256 checks. This class deliberately knows nothing about LiteRT-LM or model output;
 * it only establishes trustworthy local bytes and storage headroom.
 */
object MayraLocalModelIntegrity {
    const val COPY_BUFFER_BYTES: Int = 1024 * 1024
    const val DEFAULT_STORAGE_HEADROOM_BYTES: Long = 256L * 1024L * 1024L

    fun isLiteRtLmName(name: String): Boolean =
        name.trim().endsWith(".litertlm", ignoreCase = true)

    fun hasEnoughStorage(
        availableBytes: Long,
        modelBytes: Long,
        headroomBytes: Long = DEFAULT_STORAGE_HEADROOM_BYTES
    ): Boolean {
        if (availableBytes < 0L || modelBytes <= 0L || headroomBytes < 0L) return false
        if (modelBytes > Long.MAX_VALUE - headroomBytes) return false
        return availableBytes >= modelBytes + headroomBytes
    }

    fun sha256(file: File): String {
        require(file.isFile) { "Model path is not a file" }
        require(file.length() > 0L) { "Model file is empty" }
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun digestHex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
