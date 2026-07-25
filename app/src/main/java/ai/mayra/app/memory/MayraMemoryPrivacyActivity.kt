package ai.mayra.app.memory

import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

class MayraMemoryPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MemoryPrivacyScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MemoryPrivacyScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val memory = remember(context) { MayraPersonalMemory(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var confirmForget by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val diagnostics = remember(refresh) { memory.diagnostics() }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Memory privacy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("You control Mayra's owner-visible notes. Backup is owner-triggered, and destructive deletion always requires a second confirmation.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Stored locally", fontWeight = FontWeight.SemiBold)
                    Text("${diagnostics.notes} notes · ${diagnostics.timelineEvents} timeline events · ${diagnostics.sensitiveItems} sensitive-marked items")
                }
            }

            Button(
                onClick = { context.startActivity(Intent(context, MayraMemoryBackupActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create memory backup") }

            if (!confirmForget) {
                OutlinedButton(onClick = { confirmForget = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Forget all notes")
                }
            } else {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Delete all notes?", fontWeight = FontWeight.Bold)
                        Text("This removes active and archived notes, ideas, lists, checklists and voice transcripts. Separate timeline history remains until a dedicated audited timeline-control flow is implemented.")
                        Button(
                            onClick = {
                                val deleted = memory.notes(includeArchived = true).count { memory.deleteNote(it.id) }
                                confirmForget = false
                                refresh++
                                notice = "$deleted notes removed from Mayra memory."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Confirm delete notes") }
                        OutlinedButton(onClick = { confirmForget = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                    }
                }
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Current boundary", fontWeight = FontWeight.SemiBold)
                    Text("Normal memory is not a password vault. Credential-like content is rejected, and sensitive-marked entries are excluded from chat recall and normal briefing.", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
