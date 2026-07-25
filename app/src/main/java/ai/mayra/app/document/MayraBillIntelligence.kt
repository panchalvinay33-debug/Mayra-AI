package ai.mayra.app.document

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class MayraBillItem(
    val name: String,
    val quantity: BigDecimal? = null,
    val rate: BigDecimal? = null,
    val amount: BigDecimal? = null,
    val sourceLine: Int
)

data class MayraBillRecord(
    val vendor: String? = null,
    val billDate: String? = null,
    val invoiceNumber: String? = null,
    val items: List<MayraBillItem> = emptyList(),
    val tax: BigDecimal? = null,
    val total: BigDecimal? = null,
    val paymentStatus: String? = null,
    val confidence: Double,
    val warnings: List<String> = emptyList()
)

data class MayraExtractedDocumentText(
    val text: String,
    val mimeType: String?,
    val truncated: Boolean,
    val bytesRead: Int
)

class MayraLocalTextExtractor(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun extract(uri: Uri, mimeType: String?): MayraExtractedDocumentText? {
        val type = mimeType.orEmpty().lowercase(Locale.ROOT)
        if (!(type.startsWith("text/") || type.contains("csv") || type == "application/json" || type.isBlank())) {
            return null
        }
        return resolver.openInputStream(uri)?.use { input -> readBounded(input, mimeType) }
    }

    internal fun readBounded(input: InputStream, mimeType: String?): MayraExtractedDocumentText {
        val buffer = ByteArray(CHUNK_SIZE)
        val output = java.io.ByteArrayOutputStream()
        var truncated = false
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            val allowed = (MAX_TEXT_BYTES - output.size()).coerceAtLeast(0)
            if (allowed == 0) {
                truncated = true
                break
            }
            output.write(buffer, 0, minOf(read, allowed))
            if (read > allowed) {
                truncated = true
                break
            }
        }
        buffer.fill(0)
        val bytes = output.toByteArray()
        return try {
            MayraExtractedDocumentText(
                text = String(bytes, Charsets.UTF_8).replace('\u0000', ' ').take(MAX_TEXT_CHARACTERS),
                mimeType = mimeType,
                truncated = truncated,
                bytesRead = bytes.size
            )
        } finally {
            bytes.fill(0)
        }
    }

    private companion object {
        const val CHUNK_SIZE = 8 * 1024
        const val MAX_TEXT_BYTES = 1_000_000
        const val MAX_TEXT_CHARACTERS = 500_000
    }
}

class MayraBillParser {
    fun parse(text: String): MayraBillRecord {
        val lines = text.lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter(String::isNotBlank)
        val invoice = firstGroup(lines, INVOICE_PATTERNS)
        val date = firstGroup(lines, DATE_PATTERNS)?.let(::normalizeDate) ?: firstDate(lines)
        val total = findMoney(lines, TOTAL_LABELS)
        val tax = findMoney(lines, TAX_LABELS)
        val payment = lines.firstOrNull { line -> PAYMENT_WORDS.any { it in line.lowercase(Locale.ROOT) } }
            ?.take(120)
        val vendor = lines.firstOrNull { line ->
            line.length in 3..100 && !line.contains(Regex("\\d{4,}")) &&
                TOTAL_LABELS.none { it in line.lowercase(Locale.ROOT) }
        }
        val items = lines.mapIndexedNotNull { index, line -> parseItem(line, index + 1) }.take(MAX_ITEMS)
        val signals = listOfNotNull(invoice, date, total, vendor).size + if (items.isNotEmpty()) 1 else 0
        val warnings = buildList {
            if (total == null) add("Total amount not confidently found.")
            if (date == null) add("Bill date not confidently found.")
            if (items.isEmpty()) add("No structured line items were confidently parsed.")
        }
        return MayraBillRecord(
            vendor = vendor,
            billDate = date,
            invoiceNumber = invoice,
            items = items,
            tax = tax,
            total = total,
            paymentStatus = payment,
            confidence = (signals / 5.0).coerceIn(0.0, 1.0),
            warnings = warnings
        )
    }

