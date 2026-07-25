package ai.mayra.app.workspace

import java.util.UUID

enum class MayraWorkspaceActionType {
    SEARCH_FILE,
    ANALYSE_DOCUMENT,
    CREATE_TABLE,
    UPDATE_TABLE,
    EXPORT_DOCUMENT,
    SEND_EMAIL,
    PREPARE_WHATSAPP,
    PLACE_CALL,
    CONTROL_CALL,
    CREATE_REMINDER,
    CREATE_NOTE,
    OPEN_APP,
    READ_NOTIFICATION,
    SEARCH_CONTACT,
    UNKNOWN
}

enum class MayraWorkspaceTaskState {
    DRAFT,
    UNDERSTANDING,
    SEARCHING,
    PROCESSING,
    WAITING_FOR_PERMISSION,
    WAITING_FOR_CONFIRMATION,
    WAITING_FOR_TOOL,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class MayraSourceReference(
    val uri: String,
    val displayName: String,
    val page: Int? = null,
    val confidence: Double? = null,
    val excerpt: String? = null
)

data class MayraWorkspaceIntent(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val action: MayraWorkspaceActionType,
    val entities: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
    val sensitive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class MayraWorkspaceTask(
    val id: String = UUID.randomUUID().toString(),
    val intent: MayraWorkspaceIntent,
    val state: MayraWorkspaceTaskState = MayraWorkspaceTaskState.DRAFT,
    val progress: Int = 0,
    val statusMessage: String = "Draft",
    val sources: List<MayraSourceReference> = emptyList(),
    val resultSummary: String? = null,
    val verified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(progress in 0..100) { "Workspace progress must be between 0 and 100." }
    }
}

data class MayraWorkspaceTable(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Untitled table",
    val columns: List<String> = emptyList(),
    val rows: List<List<String>> = emptyList(),
    val revision: Long = 0L
)

data class MayraWorkspaceSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Mayra Workspace",
    val transcript: List<String> = emptyList(),
    val tasks: List<MayraWorkspaceTask> = emptyList(),
    val notes: String = "",
    val table: MayraWorkspaceTable? = null,
    val activeTaskId: String? = null,
    val revision: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MayraWorkspaceUiState(
    val session: MayraWorkspaceSession = MayraWorkspaceSession(),
    val input: String = "",
    val isSaving: Boolean = false,
    val lastSavedAt: Long = 0L,
    val error: String? = null
)
