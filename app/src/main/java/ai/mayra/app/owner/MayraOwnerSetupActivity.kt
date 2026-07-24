package ai.mayra.app.owner

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MayraOwnerSetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { OwnerSetupScreen(onClose = ::finish) } }
    }
}

@Composable
private fun OwnerSetupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MayraOwnerModeStore(context) }
    val inspector = remember(context) { MayraOwnerCapabilityInspector(context) }
    var preferences by remember { mutableStateOf(store.read()) }
    var refresh by remember { mutableIntStateOf(0) }
    val statuses = remember(refresh) { inspector.snapshot() }
    val score = remember(refresh) { inspector.readinessScore(statuses) }
    var notice by remember { mutableStateOf<String?>(null) }

    fun save(updated: MayraOwnerPreferences, message: String) {
        preferences = updated
        store.save(updated)
        notice = message
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Owner Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Private sideload mode for your own phone. This maximizes supported Android access without pretending to bypass Android security.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Access readiness", fontWeight = FontWeight.SemiBold)
                    Text("$score / 100")
                    Text(ownerModeSafetySummary(preferences), style = MaterialTheme.typography.bodySmall)
                }
            }

            notice?.let { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) } }

            SettingToggle("Owner Mode", "Use personal-device defaults instead of future Play Store defaults.", preferences.enabled) {
                save(preferences.copy(enabled = it), if (it) "Owner Mode enabled." else "Owner Mode disabled.")
            }
            SettingToggle("Direct low-risk actions", "Open apps, show routes and similar low-risk actions without extra confirmation.", preferences.directLowRiskActions) {
                save(preferences.copy(directLowRiskActions = it), "Low-risk action preference updated.")
            }
            SettingToggle("Direct medium-risk actions", "Run reminders and similar medium-risk actions directly on your phone.", preferences.directMediumRiskActions) {
                save(preferences.copy(directMediumRiskActions = it), "Medium-risk action preference updated.")
            }
            SettingToggle(
                "Trusted direct call/message handoffs",
                "Optional personal mode. Mayra may skip the extra confirmation for ordinary call and message handoffs. Sensitive, destructive, financial, legal and critical actions never use this bypass.",
                preferences.trustedDirectHandoffs
            ) {
                save(preferences.copy(trustedDirectHandoffs = it), if (it) "Trusted direct handoffs enabled." else "Trusted direct handoffs disabled.")
            }
            SettingToggle("Proactive living presence", "Allow Mayra to surface phone health, notification and routine suggestions.", preferences.proactivePresence) {
                save(preferences.copy(proactivePresence = it), "Proactive presence preference updated.")
            }
            SettingToggle("Background runtime", "Keep supported workers and monitoring active under Android background limits.", preferences.keepBackgroundRuntime) {
                save(preferences.copy(keepBackgroundRuntime = it), "Background runtime preference updated.")
            }

            Text("Phone access checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            statuses.forEach { status ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(status.title, fontWeight = FontWeight.SemiBold)
                            Text(status.state.name.replace('_', ' ').lowercase())
                        }
                        Text(status.detail, style = MaterialTheme.typography.bodySmall)
                        status.settingsIntent?.let { intent ->
                            OutlinedButton(
                                onClick = {
                                    runCatching { context.startActivity(intent) }
                                        .onFailure { notice = "This settings screen is not available on this device." }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Open setup") }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Hard Android limits", fontWeight = FontWeight.SemiBold)
                    Text("• Full incoming-call control requires Mayra to become the default dialer and implement complete Telecom UI.")
                    Text("• Accessibility must be manually enabled and cannot bypass secure fields, banking protections or app security.")
                    Text("• Root-only operations will not work on a normal non-rooted phone.")
                    Text("• Background execution can still be delayed by Android/OEM battery management.")
                    Text("• WhatsApp and other apps only support what their intents, APIs or notification actions expose.")
                }
            }

            Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh access status") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

@Composable
private fun SettingToggle(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