    private fun parseItem(line: String, sourceLine: Int): MayraBillItem? {
        val match = ITEM_PATTERN.matchEntire(line) ?: return null
        val name = match.groupValues[1].trim().trim(',', '-', ':')
        if (name.length < 2 || TOTAL_LABELS.any { it in name.lowercase(Locale.ROOT) }) return null
        val numbers = match.groupValues.drop(2).mapNotNull(::money)
        if (numbers.isEmpty()) return null
        return MayraBillItem(
            name = name.take(160),
            quantity = numbers.getOrNull(0),
            rate = numbers.getOrNull(1) ?: numbers.firstOrNull(),
            amount = numbers.getOrNull(2) ?: numbers.lastOrNull(),
            sourceLine = sourceLine
        )
    }

    private fun findMoney(lines: List<String>, labels: List<String>): BigDecimal? {
        lines.asReversed().forEach { line ->
            if (labels.any { it in line.lowercase(Locale.ROOT) }) {
                MONEY_PATTERN.findAll(line).mapNotNull { money(it.value) }.lastOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun firstGroup(lines: List<String>, patterns: List<Regex>): String? =
        lines.firstNotNullOfOrNull { line -> patterns.firstNotNullOfOrNull { it.find(line)?.groupValues?.getOrNull(1) } }
            ?.trim()?.takeIf(String::isNotBlank)?.take(100)

    private fun firstDate(lines: List<String>): String? = lines.firstNotNullOfOrNull { line ->
        GENERIC_DATE.find(line)?.value?.let(::normalizeDate)
    }

    private fun normalizeDate(value: String): String = value.trim().let { raw ->
        DATE_FORMATS.firstNotNullOfOrNull { formatter ->
            try { LocalDate.parse(raw, formatter).toString() } catch (_: DateTimeParseException) { null }
        } ?: raw.take(40)
    }

    private fun money(value: String): BigDecimal? = value
        .replace("₹", "").replace("Rs.", "", ignoreCase = true).replace("Rs", "", ignoreCase = true)
        .replace(",", "").trim().toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO }

    private companion object {
        val INVOICE_PATTERNS = listOf(
            Regex("(?i)(?:invoice|bill)\\s*(?:no|number|#)?\\s*[:.-]?\\s*([A-Z0-9/-]{3,})"),
            Regex("(?i)inv\\s*(?:no|#)?\\s*[:.-]?\\s*([A-Z0-9/-]{3,})")
        )
        val DATE_PATTERNS = listOf(Regex("(?i)(?:bill date|invoice date|date)\\s*[:.-]?\\s*([0-9]{1,4}[-/.][0-9]{1,2}[-/.][0-9]{1,4})"))
        val GENERIC_DATE = Regex("\\b[0-9]{1,4}[-/.][0-9]{1,2}[-/.][0-9]{1,4}\\b")
        val DATE_FORMATS = listOf("d/M/uuuu", "dd/MM/uuuu", "d-M-uuuu", "dd-MM-uuuu", "uuuu-MM-dd", "d.M.uuuu")
            .map(DateTimeFormatter::ofPattern)
        val TOTAL_LABELS = listOf("grand total", "net total", "total amount", "amount payable", "total")
        val TAX_LABELS = listOf("gst", "tax", "cgst", "sgst", "igst")
        val PAYMENT_WORDS = listOf("paid", "unpaid", "pending", "payment due", "cash", "upi")
        val MONEY_PATTERN = Regex("(?:₹|Rs\\.?\\s*)?[0-9][0-9,]*(?:\\.[0-9]{1,2})?")
        val ITEM_PATTERN = Regex("^(.{2,160}?)\\s+([0-9]+(?:\\.[0-9]+)?)\\s+([0-9,]+(?:\\.[0-9]{1,2})?)(?:\\s+([0-9,]+(?:\\.[0-9]{1,2})?))?$")
        const val MAX_ITEMS = 300
    }
}
