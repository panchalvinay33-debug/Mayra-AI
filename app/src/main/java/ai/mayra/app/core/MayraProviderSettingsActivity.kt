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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class MayraProviderSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = AndroidMayraProviderSettingsStore(applicationContext)
        val credentials = AndroidMayraProviderCredentialStore(applicationContext)
        setContent { MayraAITheme { ProviderSettings(store, credentials) } }
    }

    @Composable
    private fun ProviderSettings(
        store: AndroidMayraProviderSettingsStore,
        credentials: AndroidMayraProviderCredentialStore
    ) {
        val initial = remember { store.read() }
        var enabled by remember { mutableStateOf(initial.enabled) }
        var endpoint by remember { mutableStateOf(initial.endpoint) }
        var model by remember { mutableStateOf(initial.model) }
        var apiKey by remember { mutableStateOf("") }
        var credentialConfigured by remember { mutableStateOf(credentials.hasCredential()) }
        var status by remember {
            mutableStateOf(
                if (credentialConfigured) "Encrypted provider credential is available."
                else "Add an API key to enable online answers."
            )
        }

        fun refreshAssistant(successMessage: String): String =
            MayraAssistantComposition.rebuild(applicationContext).fold(
                onSuccess = { successMessage },
                onFailure = { error ->
                    "Settings were saved, but Mayra could not refresh the assistant: ${error.message ?: "unknown error"}."
                }
            )

        Scaffold { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("AI Provider", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Enable online answers", fontWeight = FontWeight.SemiBold)
                            Switch(checked = enabled, onCheckedChange = { enabled = it })
                        }
                        Text(
                            "Only conversational text may leave the device. Actions, confirmations and memory writes stay inside Mayra.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            if (credentialConfigured) "API key: stored securely ✓" else "API key: not configured",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it },
                    label = { Text("HTTPS endpoint") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(if (credentialConfigured) "Replace API key (optional)" else "API key") },
                    placeholder = { Text("Stored with Android Keystore") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                Text(
                    "Mayra never reads the saved key back into this screen. The encryption key remains in Android Keystore.",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        val keyResult = if (apiKey.isBlank()) Result.success(Unit) else credentials.write(apiKey)
                        status = keyResult.fold(
                            onSuccess = {
                                credentialConfigured = credentials.hasCredential()
                                val settingsResult = store.write(MayraProviderSettings(enabled, endpoint, model))
                                settingsResult.fold(
                                    onSuccess = {
                                        apiKey = ""
                                        refreshAssistant(
                                            if (enabled && credentialConfigured) {
                                                "Provider settings saved and online answers are active now."
                                            } else if (enabled) {
                                                "Settings saved. Add a valid API key before online answers can start."
                                            } else {
                                                "Provider settings saved. Local Mayra is active."
                                            }
                                        )
                                    },
                                    onFailure = { it.message ?: "Invalid provider settings." }
                                )
                            },
                            onFailure = { it.message ?: "Could not secure the API key." }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save provider") }

                OutlinedButton(
                    onClick = {
                        store.disable()
                        enabled = false
                        status = refreshAssistant("Online answers disabled immediately. Local Mayra remains available.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Disable online answers") }

                OutlinedButton(
                    onClick = {
                        credentials.clear()
                        credentialConfigured = false
                        apiKey = ""
                        store.disable()
                        enabled = false
                        status = refreshAssistant("Encrypted API key removed. Online answers are disabled immediately.")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Remove API key") }

                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
