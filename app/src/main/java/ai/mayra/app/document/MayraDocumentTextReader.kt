package ai.mayra.app.document

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Reads only explicitly supported text-like documents with strict byte and character limits.
 * PDF, DOC and unknown binary formats remain metadata-only until a tested parser is installed.
 */
class MayraDocumentTextReader(private val context: Context) {
    fun preview(uri: Uri, mimeType: String, maxBytes: Int = 256_000, maxCharacters: Int = 20_000): DocumentTextPreview {
        require(maxBytes in 1..1_000_000)
        require(maxCharacters in 1..100_000)
        if (!isSupportedTextType(mimeType, uri.lastPathSegment.orEmpty())) {
            return DocumentTextPreview.Unsupported("Text preview is not supported for this file type yet.")
        }

        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val limited = stream.readNBytes(maxBytes + 1)
                val truncatedByBytes = limited.size > maxBytes
                val readable = if (truncatedByBytes) limited.copyOf(maxBytes) else limited
                val text = BufferedReader(InputStreamReader(readable.inputStream(), Charsets.UTF_8))
                    .use { it.readText() }
                    .replace("\u0000", "")
                    .take(maxCharacters)
                val truncated = truncatedByBytes || text.length >= maxCharacters
                if (text.isBlank()) DocumentTextPreview.Empty
                else DocumentTextPreview.Ready(text = text, truncated = truncated)
            } ?: DocumentTextPreview.Error("Mayra could not open this document.")
        }.getOrElse { DocumentTextPreview.Error("Mayra could not read this document safely.") }
    }

    companion object {
        fun isSupportedTextType(mimeType: String, name: String): Boolean {
            val type = mimeType.lowercase()
            val extension = name.substringAfterLast('.', "").lowercase()
            return type.startsWith("text/") || type in setOf(
                "application/json", "application/xml", "application/x-yaml", "application/yaml"
            ) || extension in setOf("txt", "md", "markdown", "csv", "json", "xml", "yaml", "yml", "log")
        }
    }
}

sealed interface DocumentTextPreview {
    data class Ready(val text: String, val truncated: Boolean) : DocumentTextPreview
    data class Unsupported(val reason: String) : DocumentTextPreview
    data class Error(val reason: String) : DocumentTextPreview
    data object Empty : DocumentTextPreview
}
