package ai.mayra.app.ai

import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class AiProviderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                AiProviderScreen(
                    store = remember { AiProviderSettingsStore(applicationContext) },
                    tester = remember { AiProviderConnectionTester() },
                    onClose = ::finish
                )
            }
        }
    }
}

@Composable
private fun AiProviderScreen(
    store: AiProviderSettingsStore,
    tester: AiProviderConnectionTester,
    onClose: () -> Unit
) {
    var config by remember { mutableStateOf(store.read()) }
    var apiKeyInput by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refreshConfig() {
        config = runCatching { store.read() }.getOrElse {
            notice = "AI provider settings could not be read. Mayra will stay in local mode."
            AiProviderConfig()
        }
    }

    fun saveConfig(): Boolean {
        val validation = config.validationMessage(apiKeyInput)
        if (validation != null) {
            notice = validation
            return false
        }
        return runCatching {
            store.save(config, apiKeyInput.takeIf(String::isNotBlank))
            refreshConfig()
            apiKeyInput = ""
            notice = "AI provider settings saved."
            true
        }.getOrElse { error ->
            notice = AiProviderSafetyPolicy.sanitizeConnectionMessage(error.message)
            false
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("AI provider", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(config.status())

            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Choose intelligence mode", fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    AiProviderKind.entries.forEach { provider ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.provider == provider,
                                onClick = {
                                    config = config.copy(provider = provider)
                                    notice = null
                                }
                            )
                            Column {
                                Text(provider.label)
                                Text(
                                    if (provider == AiProviderKind.LOCAL_ONLY) {
                                        "Private offline commands and basic chat."
                                    } else {
                                        "Use OpenAI for general conversation; phone actions stay in Mayra's local safety layer."
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (config.provider == AiProviderKind.OPENAI) {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("OpenAI connection", fontWeight = FontWeight.SemiBold)
                        HorizontalDivider()
                        OutlinedTextField(
                            value = config.model,
                            onValueChange = {
                                config = config.copy(
                                    model = it.trimStart().take(AiProviderSafetyPolicy.MAX_MODEL_LENGTH)
                                )
                            },
                            label = { Text("Model") },
                            supportingText = { Text("Default: ${AiProviderConfig.DEFAULT_OPENAI_MODEL}") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apiKeyInput,
                            onValueChange = {
                                apiKeyInput = it.trim().take(AiProviderSafetyPolicy.MAX_API_KEY_LENGTH)
                            },
                            label = {
                                Text(if (config.apiKeyConfigured) "Replace API key (optional)" else "OpenAI API key")
                            },
                            supportingText = {
                                Text(
                                    if (config.apiKeyConfigured) {
                                        "A readable key is encrypted in Android Keystore. Leave blank to keep it."
                                    } else {
                                        "The key is encrypted before local storage and is never displayed again."
                                    }
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Last check: ${config.lastConnectionMessage}", style = MaterialTheme.typography.bodySmall)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { saveConfig() },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) { Text("Save") }
                            OutlinedButton(
                                onClick = {
                                    if (!saveConfig()) return@OutlinedButton
                                    val key = runCatching { store.apiKey() }.getOrNull()
                                    if (key.isNullOrBlank()) {
                                        refreshConfig()
                                        notice = "The encrypted API key is unavailable. Enter and save it again."
                                        return@OutlinedButton
                                    }
                                    busy = true
                                    notice = "Testing OpenAI connection…"
                                    scope.launch {
                                        tester.test(key, store.read().model)
                                            .onSuccess { message ->
                                                val safeMessage = AiProviderSafetyPolicy.sanitizeConnectionMessage(message)
                                                store.recordConnection(true, safeMessage)
                                                refreshConfig()
                                                notice = safeMessage
                                            }
                                            .onFailure { error ->
                                                val message = AiProviderSafetyPolicy.sanitizeConnectionMessage(error.message)
                                                store.recordConnection(false, message)
                                                refreshConfig()
                                                notice = message
                                            }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.weight(1f)
                            ) { Text(if (busy) "Testing…" else "Test") }
                        }

                        if (config.apiKeyConfigured) {
                            TextButton(
                                onClick = {
                                    runCatching { store.clearApiKey() }
                                        .onSuccess {
                                            refreshConfig()
                                            apiKeyInput = ""
                                            notice = "OpenAI API key removed."
                                        }
                                        .onFailure {
                                            notice = "The API key could not be removed from local storage."
                                        }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Remove API key") }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("Safety and fallback", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "OpenAI is used only for normal conversation. Calls, messages, reminders and other phone actions continue through Mayra's local permission and confirmation system. If online AI fails, Mayra falls back to the offline assistant.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            notice?.let {
                Text(
                    it,
                    color = if (
                        it.contains("failed", ignoreCase = true) ||
                        it.contains("required", ignoreCase = true) ||
                        it.contains("invalid", ignoreCase = true) ||
                        it.contains("could not", ignoreCase = true)
                    ) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            if (config.provider == AiProviderKind.LOCAL_ONLY) {
                Button(
                    onClick = {
                        if (saveConfig()) notice = "Local assistant mode saved."
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Use local assistant") }
            }

            TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}
