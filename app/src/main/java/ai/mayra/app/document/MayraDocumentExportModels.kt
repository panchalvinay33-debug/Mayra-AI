package ai.mayra.app.document

/**
 * Shared export contracts for Workspace output.
 * Actual renderers (PDF/XLSX/DOCX) are added as providers.
 */
enum class MayraExportFormat {
    PDF,
    XLSX,
    DOCX,
    CSV,
    TXT,
    IMAGE
}

data class MayraExportRequest(
    val workspaceId: String,
    val format: MayraExportFormat,
    val title: String,
    val includeSourceReferences: Boolean = true,
    val requireOwnerConfirmation: Boolean = true
)

data class MayraExportResult(
    val success: Boolean,
    val fileUri: String? = null,
    val message: String,
    val warnings: List<String> = emptyList()
)
