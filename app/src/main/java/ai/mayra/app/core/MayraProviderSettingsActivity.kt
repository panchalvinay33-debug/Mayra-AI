package ai.mayra.app.core

import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MayraProviderSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AndroidMayraProviderSettingsStore(applicationContext)
        setContent { MayraAITheme { ProviderSettings(store) } }
    }

    @Composable
    private fun ProviderSettings(store: AndroidMayraProviderSettingsStore) {
        val initial = remember { store.read() }
        var enabled by remember { mutableStateOf(initial.enabled) }
        var endpoint by remember { mutableStateOf(initial.endpoint) }
        var model by remember { mutableStateOf(initial.model) }
        var status by remember { mutableStateOf("Remote provider is owner-disabled by default.") }

        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Remote Provider", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Enable remote answers", fontWeight = FontWeight.SemiBold)
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        Text("Only conversational text crosses this boundary. Actions and memory writes remain local and approval-controlled.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("HTTPS endpoint") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(model, { model = it }, label = { Text("Model") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Bearer tokens are never stored on this screen. A secure runtime credential source is required separately.", style = MaterialTheme.typography.bodySmall)
                Text("Network access remains unavailable in builds without the audited INTERNET permission.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = {
                    val result = store.write(MayraProviderSettings(enabled, endpoint, model))
                    status = result.fold({ "Settings saved. Restart Mayra to apply composition changes." }, { it.message ?: "Invalid provider settings." })
                }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
                OutlinedButton(onClick = {
                    store.disable()
                    enabled = false
                    status = "Remote provider disabled immediately for the next composition."
                }, modifier = Modifier.fillMaxWidth()) { Text("Emergency disable") }
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
