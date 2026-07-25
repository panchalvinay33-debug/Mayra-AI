package ai.mayra.app.owner

import ai.mayra.app.ui.theme.MayraAITheme
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.Alignment
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

    val basicPermissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CONTACTS)
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.CALL_PHONE)
            add(Manifest.permission.SEND_SMS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refresh++
        val granted = results.count { it.value }
        notice = "$granted of ${results.size} requested permissions allowed. You can change any choice later in Android settings."
    }

    fun save(updated: MayraOwnerPreferences, message: String) {
        preferences = updated
        store.save(updated)
        notice = message
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Mayra Access Journey", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Give Mayra only the access you choose. Basic permissions use Android popups; special access opens the correct protected system screen.")

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Access readiness", fontWeight = FontWeight.SemiBold)
                        Text("$score%", fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth())
                    Text(ownerModeSafetySummary(preferences), style = MaterialTheme.typography.bodySmall)
                }
            }

            notice?.let {
                Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(14.dp)) }
            }

            AccessStepCard(
                number = "1",
                title = "Basic phone access",
                detail = "Microphone, contacts, camera, calls, SMS and notifications. Android will show its own permission dialogs.",
                action = "Request basic access",
                onAction = { permissionLauncher.launch(basicPermissions) }
            )

            Text("Special access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            statuses.filter { it.capability !in BASIC_CAPABILITIES }.forEach { status ->
                AccessStatusCard(
                    status = status,
                    onOpen = {
                        status.settingsIntent?.let { intent ->
                            runCatching { context.startActivity(intent) }
                                .onFailure { notice = "This settings screen is not available on this device." }
                        }
                    }
                )
            }

            Text("Owner preferences", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SettingToggle("Owner Mode", "Use personal-device defaults while keeping critical safeguards.", preferences.enabled) {
                save(preferences.copy(enabled = it), if (it) "Owner Mode enabled." else "Owner Mode disabled.")
            }
            SettingToggle("Direct low-risk actions", "Open apps and similar harmless actions without an extra confirmation.", preferences.directLowRiskActions) {
                save(preferences.copy(directLowRiskActions = it), "Low-risk action preference updated.")
            }
            SettingToggle("Direct medium-risk actions", "Run reminders and similar personal actions directly.", preferences.directMediumRiskActions) {
                save(preferences.copy(directMediumRiskActions = it), "Medium-risk action preference updated.")
            }
            SettingToggle(
                "Trusted call/message handoffs",
                "Optional. Sensitive, destructive, financial, legal and critical actions remain protected.",
                preferences.trustedDirectHandoffs
            ) {
                save(preferences.copy(trustedDirectHandoffs = it), "Trusted handoff preference updated.")
            }
            SettingToggle("Proactive living presence", "Allow useful phone-health, notification and routine suggestions.", preferences.proactivePresence) {
                save(preferences.copy(proactivePresence = it), "Proactive presence preference updated.")
            }
            SettingToggle("Background runtime", "Keep supported reminder and companion services active under Android limits.", preferences.keepBackgroundRuntime) {
                save(preferences.copy(keepBackgroundRuntime = it), "Background runtime preference updated.")
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Protected boundaries", fontWeight = FontWeight.SemiBold)
                    Text("Mayra does not bypass secure fields, banking protections, OTP rules, Android consent screens or third-party app restrictions.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh access status") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Finish access setup") }
        }
    }
}

@Composable
private fun AccessStepCard(number: String, title: String, detail: String, action: String, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STEP $number", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(action) }
        }
    }
}

@Composable
private fun AccessStatusCard(status: OwnerCapabilityStatus, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(status.title, fontWeight = FontWeight.SemiBold)
                Text(
                    when (status.state) {
                        OwnerAccessState.READY -> "Ready"
                        OwnerAccessState.ACTION_REQUIRED -> "Setup"
                        OwnerAccessState.DEVICE_UNSUPPORTED -> "Unavailable"
                    },
                    color = if (status.state == OwnerAccessState.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(status.detail, style = MaterialTheme.typography.bodySmall)
            if (status.state == OwnerAccessState.ACTION_REQUIRED && status.settingsIntent != null) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text("Open Android setup") }
            }
        }
    }
}

@Composable
private fun SettingToggle(title: String, detail: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private val BASIC_CAPABILITIES = setOf(
    OwnerCapability.MICROPHONE,
    OwnerCapability.CONTACTS,
    OwnerCapability.CAMERA,
    OwnerCapability.PHONE_CALLS,
    OwnerCapability.SMS,
    OwnerCapability.NOTIFICATIONS
)
