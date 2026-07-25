package ai.mayra.app.memory

import ai.mayra.app.knowledge.MayraMemoryPrivacyGuard
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
import ai.mayra.app.ui.theme.MayraAITheme
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.util.Locale

class MayraVoiceNotesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { VoiceNotesScreen(onClose = ::finish) } }
    }
}

@Composable
private fun VoiceNotesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val memory = remember(context) { MayraPersonalMemory(context) }
    var title by remember { mutableStateOf("") }
    var transcript by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val saved = remember(refresh) {
        memory.notes().filter { it.type == PersonalNoteType.VOICE_TRANSCRIPT }
    }

    val recognizer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val heard = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                    .orEmpty()
                if (heard.isBlank()) {
                    notice = "No speech transcript was returned. Please try again."
                } else {
                    transcript = heard
                    if (title.isBlank()) title = heard.take(42).trim().ifBlank { "Voice note" }
                    notice = "Transcript ready. Review it before saving."
                }
            }
            Activity.RESULT_CANCELED -> notice = "Voice-note capture was cancelled. Nothing was saved."
            else -> notice = "Speech recognition did not return a usable transcript."
        }
    }

    fun startCapture() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your Mayra voice note")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        if (intent.resolveActivity(context.packageManager) == null) {
            notice = "Speech recognition is not available on this phone."
            return
        }
        runCatching { recognizer.launch(intent) }
            .onFailure { notice = "Speech recognition could not be opened."
            }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Voice Notes", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Mayra uses Android speech recognition, then waits for your review. Nothing is saved automatically.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = ::startCapture, modifier = Modifier.fillMaxWidth()) { Text("Record a voice note") }
                    OutlinedTextField(title, { title = it.take(180) }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(
                        transcript,
                        { transcript = it.take(8_000) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Review transcript") },
                        minLines = 5,
                        supportingText = { Text("${transcript.length}/8000 characters") }
                    )
                    Button(
                        enabled = title.isNotBlank() && transcript.isNotBlank(),
                        onClick = {
                            val cleanTitle = title.trim()
                            val cleanTranscript = transcript.trim()
                            val combined = "$cleanTitle $cleanTranscript"
                            if (MayraMemoryPrivacyGuard.looksSensitive(combined)) {
                                notice = "Not saved: passwords, OTPs, card-like numbers and secret keys are blocked."
                            } else {
                                memory.saveNote(
                                    PersonalNote(
                                        type = PersonalNoteType.VOICE_TRANSCRIPT,
                                        title = cleanTitle,
                                        body = cleanTranscript,
                                        tags = setOf("voice-note")
                                    )
                                )
                                title = ""
                                transcript = ""
                                refresh++
                                notice = "Voice note saved to Mayra Memory."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save reviewed voice note") }
                }
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Text("Saved voice notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (saved.isEmpty()) Text("No saved voice notes yet.")
            saved.forEach { note ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(note.title, fontWeight = FontWeight.SemiBold)
                        Text(note.body)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = {
                                memory.saveNote(note.copy(pinned = !note.pinned), addTimeline = false)
                                refresh++
                            }) { Text(if (note.pinned) "Unpin" else "Pin") }
                            TextButton(onClick = {
                                memory.archiveNote(note.id)
                                refresh++
                            }) { Text("Archive") }
                        }
                    }
                }
            }

            Text(
                "Do not dictate passwords, OTPs, payment-card details or secret keys. Voice recognition quality depends on the service installed on the phone.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
