package ai.mayra.app.workspace

import ai.mayra.app.document.MayraDocumentAnalysisEngine
import ai.mayra.app.file.MayraFileSearchEngine
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MayraWorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val parser = MayraWorkspaceIntentParser()
    private val store = MayraWorkspaceSessionStore(application)
    private val fileSearch = MayraFileSearchEngine(application)
    private val documentAnalysis = MayraDocumentAnalysisEngine(application)
    private val globalStop = MayraGlobalStopStore(application)
    private val saveMutex = Mutex()
    private val _uiState = MutableStateFlow(MayraWorkspaceUiState())
    val uiState: StateFlow<MayraWorkspaceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = withContext(Dispatchers.IO) { store.load() }
            if (restored != null) {
                _uiState.update { current ->
                    if (current.session.revision == 0L && current.input.isBlank()) {
                        current.copy(session = restored, lastSavedAt = restored.updatedAt)
                    } else current
                }
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
        val task = initialTask(intent, now)
        appendTask(text, task, now)
        if (intent.action in FILE_ACTIONS && !globalStop.isStopped()) runFileTask(task)
        else autosave()
    }

    private fun initialTask(intent: MayraWorkspaceIntent, now: Long): MayraWorkspaceTask = when {
        globalStop.isStopped() -> MayraWorkspaceTask(
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
        intent.action in FILE_ACTIONS -> MayraWorkspaceTask(
            intent = intent,
            state = MayraWorkspaceTaskState.SEARCHING,
            progress = 20,
            statusMessage = "Searching the encrypted metadata index…",
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

    private fun appendTask(text: String, task: MayraWorkspaceTask, now: Long) {
        _uiState.update { current ->
            val existing = current.session
            val table = if (task.intent.action == MayraWorkspaceActionType.CREATE_TABLE) {
                existing.table ?: MayraWorkspaceTable(title = "Voice table", columns = extractColumns(text), revision = 1L)
            } else existing.table
            current.copy(
                session = existing.copy(
                    transcript = (existing.transcript + text).takeLast(MAX_TRANSCRIPT_ENTRIES),
                    tasks = (existing.tasks + task).takeLast(MAX_TASKS),
                    table = table,
                    activeTaskId = task.id,
                    revision = existing.revision + 1L,
                    updatedAt = now
                ),
                input = "",
                error = null
            )
        }
    }

    private fun runFileTask(task: MayraWorkspaceTask) {
        viewModelScope.launch {
            val query = task.intent.entities["query"] ?: task.intent.rawText
            val now = System.currentTimeMillis()
            val updatedTask = when (task.intent.action) {
                MayraWorkspaceActionType.SEARCH_FILE -> runSearchTask(task, query, now)
                MayraWorkspaceActionType.ANALYSE_DOCUMENT -> runAnalysisTask(task, query, now)
                else -> task
            }
            _uiState.update { current -> current.copy(session = current.session.copy(
                tasks = current.session.tasks.map { if (it.id == task.id) updatedTask else it },
                revision = current.session.revision + 1,
                updatedAt = now
            )) }
            autosave()
        }
    }

    private suspend fun runSearchTask(task: MayraWorkspaceTask, query: String, now: Long): MayraWorkspaceTask =
        runCatching { withContext(Dispatchers.IO) { fileSearch.search(query) } }.fold(
            onSuccess = { found -> when {
                !found.found -> task.copy(
                    state = MayraWorkspaceTaskState.WAITING_FOR_PERMISSION,
                    progress = 30,
                    statusMessage = "No authorized indexed file matched. Add a folder or run inventory.",
                    resultSummary = "No source was found in the current authorized index.",
                    updatedAt = now
                )
                else -> task.copy(
                    state = MayraWorkspaceTaskState.COMPLETED,
                    progress = 100,
                    statusMessage = "Found ${found.matches.size} matching file(s).",
                    sources = found.sourceReferences,
                    resultSummary = found.matches.joinToString(limit = 5) { it.displayName },
                    verified = true,
                    updatedAt = now
                )
            } },
            onFailure = { error -> task.copy(
                state = MayraWorkspaceTaskState.FAILED,
                statusMessage = "File search failed safely.",
                resultSummary = error.javaClass.simpleName,
                updatedAt = now
            ) }
        )

    private suspend fun runAnalysisTask(task: MayraWorkspaceTask, query: String, now: Long): MayraWorkspaceTask =
        runCatching { withContext(Dispatchers.IO) { documentAnalysis.analyse(query) } }.fold(
            onSuccess = { analysis -> when {
                analysis.source == null -> task.copy(
                    state = MayraWorkspaceTaskState.WAITING_FOR_PERMISSION,
                    progress = 30,
                    statusMessage = analysis.summary,
                    resultSummary = analysis.summary,
                    updatedAt = now
                )
                analysis.needsPdfOrOcrTool -> task.copy(
                    state = MayraWorkspaceTaskState.WAITING_FOR_TOOL,
                    progress = 55,
                    statusMessage = analysis.summary,
                    sources = listOf(analysis.source),
                    resultSummary = analysis.summary,
                    verified = false,
                    updatedAt = now
                )
                else -> task.copy(
                    state = MayraWorkspaceTaskState.COMPLETED,
                    progress = 100,
                    statusMessage = if (analysis.verified) "Document analysed with source verification." else "Document parsed with low confidence; review required.",
                    sources = listOf(analysis.source),
                    resultSummary = analysis.summary,
                    verified = analysis.verified,
                    updatedAt = now
                )
            } },
            onFailure = { error -> task.copy(
                state = MayraWorkspaceTaskState.FAILED,
                statusMessage = "Document analysis failed safely.",
                resultSummary = error.javaClass.simpleName,
                updatedAt = now
            ) }
        )

    fun pauseActiveTask() = mutateActiveTask(MayraWorkspaceTaskState.PAUSED, "Paused by the owner.")

    fun continueActiveTask() {
        val task = activeTask()?.takeUnless { it.state in TERMINAL_STATES } ?: return
        if (task.intent.action in FILE_ACTIONS) {
            mutateActiveTask(MayraWorkspaceTaskState.SEARCHING, "Searching the encrypted metadata index…")
            runFileTask(task)
        } else {
            val target = if (task.intent.requiresConfirmation) MayraWorkspaceTaskState.WAITING_FOR_CONFIRMATION
            else MayraWorkspaceTaskState.WAITING_FOR_TOOL
            mutateActiveTask(target, waitingMessage(task.intent.action))
        }
    }

    fun cancelActiveTask() = mutateActiveTask(MayraWorkspaceTaskState.CANCELLED, "Cancelled by the owner.")

    fun updateNotes(value: String) {
        val now = System.currentTimeMillis()
        _uiState.update { current -> current.copy(session = current.session.copy(
            notes = value.take(MAX_NOTES_LENGTH),
            revision = current.session.revision + 1L,
            updatedAt = now
        )) }
        autosave()
    }

    fun clearSession() {
        _uiState.value = MayraWorkspaceUiState(session = MayraWorkspaceSession())
        viewModelScope.launch(Dispatchers.IO) { saveMutex.withLock { store.clear() } }
    }

    private fun mutateActiveTask(state: MayraWorkspaceTaskState, message: String) {
        val activeId = _uiState.value.session.activeTaskId ?: return
        val currentTask = activeTask()?.takeUnless { it.state in TERMINAL_STATES } ?: return
        val now = System.currentTimeMillis()
        _uiState.update { current -> current.copy(session = current.session.copy(
            tasks = current.session.tasks.map { task ->
                if (task.id == currentTask.id && task.id == activeId) task.copy(state = state, statusMessage = message, updatedAt = now)
                else task
            },
            revision = current.session.revision + 1L,
            updatedAt = now
        )) }
        autosave()
    }

    private fun activeTask(): MayraWorkspaceTask? = _uiState.value.session.let { session ->
        session.tasks.firstOrNull { it.id == session.activeTaskId }
    }

    private fun autosave() {
        val snapshot = _uiState.value.session
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { saveMutex.withLock { store.save(snapshot) } } }
                .onSuccess { _uiState.update { current ->
                    if (snapshot.revision >= current.session.revision) current.copy(isSaving = false, lastSavedAt = snapshot.updatedAt, error = null)
                    else current.copy(isSaving = true)
                } }
                .onFailure { error -> _uiState.update { it.copy(isSaving = false, error = error.message ?: "Workspace autosave failed.") } }
        }
    }

    private fun extractColumns(text: String): List<String> {
        val normalized = text.lowercase()
        val known = listOf("naam" to "Naam", "name" to "Name", "saman" to "Saman", "item" to "Item",
            "quantity" to "Quantity", "qty" to "Quantity", "rate" to "Rate", "amount" to "Amount",
            "total" to "Total", "date" to "Date")
        return known.filter { (token, _) -> token in normalized }.map { it.second }.distinct()
            .ifEmpty { listOf("Column 1", "Column 2") }
    }

    private fun waitingMessage(action: MayraWorkspaceActionType): String = when (action) {
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
        val FILE_ACTIONS = setOf(MayraWorkspaceActionType.SEARCH_FILE, MayraWorkspaceActionType.ANALYSE_DOCUMENT)
        val TERMINAL_STATES = setOf(MayraWorkspaceTaskState.COMPLETED, MayraWorkspaceTaskState.FAILED, MayraWorkspaceTaskState.CANCELLED)
    }
}
