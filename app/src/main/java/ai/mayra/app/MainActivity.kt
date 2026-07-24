package ai.mayra.app

import ai.mayra.app.chat.ChatViewModel
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.platform.device.AndroidDevicePermissionStateReader
import ai.mayra.app.platform.device.AndroidInstalledAppDataSource
import ai.mayra.app.platform.device.DevicePermissionSnapshotProvider
import ai.mayra.app.runtime.RuntimeControlDialog
import ai.mayra.app.runtime.RuntimeControlViewModel
import ai.mayra.app.settings.MayraSettingsStore
import ai.mayra.app.settings.SettingsActivity
import ai.mayra.app.ui.theme.MayraAITheme
import ai.mayra.app.voice.AndroidVoiceAssistant
import ai.mayra.app.voice.MicrophonePermission
import ai.mayra.app.voice.RealtimeVoiceLoopPolicy
import ai.mayra.app.voice.VoiceState
import ai.mayra.app.voice.VoiceTransportState
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!MayraSettingsStore(this).read().onboardingCompleted) {
            startActivity(
                Intent(this, SettingsActivity::class.java)
                    .putExtra(SettingsActivity.EXTRA_ONBOARDING, true)
            )
        }
        setContent { MayraAITheme { MayraHome() } }
    }
}

@Composable
private fun MayraHome(
    chatViewModel: ChatViewModel = viewModel(),
    runtimeViewModel: RuntimeControlViewModel = viewModel()
) {
    val state by chatViewModel.uiState.collectAsStateWithLifecycle()
    val runtimeState by runtimeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsStore = remember(context) { MayraSettingsStore(context) }
    var settings by remember { mutableStateOf(settingsStore.read()) }
    val listState = rememberLazyListState()
    var voiceState by remember { mutableStateOf(VoiceState()) }
    var showReadiness by remember { mutableStateOf(false) }
    var showRuntime by remember { mutableStateOf(false) }
    var readinessRefresh by remember { mutableIntStateOf(0) }
    val voiceLoopPolicy = remember { RealtimeVoiceLoopPolicy() }
    val voiceAssistant = remember {
        AndroidVoiceAssistant(context) { newState -> voiceState = newState }
    }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        settings = settingsStore.read()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) settings = settingsStore.read()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionReader = remember(context, readinessRefresh) {
        AndroidDevicePermissionStateReader(context)
    }
    val permissionSnapshot = remember(permissionReader, readinessRefresh) {
        DevicePermissionSnapshotProvider(permissionReader).snapshot()
    }
    val installedAppsCount = remember(context, readinessRefresh) {
        runCatching { AndroidInstalledAppDataSource(context).loadLaunchableApps().size }.getOrDefault(0)
    }
    val microphoneReady = remember(context, readinessRefresh) {
        MicrophonePermission.isGranted(context)
    }

    LaunchedEffect(voiceState.partialTranscript, voiceState.isFinalTranscript) {
        if (!voiceState.isFinalTranscript && voiceState.partialTranscript.isNotBlank()) {
            chatViewModel.updateInput(voiceState.partialTranscript)
        }
    }

    LaunchedEffect(
        voiceState.isFinalTranscript,
        voiceState.transcript,
        voiceState.recognitionConfidence,
        state.isThinking
    ) {
        val decision = voiceLoopPolicy.onVoiceState(voiceState, assistantBusy = state.isThinking)
        decision.submitTranscript?.let { transcript ->
            voiceAssistant.stopListening()
            chatViewModel.updateInput(transcript)
            chatViewModel.sendMessage()
        }
    }

    LaunchedEffect(state.messages.size, settings.speakResponses) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
        val latest = state.messages.lastOrNull()
        if (settings.speakResponses && latest?.sender == MayraMessage.Sender.MAYRA) {
            val decision = voiceLoopPolicy.onAssistantResponse(
                responseText = latest.text,
                responseKey = latest.timestamp.toString(),
                continuousMode = voiceState.continuousMode
            )
            decision.speakResponse?.let {
                voiceAssistant.speak(it, listenAfter = decision.listenAfterSpeech)
            }
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            val decision = voiceLoopPolicy.onAssistantFailure(voiceState.continuousMode)
            if (decision.startListening) voiceAssistant.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceLoopPolicy.reset()
            voiceAssistant.release()
        }
    }

    val microphoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        readinessRefresh++
        if (granted) {
            voiceAssistant.setContinuousMode(settings.continuousVoiceByDefault)
            voiceAssistant.startListening()
        } else {
            voiceState = VoiceState(
                transportState = VoiceTransportState.ERROR,
                error = "Microphone permission is required for voice input"
            )
        }
    }

    val devicePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        readinessRefresh++
    }

    fun startVoiceConversation() {
        if (MicrophonePermission.isGranted(context)) {
            voiceAssistant.setContinuousMode(settings.continuousVoiceByDefault)
            voiceAssistant.startListening()
        } else {
            microphoneLauncher.launch(MicrophonePermission.permission)
        }
    }

    fun stopVoiceConversation() {
        voiceAssistant.setContinuousMode(false)
        voiceAssistant.stopListening()
    }

    fun requestDevicePermissions() {
        devicePermissionsLauncher.launch(runtimePermissionNames())
    }

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, tonalElevation = 6.dp, modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("M", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mayra AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(voiceStatusText(state.isThinking, voiceState))
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = { showReadiness = true },
                    label = { Text("Device") }
                )
                AssistChip(
                    onClick = {
                        runtimeViewModel.refresh()
                        showRuntime = true
                    },
                    label = { Text("Runtime") }
                )
                AssistChip(
                    onClick = {
                        settingsLauncher.launch(Intent(context, SettingsActivity::class.java))
                    },
                    label = { Text("Settings") }
                )
                Spacer(Modifier.weight(1f))
                if (state.messages.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            voiceLoopPolicy.reset()
                            chatViewModel.clearConversation()
                        },
                        enabled = !state.isThinking
                    ) {
                        Text("Clear")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.messages.isEmpty()) item {
                    Text(
                        welcomeMessage(settings.normalizedName),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                items(state.messages, key = { it.timestamp }) { message ->
                    val label = if (message.sender == MayraMessage.Sender.USER) "You" else "Mayra"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Text(message.text)
                        }
                    }
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            voiceState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (voiceState.isListening && voiceState.partialTranscript.isNotBlank()) {
                Text(
                    "Heard: ${voiceState.partialTranscript}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = chatViewModel::updateInput,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask Mayra anything…") },
                enabled = !state.isThinking,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { chatViewModel.sendMessage() })
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        when {
                            voiceState.isSpeaking -> voiceAssistant.interruptSpeech(resumeListening = true)
                            voiceState.continuousMode -> stopVoiceConversation()
                            else -> startVoiceConversation()
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(
                        when {
                            voiceState.isSpeaking -> "Interrupt"
                            voiceState.continuousMode -> "Stop voice"
                            else -> "🎙 Talk"
                        }
                    )
                }
                Button(
                    onClick = chatViewModel::sendMessage,
                    enabled = state.input.isNotBlank() && !state.isThinking,
                    modifier = Modifier.weight(2f).height(52.dp)
                ) {
                    Text(if (state.isThinking) "Thinking…" else "Send to Mayra")
                }
            }
        }
    }

    if (showReadiness) {
        DeviceReadinessDialog(
            microphoneReady = microphoneReady,
            speechAvailable = voiceState.speechAvailable,
            ttsReady = voiceState.ttsReady,
            grantedPermissions = permissionSnapshot.granted,
            installedAppsCount = installedAppsCount,
            onRequestPermissions = ::requestDevicePermissions,
            onRefresh = { readinessRefresh++ },
            onDismiss = { showReadiness = false }
        )
    }

    if (showRuntime) {
        RuntimeControlDialog(
            state = runtimeState,
            onRefresh = runtimeViewModel::refresh,
            onDismiss = { showRuntime = false }
        )
    }
}

