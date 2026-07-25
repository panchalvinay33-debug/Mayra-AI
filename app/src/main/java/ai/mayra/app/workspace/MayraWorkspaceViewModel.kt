package ai.mayra.app.workspace

import ai.mayra.app.safety.MayraGlobalStopStore
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MayraWorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = MayraWorkspaceIntentParser()
    private val store = MayraWorkspaceSessionStore(application)
    private val globalStop = MayraGlobalStopStore(application)
    private val _uiState = MutableStateFlow(MayraWorkspaceUiState())
    val uiState: StateFlow<MayraWorkspaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) { store.load() }
            if (restored != null) {
                _uiState.update { it.copy(session = restored, lastSavedAt = restored.updatedAt) }
            }
        }
    }

    fun updateInput(value: String) {
        _uiState.update { it.copy(input = value.take(MAX_INPUT_LENGTH), error = null) }
    }

    fun submitInput() {
        val text = _uiState.value.input.trim()
        if (text.isBlank()) return
        val intent = parser.parse(text)
        val now = System.currentTimeMillis()
        val stopped = globalStop.isStopped()
        val task = when {
            stopped -> MayraWorkspaceTask(
                intent = intent,
                state = MayraWorkspaceTaskState.FAILED,
                statusMessage = "Global Stop is active. Resume Mayra actions before continuing.",
                updatedAt = now
            )
            intent.action == MayraWorkspaceActionType.UNKNOWN -> MayraWorkspaceTask(
                intent = intent,
                state = MayraWorkspaceTaskState.FAILED,
                statusMessage = "I could not map this to a Workspace action yet.",
                updatedAt = now
            )
            intent.action == MayraWorkspaceActionType.CREATE_TABLE -> MayraWorkspaceTask(
                intent = intent,
                state = MayraWorkspaceTaskState.COMPLETED,
                progress = 100,
                statusMessage = "Table workspace created.",
                resultSummary = "A local table draft is ready for rows and corrections.",
                verified = true,
                updatedAt = now
            )
            intent.requiresConfirmation -> MayraWorkspaceTask(
                intent = intent,
                state = MayraWorkspaceTaskState.WAITING_FOR_CONFIRMATION,
                progress = 20,
                statusMessage = "Review is required before this external action.",
                updatedAt = now
            )
            else -> MayraWorkspaceTask(
                intent = intent,
                state = MayraWorkspaceTaskState.WAITING_FOR_TOOL,
                progress = 10,
                statusMessage = waitingMessage(intent.action),
                updatedAt = now
            )
        }

        _uiState.update { current ->
            val existing = current.session
            val nextTable = if (intent.action == MayraWorkspaceActionType.CREATE_TABLE) {
                existing.table ?: MayraWorkspaceTable(
                    title = "Voice table",
                    columns = extractColumns(text),
                    revision = 1L
                )
            } else existing.table
            val nextSession = existing.copy(
                transcript = (existing.transcript + text).takeLast(MAX_TRANSCRIPT_ENTRIES),
                tasks = (existing.tasks + task).takeLast(MAX_TASKS),
                table = nextTable,
                activeTaskId = task.id,
                revision = existing.revision + 1L,
                updatedAt = now
            )
            current.copy(session = nextSession, input = "", error = null)
        }
        autosave()
    }

    fun pauseActiveTask() = mutateActiveTask(
        MayraWorkspaceTaskState.PAUSED,
        "Paused by the owner."
    )

    fun continueActiveTask() {
        val task = activeTask() ?: return
        val target = if (task.intent.requiresConfirmation) {
            MayraWorkspaceTaskState.WAITING_FOR_CONFIRMATION
        } else {
            MayraWorkspaceTaskState.WAITING_FOR_TOOL
        }
        mutateActiveTask(target, waitingMessage(task.intent.action))
    }

    fun cancelActiveTask() = mutateActiveTask(
        MayraWorkspaceTaskState.CANCELLED,
        "Cancelled by the owner."
    )

    fun updateNotes(value: String) {
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            current.copy(
                session = current.session.copy(
                    notes = value.take(MAX_NOTES_LENGTH),
                    revision = current.session.revision + 1L,
                    updatedAt = now
                )
            )
        }
        autosave()
    }

    fun clearSession() {
        val fresh = MayraWorkspaceSession()
        _uiState.value = MayraWorkspaceUiState(session = fresh)
        viewModelScope.launch(Dispatchers.IO) { store.clear() }
    }

    private fun mutateActiveTask(state: MayraWorkspaceTaskState, message: String) {
        val activeId = _uiState.value.session.activeTaskId ?: return
        val now = System.currentTimeMillis()
        _uiState.update { current ->
            val tasks = current.session.tasks.map { task ->
                if (task.id == activeId && task.state !in TERMINAL_STATES) {
                    task.copy(state = state, statusMessage = message, updatedAt = now)
                } else task
            }
            current.copy(
                session = current.session.copy(
                    tasks = tasks,
                    revision = current.session.revision + 1L,
                    updatedAt = now
                )
            )
        }
        autosave()
    }

    private fun activeTask(): MayraWorkspaceTask? {
        val session = _uiState.value.session
        return session.tasks.firstOrNull { it.id == session.activeTaskId }
    }

    private fun autosave() {
        val snapshot = _uiState.value.session
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { store.save(snapshot) } }
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, lastSavedAt = snapshot.updatedAt, error = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = error.message ?: "Workspace autosave failed."
                        )
                    }
                }
        }
    }

    private fun extractColumns(text: String): List<String> {
        val normalized = text.lowercase()
        val known = listOf(
            "naam" to "Naam",
            "name" to "Name",
            "saman" to "Saman",
            "item" to "Item",
            "quantity" to "Quantity",
            "qty" to "Quantity",
            "rate" to "Rate",
            "amount" to "Amount",
            "total" to "Total",
            "date" to "Date"
        )
        return known.filter { (token, _) -> token in normalized }
            .map { it.second }
            .distinct()
            .ifEmpty { listOf("Column 1", "Column 2") }
    }

    private fun waitingMessage(action: MayraWorkspaceActionType): String = when (action) {
        MayraWorkspaceActionType.SEARCH_FILE,
        MayraWorkspaceActionType.ANALYSE_DOCUMENT -> "Waiting for the File Intelligence index and document tools."
        MayraWorkspaceActionType.UPDATE_TABLE -> "Waiting for the table mutation engine."
        MayraWorkspaceActionType.EXPORT_DOCUMENT -> "Waiting for the export renderer."
        MayraWorkspaceActionType.CREATE_REMINDER -> "Ready to hand this request to the existing reminder engine."
        MayraWorkspaceActionType.CREATE_NOTE -> "Ready to hand this request to Mayra memory."
        MayraWorkspaceActionType.SEARCH_CONTACT -> "Ready to use the existing contact identity resolver."
        MayraWorkspaceActionType.READ_NOTIFICATION -> "Ready to use notification intelligence."
        MayraWorkspaceActionType.OPEN_APP -> "Ready to use the local Android action layer."
        else -> "Waiting for the required tool adapter."
    }

    private companion object {
        const val MAX_INPUT_LENGTH = 2_000
        const val MAX_NOTES_LENGTH = 50_000
        const val MAX_TRANSCRIPT_ENTRIES = 300
        const val MAX_TASKS = 200
        val TERMINAL_STATES = setOf(
            MayraWorkspaceTaskState.COMPLETED,
            MayraWorkspaceTaskState.FAILED,
            MayraWorkspaceTaskState.CANCELLED
        )
    }
}
