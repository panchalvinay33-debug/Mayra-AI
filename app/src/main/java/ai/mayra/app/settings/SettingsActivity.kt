package ai.mayra.app.settings

import ai.mayra.app.ai.AiProviderActivity
import ai.mayra.app.ai.AiProviderSettingsStore
import ai.mayra.app.background.MayraNotificationCenterActivity
import ai.mayra.app.pulse.MayraPulseActivity
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val onboarding = intent.getBooleanExtra(EXTRA_ONBOARDING, false)
        setContent {
            MayraAITheme {
                MayraSettingsScreen(
                    onboarding = onboarding,
                    store = remember { MayraSettingsStore(applicationContext) },
                    aiProviderStore = remember { AiProviderSettingsStore(applicationContext) },
                    onOpenAiProvider = { startActivity(Intent(this, AiProviderActivity::class.java)) },
                    onOpenPulse = { startActivity(Intent(this, MayraPulseActivity::class.java)) },
                    onOpenNotifications = { startActivity(Intent(this, MayraNotificationCenterActivity::class.java)) },
                    onClose = ::finish
                )
            }
        }
    }
    companion object { const val EXTRA_ONBOARDING = "mayra.extra.ONBOARDING" }
}

@Composable
private fun MayraSettingsScreen(
    onboarding: Boolean,
    store: MayraSettingsStore,
    aiProviderStore: AiProviderSettingsStore,
    onOpenAiProvider: () -> Unit,
    onOpenPulse: () -> Unit,
    onOpenNotifications: () -> Unit,
    onClose: () -> Unit
) {
    var settings by remember { mutableStateOf(store.read()) }
    var aiProviderConfig by remember { mutableStateOf(aiProviderStore.read()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(if (onboarding) "Welcome to Mayra AI" else "Mayra settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(if (onboarding) "Set up how Mayra should speak, remember and personalize your experience." else settings.summary())

            SettingsSection("Mayra presence") {
                Text("See what Mayra senses about this phone right now—battery, network, storage, memory, heat and available capabilities.")
                OutlinedButton(onClick = onOpenPulse, modifier = Modifier.fillMaxWidth()) { Text("Open Mayra Pulse") }
            }
            SettingsSection("Notification intelligence") {
                Text("Review locally captured unread notifications, protected summaries, per-app privacy and supported quick replies.")
                Text("Notification Access is optional and can be revoked from Android settings at any time.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenNotifications, modifier = Modifier.fillMaxWidth()) { Text("Open Notification Center") }
            }
            SettingsSection("Your profile") {
                OutlinedTextField(
                    value = settings.userName,
                    onValueChange = { value -> settings = settings.copy(userName = value.take(MayraSettings.MAX_NAME_LENGTH)); error = null },
                    label = { Text("What should Mayra call you?") },
                    supportingText = { Text("Stored only in this app on your device.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            SettingsSection("Language") {
                MayraLanguage.entries.forEach { language ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = settings.language == language, onClick = { settings = settings.copy(language = language) })
                        Text(language.label)
                    }
                }
            }
            SettingsSection("Voice identity") {
                SettingsToggle("Speak Mayra's responses", "Read assistant replies aloud.", settings.speakResponses) { settings = settings.copy(speakResponses = it) }
                SettingsToggle("Start continuous voice by default", "Keep listening after Mayra finishes speaking.", settings.continuousVoiceByDefault) { settings = settings.copy(continuousVoiceByDefault = it) }
                SettingsToggle("Prefer high-quality offline voice", "Automatically choose the best installed non-network voice for the selected language.", settings.preferHighQualityOfflineVoice) { settings = settings.copy(preferHighQualityOfflineVoice = it) }
                Text("Voice style", fontWeight = FontWeight.Medium)
                MayraVoiceStyle.entries.forEach { style ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = settings.voiceStyle == style,
                            onClick = { settings = settings.copy(voiceStyle = style, voiceRate = style.defaultRate, voicePitch = style.defaultPitch) }
                        )
                        Column {
                            Text(style.label)
                            Text(style.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text("Speaking speed: ${"%.2f".format(settings.voiceRate)}×")
                Slider(
                    value = settings.voiceRate,
                    onValueChange = { settings = settings.copy(voiceRate = it) },
                    valueRange = MayraSettings.MIN_VOICE_RATE..MayraSettings.MAX_VOICE_RATE
                )
                Text("Voice pitch: ${"%.2f".format(settings.voicePitch)}×")
                Slider(
                    value = settings.voicePitch,
                    onValueChange = { settings = settings.copy(voicePitch = it) },
                    valueRange = MayraSettings.MIN_VOICE_PITCH..MayraSettings.MAX_VOICE_PITCH
                )
                Text("Save settings, return to chat, and Mayra will use the new voice on her next reply.", style = MaterialTheme.typography.bodySmall)
            }
            SettingsSection("AI intelligence") {
                aiProviderConfig = aiProviderStore.read()
                Text(aiProviderConfig.status())
                Text("Connect OpenAI for full conversation while Mayra keeps phone actions inside its local safety layer.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onOpenAiProvider, modifier = Modifier.fillMaxWidth()) { Text("Configure AI provider") }
            }
            SettingsSection("Memory and personalization") {
                SettingsToggle("Memory", "Allow Mayra to remember useful preferences and context.", settings.memoryEnabled) { enabled -> settings = settings.copy(memoryEnabled = enabled, personalizationEnabled = settings.personalizationEnabled && enabled) }
                SettingsToggle("Personalization", "Use saved preferences to tailor answers and actions.", settings.personalizationEnabled, enabled = settings.memoryEnabled) { settings = settings.copy(personalizationEnabled = it) }
            }
            SettingsSection("Privacy") {
                SettingsToggle("Share anonymous diagnostics", "Off by default. No conversations are included by this setting.", settings.diagnosticsSharingEnabled) { settings = settings.copy(diagnosticsSharingEnabled = it) }
                Text("Profile and voice preferences stay in local app storage. The AI provider key is encrypted with Android Keystore.", style = MaterialTheme.typography.bodySmall)
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    val validation = settings.validationMessage()
                    if (validation != null) error = validation else {
                        if (onboarding) store.completeOnboarding(settings) else store.save(settings)
                        onClose()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (onboarding) "Finish setup" else "Save settings") }

            if (!onboarding) {
                OutlinedButton(onClick = { showResetConfirmation = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset Mayra settings") }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = { Text("Reset settings?") },
            text = { Text("This clears your profile, AI provider key, language, voice and memory preferences. Onboarding will appear again.") },
            confirmButton = {
                TextButton(onClick = {
                    store.reset(); aiProviderStore.reset(); settings = store.read(); aiProviderConfig = aiProviderStore.read()
                    showResetConfirmation = false; onClose()
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirmation = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}