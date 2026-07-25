package ai.mayra.app.presence

import ai.mayra.app.chat.ChatViewModel
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.runtime.RuntimeControlActivity
import ai.mayra.app.settings.MayraSettingsStore
import ai.mayra.app.settings.SettingsActivity
import ai.mayra.app.testing.MayraDeviceTestActivity
import ai.mayra.app.ui.theme.MayraAITheme
import ai.mayra.app.voice.AndroidVoiceAssistant
import ai.mayra.app.voice.MicrophonePermission
import ai.mayra.app.voice.RealtimeVoiceLoopPolicy
import ai.mayra.app.voice.VoiceState
import ai.mayra.app.voice.VoiceTransportState
import ai.mayra.app.workspace.MayraWorkspaceActivity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MayraCompanionActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = MayraSettingsStore(this)
        if (!settingsStore.read().onboardingCompleted) {
            startActivity(Intent(this, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_ONBOARDING, true))
        }
        setContent {
            MayraAITheme {
                MayraCompanionScreen(
                    onOpenWorkspace = { startActivity(Intent(this, MayraWorkspaceActivity::class.java)) },
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onOpenDevice = { startActivity(Intent(this, MayraDeviceTestActivity::class.java)) },
                    onOpenRuntime = { startActivity(Intent(this, RuntimeControlActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun MayraCompanionScreen(
    onOpenWorkspace: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenRuntime: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val settingsStore = remember(context) { MayraSettingsStore(context) }
    var settings by remember { mutableStateOf(settingsStore.read()) }
    var voiceState by remember { mutableStateOf(VoiceState()) }
    val voiceLoopPolicy = remember { RealtimeVoiceLoopPolicy() }
    val voiceAssistant = remember { AndroidVoiceAssistant(context) { next -> voiceState = next } }

    val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) settings = settingsStore.read()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
        uiState.isThinking
    ) {
        val decision = voiceLoopPolicy.onVoiceState(voiceState, assistantBusy = uiState.isThinking)
        decision.submitTranscript?.let { transcript ->
            voiceAssistant.stopListening()
            chatViewModel.updateInput(transcript)
            chatViewModel.sendMessage()
        }
    }

    LaunchedEffect(uiState.messages.size, settings.speakResponses) {
        val latest = uiState.messages.lastOrNull()
        if (settings.speakResponses && latest?.sender == MayraMessage.Sender.MAYRA) {
            val decision = voiceLoopPolicy.onAssistantResponse(
                responseText = latest.text,
                responseKey = latest.timestamp.toString(),
                continuousMode = voiceState.continuousMode
            )
            decision.speakResponse?.let { text ->
                voiceAssistant.speak(text, listenAfter = decision.listenAfterSpeech)
            }
        }
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
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

    fun startVoice() {
        if (MicrophonePermission.isGranted(context)) {
            voiceAssistant.setContinuousMode(settings.continuousVoiceByDefault)
            voiceAssistant.startListening()
        } else {
            microphoneLauncher.launch(MicrophonePermission.permission)
        }
    }

    fun stopVoice() {
        voiceAssistant.setContinuousMode(false)
        voiceAssistant.stopListening()
    }

    val visualState = when {
        uiState.isThinking -> UnifiedMayraVisualState.THINKING
        voiceState.isSpeaking -> UnifiedMayraVisualState.SPEAKING
        voiceState.isListening -> UnifiedMayraVisualState.LISTENING
        voiceState.transportState == VoiceTransportState.PROCESSING -> UnifiedMayraVisualState.UNDERSTANDING
        voiceState.transportState == VoiceTransportState.ERROR -> UnifiedMayraVisualState.ERROR
        !voiceState.speechAvailable -> UnifiedMayraVisualState.OFFLINE
        else -> UnifiedMayraVisualState.READY
    }

    val voiceLabel = when {
        voiceState.isSpeaking -> "Interrupt"
        voiceState.continuousMode -> "Stop"
        else -> "🎙"
    }

    MayraUnifiedChatSurface(
        userName = settings.normalizedName,
        messages = uiState.messages,
        input = uiState.input,
        visualState = visualState,
        partialTranscript = voiceState.partialTranscript.takeIf { voiceState.isListening }.orEmpty(),
        error = uiState.error ?: voiceState.error,
        sendEnabled = uiState.input.isNotBlank() && !uiState.isThinking,
        voiceButtonLabel = voiceLabel,
        onInputChange = chatViewModel::updateInput,
        onSend = chatViewModel::sendMessage,
        onVoice = {
            when {
                voiceState.isSpeaking -> voiceAssistant.interruptSpeech(resumeListening = true)
                voiceState.continuousMode -> stopVoice()
                else -> startVoice()
            }
        },
        onClear = {
            voiceLoopPolicy.reset()
            chatViewModel.clearConversation()
        },
        onOpenWorkspace = onOpenWorkspace,
        onOpenSettings = onOpenSettings,
        onOpenDevice = onOpenDevice,
        onOpenRuntime = onOpenRuntime,
        onQuickPrompt = chatViewModel::updateInput
    )
}
