package ai.mayra.app.memory

import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MayraMemoryBackupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MemoryBackupScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MemoryBackupScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val memory = remember(context) { MayraPersonalMemory(context) }
    var notice by remember { mutableStateOf<String?>(null) }
    val exportText = remember { MayraMemoryBackupFormatter.format(memory) }
    val filename = remember {
        "mayra-memory-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.txt"
    }

    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(exportText) }
                    ?: error("No writable stream")
            }.onSuccess {
                notice = "Memory backup saved. Sensitive-marked items were excluded."
            }.onFailure {
                notice = "Mayra could not save this backup."
            }
        }
    }

    val diagnostics = remember { memory.diagnostics() }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Memory Backup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Create an owner-controlled plain-text backup of normal Mayra notes, checklists and timeline entries.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Export scope", fontWeight = FontWeight.SemiBold)
                    Text("${diagnostics.activeNotes} active notes · ${diagnostics.timelineEvents} timeline events")
                    Text("Sensitive-marked items and credentials rejected by Memory V2 are not included.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(onClick = { createDocument.launch(filename) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save memory backup")
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Privacy note", fontWeight = FontWeight.SemiBold)
                    Text(
                        "The exported file is outside Mayra after you choose its location. Protect it using your phone storage, cloud-drive or device-lock settings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

object MayraMemoryBackupFormatter {
    fun format(memory: MayraPersonalMemory, now: Long = System.currentTimeMillis()): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))
        val notes = memory.notes(includeArchived = true).filterNot { it.sensitive }
        val events = memory.timeline(includeSensitive = false, limit = 1_500)
        return buildString {
            appendLine("Mayra Memory Backup")
            appendLine("Generated: $date")
            appendLine("Sensitive-marked items excluded: yes")
            appendLine()
            appendLine("NOTES")
            notes.forEach { note ->
                appendLine("- [${note.type.name}] ${note.title}")
                if (note.body.isNotBlank()) appendLine("  ${note.body.replace('\n', ' ')}")
                if (note.tags.isNotEmpty()) appendLine("  Tags: ${note.tags.joinToString(", ")}")
                note.checklist.forEach { item -> appendLine("  [${if (item.completed) "x" else " "}] ${item.text}") }
                if (note.pinned) appendLine("  Pinned: yes")
                if (note.archivedAt != null) appendLine("  Archived: yes")
            }
            appendLine()
            appendLine("TIMELINE")
            events.forEach { event ->
                appendLine("- ${event.title}")
                if (event.description.isNotBlank()) appendLine("  ${event.description.replace('\n', ' ')}")
            }
        }.take(1_000_000)
    }
}