internal fun welcomeMessage(userName: String): String = if (userName.isBlank()) {
    "Namaste. I’m Mayra. What can I help you with today?"
} else {
    "Namaste, $userName. I’m Mayra. What can I help you with today?"
}

private fun voiceStatusText(isThinking: Boolean, voiceState: VoiceState): String = when {
    isThinking -> "Thinking…"
    voiceState.isSpeaking -> "Speaking… tap Interrupt to talk"
    voiceState.isListening -> "Listening…"
    voiceState.transportState == VoiceTransportState.PROCESSING -> "Understanding…"
    voiceState.continuousMode -> "Voice conversation active"
    !voiceState.speechAvailable -> "Speech recognition unavailable"
    else -> "● Ready to help"
}

@Composable
private fun DeviceReadinessDialog(
    microphoneReady: Boolean,
    speechAvailable: Boolean,
    ttsReady: Boolean,
    grantedPermissions: Set<DevicePermission>,
    installedAppsCount: Int,
    onRequestPermissions: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val rows = listOf(
        DevicePermission.READ_CONTACTS to "Find contacts",
        DevicePermission.CALL_PHONE to "Start phone calls",
        DevicePermission.SEND_MESSAGES to "Prepare messages",
        DevicePermission.POST_NOTIFICATIONS to "Assistant notifications",
        DevicePermission.SCHEDULE_EXACT_ALARM to "Exact reminders",
        DevicePermission.QUERY_APPS to "Open installed apps"
    )
    val voiceReadyCount = listOf(microphoneReady, speechAvailable, ttsReady).count { it }
    val readyCount = rows.count { it.first in grantedPermissions } + voiceReadyCount
    val totalCount = rows.size + 3

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Device readiness") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$readyCount of $totalCount capabilities ready")
                Text("$installedAppsCount launchable apps detected")
                HorizontalDivider()
                ReadinessRow("Microphone permission", microphoneReady)
                ReadinessRow("Speech recognition service", speechAvailable)
                ReadinessRow("Mayra voice output", ttsReady)
                rows.forEach { (permission, label) ->
                    ReadinessRow(label, permission in grantedPermissions)
                }
                Text(
                    "Voice recognition quality and offline availability depend on the speech service installed on the phone.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(onClick = onRequestPermissions) { Text("Allow permissions") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onRefresh) { Text("Refresh") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun ReadinessRow(label: String, ready: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Text(if (ready) "Ready ✓" else "Permission needed")
    }
}

private fun runtimePermissionNames(): Array<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.CALL_PHONE)
    add(Manifest.permission.SEND_SMS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
    }
}.toTypedArray()
