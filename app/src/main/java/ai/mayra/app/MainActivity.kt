package ai.mayra.app

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.mayra.app.chat.ChatViewModel
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.ui.theme.MayraAITheme
import ai.mayra.app.voice.AndroidVoiceAssistant
import ai.mayra.app.voice.AndroidVoiceState
import ai.mayra.app.voice.MicrophonePermission

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraHome() } }
    }
}

@Composable
private fun MayraHome(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var androidVoiceState by remember { mutableStateOf(AndroidVoiceState()) }
    var lastSpokenMessageCount by remember { mutableIntStateOf(0) }

    val voiceAssistant = remember {
        AndroidVoiceAssistant(context) { newState ->
            androidVoiceState = newState
            when {
                newState.isSpeaking -> viewModel.onSpeechStarted()
                !newState.isSpeaking -> viewModel.onSpeechFinished()
            }
            newState.error?.let(viewModel::onVoiceError)
        }
    }

    LaunchedEffect(androidVoiceState.transcript, androidVoiceState.isFinalTranscript) {
        val transcript = androidVoiceState.transcript.trim()
        if (transcript.isBlank()) return@LaunchedEffect

        if (androidVoiceState.isFinalTranscript) {
            viewModel.onVoiceTranscript(transcript)
        } else {
            viewModel.updateInput(transcript)
        }
    }

    LaunchedEffect(state.messages.size) {
        val latest = state.messages.lastOrNull()
        if (
            state.messages.size > lastSpokenMessageCount &&
            latest?.sender == MayraMessage.Sender.MAYRA
        ) {
            lastSpokenMessageCount = state.messages.size
            voiceAssistant.speak(latest.text)
        }
    }

    DisposableEffect(Unit) {
        onDispose { voiceAssistant.release() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceListening()
            voiceAssistant.startListening()
        } else {
            viewModel.onVoiceError("Microphone permission is required for voice input.")
        }
    }

    fun startVoice() {
        if (MicrophonePermission.isGranted(context)) {
            viewModel.startVoiceListening()
            voiceAssistant.startListening()
        } else {
            permissionLauncher.launch(MicrophonePermission.permission)
        }
    }

    fun stopVoice() {
        voiceAssistant.stopListening()
        viewModel.stopVoiceListening()
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    tonalElevation = 6.dp,
                    modifier = Modifier.size(58.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "M",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Mayra AI",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            state.isThinking -> "Thinking…"
                            androidVoiceState.isSpeaking -> "Speaking…"
                            androidVoiceState.isListening -> "Listening…"
                            else -> "● Ready to help"
                        }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        Text(
                            "Namaste. I’m Mayra. What can I help you with today?",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                items(state.messages) { message ->
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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::updateInput,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask Mayra anything…") },
                enabled = !state.isThinking,
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (androidVoiceState.isListening) stopVoice() else startVoice()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Text(if (androidVoiceState.isListening) "Stop" else "🎙 Voice")
                }
                Button(
                    onClick = viewModel::sendMessage,
                    enabled = state.input.isNotBlank() && !state.isThinking,
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp)
                ) {
                    Text(if (state.isThinking) "Thinking…" else "Send to Mayra")
                }
            }
        }
    }
}
