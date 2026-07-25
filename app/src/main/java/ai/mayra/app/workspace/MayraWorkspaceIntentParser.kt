package ai.mayra.app.workspace

class MayraWorkspaceIntentParser {
    fun parse(rawText: String): MayraWorkspaceIntent {
        val normalized = rawText.trim().replace(Regex("\\s+"), " ")
        val lower = normalized.lowercase()
        val action = when {
            containsAny(lower, "bill", "invoice", "receipt", "pdf", "document", "file") &&
                containsAny(lower, "dekho", "dhundo", "search", "find", "analyse", "analyze", "check") -> {
                if (containsAny(lower, "analyse", "analyze", "check", "dekho")) {
                    MayraWorkspaceActionType.ANALYSE_DOCUMENT
                } else {
                    MayraWorkspaceActionType.SEARCH_FILE
                }
            }
            containsAny(lower, "table banao", "table bana", "create table", "sheet banao") ->
                MayraWorkspaceActionType.CREATE_TABLE
            containsAny(lower, "column jodo", "row jodo", "update table", "quantity", "sort", "filter", "total") ->
                MayraWorkspaceActionType.UPDATE_TABLE
            containsAny(lower, "pdf bana", "excel", "xlsx", "csv", "docx", "word", "export", "print-ready", "report bana") ->
                MayraWorkspaceActionType.EXPORT_DOCUMENT
            containsAny(lower, "email", "mail karo", "mail bhejo") ->
                MayraWorkspaceActionType.SEND_EMAIL
            containsAny(lower, "whatsapp") ->
                MayraWorkspaceActionType.PREPARE_WHATSAPP
            containsAny(lower, "call karo", "phone karo", "dial") ->
                MayraWorkspaceActionType.PLACE_CALL
            containsAny(lower, "speaker on", "mute", "unmute", "hold", "call cut", "keypad") ->
                MayraWorkspaceActionType.CONTROL_CALL
            containsAny(lower, "reminder", "yaad dilana", "yaad dila") ->
                MayraWorkspaceActionType.CREATE_REMINDER
            containsAny(lower, "note banao", "note bana", "likh lo") ->
                MayraWorkspaceActionType.CREATE_NOTE
            containsAny(lower, "contact dhundo", "contact search", "number dhundo") ->
                MayraWorkspaceActionType.SEARCH_CONTACT
            containsAny(lower, "notification padho", "notification read") ->
                MayraWorkspaceActionType.READ_NOTIFICATION
            containsAny(lower, "open app", "app kholo") ->
                MayraWorkspaceActionType.OPEN_APP
            else -> MayraWorkspaceActionType.UNKNOWN
        }

        val entities = buildMap {
            extractQuoted(normalized)?.let { put("quoted_text", it) }
            extractEmail(normalized)?.let { put("email", it) }
            extractFormat(lower)?.let { put("format", it) }
            extractLikelyFileQuery(normalized, action)?.let { put("query", it) }
        }

        val requiresConfirmation = action in setOf(
            MayraWorkspaceActionType.SEND_EMAIL,
            MayraWorkspaceActionType.PREPARE_WHATSAPP,
            MayraWorkspaceActionType.PLACE_CALL,
            MayraWorkspaceActionType.CONTROL_CALL
        )

        return MayraWorkspaceIntent(
            rawText = normalized,
            action = action,
            entities = entities,
            requiresConfirmation = requiresConfirmation,
            sensitive = containsAny(lower, "otp", "pin", "password", "bank", "payment", "private")
        )
    }

    private fun containsAny(value: String, vararg needles: String): Boolean = needles.any(value::contains)

    private fun extractQuoted(value: String): String? = Regex("[\"“](.+?)[\"”]")
        .find(value)
        ?.groupValues
        ?.getOrNull(1)
        ?.trim()
        ?.takeIf(String::isNotBlank)

    private fun extractEmail(value: String): String? = Regex("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", RegexOption.IGNORE_CASE)
        .find(value)
        ?.value

    private fun extractFormat(value: String): String? = when {
        "xlsx" in value || "excel" in value -> "XLSX"
        "docx" in value || "word" in value -> "DOCX"
        "csv" in value -> "CSV"
        "pdf" in value -> "PDF"
        "jpg" in value || "jpeg" in value -> "JPG"
        "png" in value -> "PNG"
        "txt" in value || "text file" in value -> "TXT"
        else -> null
    }

    private fun extractLikelyFileQuery(text: String, action: MayraWorkspaceActionType): String? {
        if (action !in setOf(MayraWorkspaceActionType.SEARCH_FILE, MayraWorkspaceActionType.ANALYSE_DOCUMENT)) return null
        return text
            .replace(Regex("(?i)\\b(mayra|mobile mein|phone mein|dekho|dhundo|search karo|find|analyse|analyze|check karo)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.', ',', '?')
            .take(180)
            .takeIf(String::isNotBlank)
    }
}
