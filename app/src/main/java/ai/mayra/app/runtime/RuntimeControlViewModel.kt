package ai.mayra.app.runtime

import ai.mayra.app.MayraRuntime
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RuntimeControlViewModel(
    private val snapshotProvider: () -> RuntimeControlSnapshot = {
        check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
        MayraRuntime.controlCenter.snapshot()
    }
) : ViewModel() {
    private val _uiState = MutableStateFlow(RuntimeControlUiState.Loading)
    val uiState: StateFlow<RuntimeControlUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = runCatching { snapshotProvider().toUiState() }
            .getOrElse { RuntimeControlUiState.failure(it.message ?: "Runtime snapshot failed.") }
    }
}
