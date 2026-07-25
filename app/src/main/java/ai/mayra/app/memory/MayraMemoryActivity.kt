package ai.mayra.app.memory

import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MayraMemoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MemoryAndNotesScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MemoryAndNotesScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { MayraPersonalDatabase.get(context) }
    val engine = remember(context) { MayraMemoryEngine(context) }
    var memories by remember { mutableStateOf(emptyList<MayraMemoryEntity>()) }
    var notes by remember { mutableStateOf(emptyList<MayraNoteEntity>()) }
    var query by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(MayraMemoryKind.FACT) }
    var kindMenu by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("Memory") }
    var notice by remember { mutableStateOf<String?>(null) }
    var promptPreview by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        launch { database.memoryDao().observeActive().collectLatest { memories = it } }
        launch { database.noteDao().observeActive().collectLatest { notes = it } }
    }

    val shownMemories = memories.filter {
        query.isBlank() || listOf(it.title, it.text, it.tags, it.kind.name).any { value -> value.contains(query, true) }
    }
    val shownNotes = notes.filter {
        query.isBlank() || listOf(it.title, it.body, it.category).any { value -> value.contains(query, true) }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Memory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Everything here stays on this phone. Mayra rejects passwords, OTPs, card-like numbers and secret keys.")

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { mode = "Memory" }, modifier = Modifier.weight(1f)) { Text("Memories (${memories.size})") }
                OutlinedButton(onClick = { mode = "Note" }, modifier = Modifier.weight(1f)) { Text("Notes (${notes.size})") }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search memory and notes") },
                singleLine = true
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (mode == "Memory") "Remember something" else "Create a note", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text(if (mode == "Memory") "What should Mayra remember?" else "Note") }, minLines = 3)
                    if (mode == "Memory") {
                        Column {
                            OutlinedButton(onClick = { kindMenu = true }, modifier = Modifier.fillMaxWidth()) { Text("Type: ${kind.name.lowercase().replaceFirstChar(Char::uppercase)}") }
                            DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                                MayraMemoryKind.entries.filterNot { it == MayraMemoryKind.NOTE }.forEach { option ->
                                    DropdownMenuItem(text = { Text(option.name.lowercase().replaceFirstChar(Char::uppercase)) }, onClick = { kind = option; kindMenu = false })
                                }
                            }
                        }
                    }
                    Button(
                        enabled = title.isNotBlank() && body.isNotBlank(),
                        onClick = {
                            scope.launch {
                                runCatching {
                                    if (mode == "Memory") {
                                        engine.remember(kind, title, body)
                                    } else {
                                        require(!SensitiveMemoryGuard.looksSensitive("$title $body"))
                                        database.noteDao().upsert(MayraNoteEntity(title = title.trim(), body = body.trim()))
                                    }
                                }.onSuccess {
                                    notice = if (mode == "Memory") "Saved to Mayra memory." else "Note saved."
                                    title = ""; body = ""
                                }.onFailure { notice = "Not saved: ${it.message ?: "sensitive or invalid content"}" }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save") }
                }
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (mode == "Memory") {
                Text("Saved memories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                shownMemories.forEach { memory ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(memory.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(memory.kind.name.lowercase())
                            }
                            Text(memory.text)
                            if (memory.tags.isNotBlank()) Text(memory.tags, style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { engine.archive(memory.id) } }) { Text("Archive") }
                            }
                        }
                    }
                }
                if (shownMemories.isEmpty()) Text("No matching memories yet.")
            } else {
                Text("Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                shownNotes.forEach { note ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(note.title, fontWeight = FontWeight.SemiBold)
                            Text(note.body)
                            Text(note.category, style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { scope.launch { database.noteDao().archive(note.id) } }) { Text("Archive") }
                            }
                        }
                    }
                }
                if (shownNotes.isEmpty()) Text("No matching notes yet.")
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Memory context preview", fontWeight = FontWeight.SemiBold)
                    Text("See what Mayra would recall for the current search. This preview never sends anything online by itself.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { scope.launch { promptPreview = engine.contextForPrompt(query.ifBlank { "recent personal context" }) } },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Preview recalled context") }
                    if (promptPreview.isNotBlank()) Text(promptPreview, style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
