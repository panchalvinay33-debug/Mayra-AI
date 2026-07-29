package ai.mayra.app

import ai.mayra.app.chat.ChatViewModel
import ai.mayra.app.core.MayraActivityHistoryActivity
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.MayraProviderSettingsActivity
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.document.MayraDocumentActivity
import ai.mayra.app.memory.MayraMemoryCenterActivity
import ai.mayra.app.platform.device.AndroidDevicePermissionStateReader
import ai.mayra.app.platform.device.AndroidInstalledAppDataSource
import ai.mayra.app.platform.device.DevicePermissionSnapshotProvider
import ai.mayra.app.ui.theme.MayraAITheme
import ai.mayra.app.voice.AndroidVoiceAssistant
import ai.mayra.app.voice.MicrophonePermission
import ai.mayra.app.voice.VoiceState
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraHome() } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MayraHome(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var voiceState by remember { mutableStateOf(VoiceState()) }
    var lastSpokenMessageId by remember { mutableStateOf<Long?>(null) }
    var showReadiness by remember { mutableStateOf(false) }
    var readinessRefresh by remember { mutableIntStateOf(0) }
    val voiceAssistant = remember { AndroidVoiceAssistant(context) { newState -> voiceState = newState } }
    val permissionReader = remember(context, readinessRefresh) { AndroidDevicePermissionStateReader(context) }
    val permissionSnapshot = remember(permissionReader, readinessRefresh) { DevicePermissionSnapshotProvider(permissionReader).snapshot() }
    val installedAppsCount = remember(context, readinessRefresh) { runCatching { AndroidInstalledAppDataSource(context).loadLaunchableApps().size }.getOrDefault(0) }
    val microphoneReady = remember(context, readinessRefresh) { MicrophonePermission.isGranted(context) }

    LaunchedEffect(voiceState.transcript, voiceState.isListening) {
        if (voiceState.transcript.isNotBlank()) viewModel.updateInput(voiceState.transcript)
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
        val latest = state.messages.lastOrNull()
        if (latest?.sender == MayraMessage.Sender.MAYRA && latest.timestamp != lastSpokenMessageId) {
            lastSpokenMessageId = latest.timestamp
            voiceAssistant.speak(latest.text)
        }
    }
    DisposableEffect(Unit) { onDispose { voiceAssistant.release() } }

    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        readinessRefresh++
        if (granted) voiceAssistant.startListening() else voiceState = VoiceState(error = "Microphone permission is required for voice input")
    }
    val devicePermissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { readinessRefresh++ }
    fun startVoice() {
        if (MicrophonePermission.isGranted(context)) voiceAssistant.startListening() else microphoneLauncher.launch(MicrophonePermission.permission)
    }
    fun requestDevicePermissions() { devicePermissionsLauncher.launch(runtimePermissionNames()) }
    fun openActivity(activity: Class<out ComponentActivity>) {
        context.startActivity(Intent(context, activity))
    }

    val interactionEnabled = !state.isThinking && state.pendingConfirmation == null && state.pendingMemoryApproval == null
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, tonalElevation = 6.dp, modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("M", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mayra AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(when {
                        state.isThinking -> "Thinking…"
                        state.pendingConfirmation != null || state.pendingMemoryApproval != null -> "Waiting for confirmation"
                        voiceState.isListening -> "Listening…"
                        else -> "● Offline core ready"
                    })
                    Text("Private on-device mode", style = MaterialTheme.typography.bodySmall)
                }
                if (state.messages.isNotEmpty()) {
                    TextButton(onClick = viewModel::clearConversation, enabled = interactionEnabled) { Text("Clear") }
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 3
            ) {
                AssistChip(onClick = { openActivity(MayraActivityHistoryActivity::class.java) }, label = { Text("History") })
                AssistChip(onClick = { openActivity(MayraDocumentActivity::class.java) }, label = { Text("Library") })
                AssistChip(onClick = { openActivity(MayraMemoryCenterActivity::class.java) }, label = { Text("Memory") })
                AssistChip(onClick = { openActivity(MayraProviderSettingsActivity::class.java) }, label = { Text("Provider") })
                AssistChip(onClick = { showReadiness = true }, label = { Text("Device") })
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.messages.isEmpty()) item {
                    Text("Namaste. I’m Mayra. What can I help you with today?", style = MaterialTheme.typography.titleMedium)
                }
                items(state.messages, key = { it.timestamp }) { message ->
                    val label = if (message.sender == MayraMessage.Sender.USER) "You" else "Mayra"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Text(message.text)
                            if (message.usedPersonalMemoryKeys.isNotEmpty()) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    message.usedPersonalMemoryKeys.take(3).forEach { key ->
                                        AssistChip(onClick = {}, label = { Text("🧠 $key") })
                                    }
                                }
                                if (message.usedPersonalMemoryKeys.size > 3) {
                                    Text("+${message.usedPersonalMemoryKeys.size - 3} more approved memories", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            voiceState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::updateInput,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask Mayra anything…") },
                enabled = interactionEnabled,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() })
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { if (voiceState.isListening) voiceAssistant.stopListening() else startVoice() },
                    enabled = interactionEnabled,
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text(if (voiceState.isListening) "Stop" else "🎙 Voice") }
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = state.input.isNotBlank() && interactionEnabled,
                    modifier = Modifier.weight(2f).height(52.dp)
                ) { Text(if (state.isThinking) "Thinking…" else "Send to Mayra") }
            }
        }
    }

    state.pendingConfirmation?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingAction,
            title = { Text("Confirm action") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(pending.prompt); Text(pending.message, fontWeight = FontWeight.SemiBold); Text("Only this exact request can use the one-time confirmation token.", style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { Button(onClick = viewModel::confirmPendingAction, enabled = !state.isThinking) { Text("Confirm") } },
            dismissButton = { TextButton(onClick = viewModel::cancelPendingAction, enabled = !state.isThinking) { Text("Cancel") } }
        )
    }
    state.pendingMemoryApproval?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPendingMemory,
            title = { Text(if (pending.previousValue == null) "Save this memory?" else "Replace this memory?") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(pending.key, fontWeight = FontWeight.Bold); pending.previousValue?.let { Text("Current: $it"); HorizontalDivider() }; Text("New: ${pending.newValue}"); Text(if (pending.previousValue == null) "Mayra will store this locally only after you tap Save." else "Saving will replace the current value and increase its revision.", style = MaterialTheme.typography.bodySmall) } },
            confirmButton = { Button(onClick = viewModel::savePendingMemory, enabled = !state.isThinking) { Text(if (pending.previousValue == null) "Save" else "Replace") } },
            dismissButton = { TextButton(onClick = viewModel::cancelPendingMemory, enabled = !state.isThinking) { Text("Not now") } }
        )
    }
    if (showReadiness) DeviceReadinessDialog(microphoneReady, permissionSnapshot.granted, installedAppsCount, ::requestDevicePermissions, { readinessRefresh++ }, { showReadiness = false })
}

