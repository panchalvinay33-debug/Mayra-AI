package ai.mayra.app.memory

import ai.mayra.app.MayraRuntime
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MayraMemoryCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MemoryCenter() } }
    }

    @Composable
    private fun MemoryCenter() {
        var refresh by remember { mutableStateOf(0) }
        var deleteId by remember { mutableStateOf<String?>(null) }
        var clearAll by remember { mutableStateOf(false) }
        val memories = remember(refresh) { MayraRuntime.personalMemory.activeMemories() }

        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Memory Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Only memories you explicitly approved are stored locally.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { share(MayraRuntime.personalMemoryStore.exportText()) }, modifier = Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton(onClick = { clearAll = true }, enabled = memories.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Clear all") }
                }
                if (memories.isEmpty()) Text("No approved memories yet.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(memories, key = { it.id }) { memory ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(memory.key, fontWeight = FontWeight.Bold)
                                Text(memory.value)
                                Text("Category: ${memory.category.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                                Text("Source: ${memory.provenance.sourceType} · ${memory.provenance.sourceReference}", style = MaterialTheme.typography.bodySmall)
                                Text("Revision ${memory.revision} · updated ${format(memory.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                                memory.expiresAt?.let { Text("Expires ${format(it)}", style = MaterialTheme.typography.bodySmall) }
                                TextButton(onClick = { deleteId = memory.id }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }

        deleteId?.let { id ->
            AlertDialog(
                onDismissRequest = { deleteId = null },
                title = { Text("Delete this memory?") },
                text = { Text("Mayra will stop using it immediately.") },
                confirmButton = { Button(onClick = { MayraRuntime.personalMemory.delete(id); deleteId = null; refresh++ }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }
            )
        }
        if (clearAll) AlertDialog(
            onDismissRequest = { clearAll = false },
            title = { Text("Clear all memories?") },
            text = { Text("This removes every approved personal memory from this device.") },
            confirmButton = { Button(onClick = { MayraRuntime.personalMemory.clear(); clearAll = false; refresh++ }) { Text("Clear all") } },
            dismissButton = { TextButton(onClick = { clearAll = false }) { Text("Cancel") } }
        )
    }

    private fun share(text: String) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mayra personal memories")
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Export memories"))
    }

    private fun format(instant: java.time.Instant): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(instant)
}
