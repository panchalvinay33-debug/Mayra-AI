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
        val notice = _uiState.value.notice
        _uiState.value = runCatching { snapshotProvider().toUiState().copy(notice = notice) }
            .getOrElse { RuntimeControlUiState.failure(it.message ?: "Runtime snapshot failed.") }
    }

    fun approve(actionId: String) = performAction { approveAction(actionId) }

    fun reject(actionId: String) = performAction { rejectAction(actionId) }

    fun cancelPlan(planId: String) = performAction { cancelPlanAction(planId) }

    fun runNext(planId: String) {
        viewModelScope.launch {
            val result = runCatching { runNextAction(planId) }.getOrElse {
                updateFailure(it.message ?: "Workflow execution failed.")
                return@launch
            }
            applyResult(result)
        }
    }

    private fun performAction(action: () -> RuntimeControlResult) {
        val result = runCatching(action).getOrElse {
            updateFailure(it.message ?: "Runtime action failed.")
            return
        }
        applyResult(result)
    }

    private fun applyResult(result: RuntimeControlResult) {
        val message = when (result) {
            is RuntimeControlResult.Success -> result.message
            is RuntimeControlResult.NotFound -> result.message
            is RuntimeControlResult.InvalidState -> result.message
            is RuntimeControlResult.Failure -> result.message
        }
        val succeeded = result is RuntimeControlResult.Success
        _uiState.value = _uiState.value.copy(
            notice = message.takeIf { succeeded },
            error = message.takeUnless { succeeded }
        )
        if (succeeded) refresh()
    }

    private fun updateFailure(message: String) {
        _uiState.value = _uiState.value.copy(notice = null, error = message)
    }
}