@Composable
private fun DeviceReadinessDialog(microphoneReady: Boolean, grantedPermissions: Set<DevicePermission>, installedAppsCount: Int, onRequestPermissions: () -> Unit, onRefresh: () -> Unit, onDismiss: () -> Unit) {
    val rows = listOf(DevicePermission.READ_CONTACTS to "Find contacts", DevicePermission.CALL_PHONE to "Start phone calls", DevicePermission.SEND_MESSAGES to "Prepare messages", DevicePermission.POST_NOTIFICATIONS to "Assistant notifications", DevicePermission.SCHEDULE_EXACT_ALARM to "Exact reminders", DevicePermission.QUERY_APPS to "Open installed apps")
    val readyCount = rows.count { it.first in grantedPermissions } + if (microphoneReady) 1 else 0
    val totalCount = rows.size + 1
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Device readiness") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("$readyCount of $totalCount capabilities ready"); Text("$installedAppsCount launchable apps detected"); HorizontalDivider(); ReadinessRow("Voice input", microphoneReady); rows.forEach { (permission, label) -> ReadinessRow(label, permission in grantedPermissions) }; Text("Exact reminders may require enabling special access from Android settings.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = onRequestPermissions) { Text("Allow permissions") } }, dismissButton = { Row { TextButton(onClick = onRefresh) { Text("Refresh") }; TextButton(onClick = onDismiss) { Text("Close") } } })
}

@Composable
private fun ReadinessRow(label: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, modifier = Modifier.weight(1f)); Text(if (ready) "Ready ✓" else "Permission needed") }
}

private fun runtimePermissionNames(): Array<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO); add(Manifest.permission.READ_CONTACTS); add(Manifest.permission.CALL_PHONE); add(Manifest.permission.SEND_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()
