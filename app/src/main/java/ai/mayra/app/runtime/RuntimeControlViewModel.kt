package ai.mayra.app.runtime

import ai.mayra.app.MayraRuntime
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RuntimeControlViewModel(
    private val snapshotProvider: () -> RuntimeControlSnapshot = {
        check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
        MayraRuntime.controlCenter.snapshot()
    },
    private val approveAction: (String) -> RuntimeControlResult = { actionId ->
        check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
        MayraRuntime.controlCenter.approvePendingAction(actionId)
    },
    private val rejectAction: (String) -> RuntimeControlResult = { actionId ->
        check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
        MayraRuntime.controlCenter.rejectPendingAction(actionId)
    },
    private val cancelPlanAction: (String) -> RuntimeControlResult = { planId ->
        check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
        MayraRuntime.controlCenter.cancelPlan(planId)
    },
    private val runNextAction: suspend (String) -> RuntimeControlResult = { planId ->
        check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
        MayraRuntime.controlCenter.executeNextPlanStep(planId)
    }
) : ViewModel() {
    private val _uiState = MutableStateFlow(RuntimeControlUiState.Loading)
    val uiState: StateFlow<RuntimeControlUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (_uiState.value.isBusy) return
        val notice = _uiState.value.notice
        _uiState.value = runCatching { snapshotProvider().toUiState().copy(notice = notice) }
            .getOrElse { RuntimeControlUiState.failure(it.message ?: "Runtime snapshot failed.") }
    }

    fun approve(actionId: String) = performAction("Approving action…") { approveAction(actionId) }

    fun reject(actionId: String) = performAction("Rejecting action…") { rejectAction(actionId) }

    fun cancelPlan(planId: String) = performAction("Cancelling workflow…") { cancelPlanAction(planId) }

    fun runNext(planId: String) {
        if (!beginAction("Running next workflow step…")) return
        viewModelScope.launch {
            val result = runCatching { runNextAction(planId) }.getOrElse {
                finishFailure(it.message ?: "Workflow execution failed.")
                return@launch
            }
            finishResult(result)
        }
    }

    private fun performAction(label: String, action: () -> RuntimeControlResult) {
        if (!beginAction(label)) return
        val result = runCatching(action).getOrElse {
            finishFailure(it.message ?: "Runtime action failed.")
            return
        }
        finishResult(result)
    }

    private fun beginAction(label: String): Boolean {
        if (_uiState.value.isBusy) return false
        _uiState.value = _uiState.value.copy(
            isBusy = true,
            busyLabel = label,
            notice = null,
            error = null
        )
        return true
    }

    private fun finishResult(result: RuntimeControlResult) {
        val message = when (result) {
            is RuntimeControlResult.Success -> result.message
            is RuntimeControlResult.NotFound -> result.message
            is RuntimeControlResult.InvalidState -> result.message
            is RuntimeControlResult.Failure -> result.message
        }
        val succeeded = result is RuntimeControlResult.Success
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            busyLabel = null,
            notice = message.takeIf { succeeded },
            error = message.takeUnless { succeeded }
        )
        if (succeeded) refresh()
    }

    private fun finishFailure(message: String) {
        _uiState.value = _uiState.value.copy(
            isBusy = false,
            busyLabel = null,
            notice = null,
            error = message
        )
    }
}
