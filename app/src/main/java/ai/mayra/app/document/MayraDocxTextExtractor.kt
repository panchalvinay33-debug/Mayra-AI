package ai.mayra.app.document

import android.content.Context
import android.net.Uri
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Extracts text from the XML parts of a DOCX package. Embedded media, macros and external
 * relationships are never opened or executed.
 */
class MayraDocxTextExtractor(private val context: Context) {
    fun extract(document: MayraDocument): DocumentExtractionResult {
        if (document.sizeBytes > MAX_DOCX_BYTES) {
            return DocumentExtractionResult.Failure(
                "This DOCX is larger than the safe ${MAX_DOCX_BYTES / 1_048_576} MB indexing limit."
            )
        }

        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(document.uri))?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    val output = StringBuilder()
                    var entries = 0
                    var relevantBytes = 0L
                    var foundMainDocument = false
                    var truncated = false

                    while (true) {
                        val entry = zip.nextEntry ?: break
                        entries++
                        if (entries > MAX_ZIP_ENTRIES) {
                            return@use DocumentExtractionResult.Failure(
                                "This DOCX contains too many package entries to index safely."
                            )
                        }

                        val normalizedName = entry.name.replace('\\', '/').lowercase(Locale.ROOT)
                        if (!entry.isDirectory && isReadableWordXml(normalizedName)) {
                            val bytes = readEntry(zip)
                            if (bytes == null) {
                                truncated = true
                                zip.closeEntry()
                                break
                            }
                            relevantBytes += bytes.size
                            if (relevantBytes > MAX_RELEVANT_XML_BYTES) {
                                return@use DocumentExtractionResult.Failure(
                                    "This DOCX expands beyond the safe XML indexing limit."
                                )
                            }
                            if (normalizedName == MAIN_DOCUMENT_PART) foundMainDocument = true
                            appendWordXml(bytes, output)
                            if (output.length > MAX_INDEXED_CHARS) {
                                truncated = true
                                zip.closeEntry()
                                break
                            }
                        }
                        zip.closeEntry()
                    }

                    if (!foundMainDocument) {
                        DocumentExtractionResult.Failure(
                            "This file is not a readable DOCX package or its main document XML is missing."
                        )
                    } else {
                        val normalized = normalizeDocumentText(output.toString())
                        DocumentExtractionResult.Success(
                            text = normalized.take(MAX_INDEXED_CHARS),
                            truncated = truncated || normalized.length > MAX_INDEXED_CHARS
                        )
                    }
                }
            } ?: DocumentExtractionResult.Failure("The DOCX stream could not be opened.")
        }.getOrElse { error ->
            val reason = when (error) {
                is ZipException -> "This DOCX package is damaged or is not a valid Office Open XML file."
                else -> error.message.orEmpty().ifBlank {
                    "DOCX text extraction failed. The file may be damaged or unsupported."
                }
            }
            DocumentExtractionResult.Failure(reason)
        }
    }

    private fun readEntry(zip: ZipInputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = zip.read(buffer)
            if (read <= 0) break
            total += read
            if (total > MAX_SINGLE_XML_BYTES) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun appendWordXml(bytes: ByteArray, output: StringBuilder) {
        val parser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser().apply {
            setInput(ByteArrayInputStream(bytes), StandardCharsets.UTF_8.name())
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && output.length <= MAX_INDEXED_CHARS) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "tab" -> output.append('\t')
                    "br", "cr" -> output.append('\n')
                }
                XmlPullParser.TEXT -> if (parser.depth > 0 && parser.name == null) {
                    val parentName = runCatching { parser.getNameAtDepth(parser.depth) }.getOrNull()
                    if (parentName == "t" || parentName == "instrText" || parentName == "delText") {
                        output.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "p", "tr" -> output.append('\n')
                    "tc" -> output.append('\t')
                }
            }
            event = parser.next()
        }
    }

    private fun XmlPullParser.getNameAtDepth(targetDepth: Int): String? =
        if (depth == targetDepth) name else null

    private fun isReadableWordXml(name: String): Boolean =
        name == MAIN_DOCUMENT_PART ||
            name.matches(Regex("word/header\\d*\\.xml")) ||
            name.matches(Regex("word/footer\\d*\\.xml")) ||
            name == "word/footnotes.xml" ||
            name == "word/endnotes.xml"

    companion object {
        const val MAX_DOCX_BYTES = 25L * 1_048_576L
        const val MAX_ZIP_ENTRIES = 2_000
        const val MAX_SINGLE_XML_BYTES = 8 * 1_048_576
        const val MAX_RELEVANT_XML_BYTES = 20L * 1_048_576L
        const val MAX_INDEXED_CHARS = 500_000
        private const val BUFFER_SIZE = 8_192
        private const val MAIN_DOCUMENT_PART = "word/document.xml"
    }
}
