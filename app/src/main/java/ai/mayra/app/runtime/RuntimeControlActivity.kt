package ai.mayra.app.runtime

import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue

/** Dedicated internal destination used by runtime-attention notifications. */
class RuntimeControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                val controller: RuntimeControlViewModel = viewModel()
                val state by controller.uiState.collectAsStateWithLifecycle()
                RuntimeControlDialog(
                    state = state,
                    onRefresh = controller::refresh,
                    onDismiss = ::finish,
                    controller = controller
                )
            }
        }
    }
}
