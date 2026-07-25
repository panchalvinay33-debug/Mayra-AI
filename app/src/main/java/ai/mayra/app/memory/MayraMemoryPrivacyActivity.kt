package ai.mayra.app.memory

import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
    var confirmForget by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val diagnostics = memory.diagnostics()

    ScaffoldBody {
        Text("Memory privacy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("You control Mayra's owner-visible notes. Exports exclude entries marked sensitive. Timeline history is not silently deleted by the note-clear action.")

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Stored locally", fontWeight = FontWeight.SemiBold)
                Text("${diagnostics.notes} notes · ${diagnostics.timelineEvents} timeline events · ${diagnostics.sensitiveItems} sensitive-marked items")
            }
        }

        Button(
            onClick = {
                val export = MayraMemoryExport.build(memory.notes(includeArchived = true))
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Mayra Memory Export")
                    putExtra(Intent.EXTRA_TEXT, export)
                }
                runCatching { context.startActivity(Intent.createChooser(share, "Export Mayra memory")) }
                    .onFailure { notice = "No compatible app is available for export." }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Export non-sensitive notes") }

        if (!confirmForget) {
            OutlinedButton(onClick = { confirmForget = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Forget all notes")
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Delete all notes?", fontWeight = FontWeight.Bold)
                    Text("This removes active and archived notes, ideas, lists, checklists and voice transcripts. It does not erase separate timeline history.")
                    Button(
                        onClick = {
                            val deleted = memory.notes(includeArchived = true).count { memory.deleteNote(it.id) }
                            confirmForget = false
                            notice = "$deleted notes removed from Mayra memory."
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Confirm delete notes") }
                    OutlinedButton(onClick = { confirmForget = false }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }

        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
private fun ScaffoldBody(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

object MayraMemoryExport {
    fun build(notes: List<PersonalNote>, now: Long = System.currentTimeMillis()): String {
        val visible = notes.filterNot { it.sensitive }
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))
        return buildString {
            appendLine("Mayra Memory Export")
            appendLine("Generated: $stamp")
            appendLine("Sensitive-marked entries excluded")
            appendLine()
            if (visible.isEmpty()) appendLine("No exportable notes.")
            visible.sortedByDescending { it.updatedAt }.forEach { note ->
                appendLine("# ${note.title}")
                appendLine("Type: ${note.type.name}")
                if (note.tags.isNotEmpty()) appendLine("Tags: ${note.tags.sorted().joinToString(", ")}")
                if (note.body.isNotBlank()) appendLine(note.body)
                note.checklist.forEach { item -> appendLine("${if (item.completed) "[x]" else "[ ]"} ${item.text}") }
                appendLine()
            }
        }.take(200_000)
    }
}
