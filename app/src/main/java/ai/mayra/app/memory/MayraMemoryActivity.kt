package ai.mayra.app.memory

import ai.mayra.app.document.MayraDocumentActivity
import ai.mayra.app.knowledge.ChecklistItem
import ai.mayra.app.knowledge.MayraMemoryPrivacyGuard
import ai.mayra.app.knowledge.MayraMemoryRecall
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MayraMemoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraMemoryScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MayraMemoryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val memory = remember(context) { MayraPersonalMemory(context) }
    val recall = remember(context) { MayraMemoryRecall(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PersonalNoteType.NOTE) }
    var pinned by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf("") }

    val notes = remember(refresh) { memory.notes() }
    val shown = remember(refresh, search) {
        if (search.isBlank()) notes else notes.filter { note ->
            listOf(note.title, note.body, note.tags.joinToString(" "), note.type.name)
                .any { it.contains(search, true) }
        }
    }
    val diagnostics = remember(refresh) { memory.diagnostics() }

    fun resetComposer() {
        title = ""
        body = ""
        tagsText = ""
        type = PersonalNoteType.NOTE
        pinned = false
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Memory", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Owner-visible personal memory stored on this phone. Passwords, OTPs, card-like numbers and secret keys are blocked from normal memory.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Capture, library & backup", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(context, MayraVoiceNotesActivity::class.java)) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Voice note") }
                        OutlinedButton(
                            onClick = { context.startActivity(Intent(context, MayraDocumentActivity::class.java)) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Documents") }
                    }
                    OutlinedButton(
                        onClick = { context.startActivity(Intent(context, MayraMemoryBackupActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Backup normal memory") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Memory health", fontWeight = FontWeight.SemiBold)
                    Text("${diagnostics.activeNotes} active · ${diagnostics.timelineEvents} timeline events · ${diagnostics.pinnedNotes} pinned")
                    Text("${diagnostics.checklistItems - diagnostics.completedChecklistItems} checklist items still open", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search memory") }, singleLine = true)

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Remember something", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(
                            PersonalNoteType.NOTE to "Note",
                            PersonalNoteType.IDEA to "Idea",
                            PersonalNoteType.SHOPPING_LIST to "Shopping",
                            PersonalNoteType.CHECKLIST to "Checklist"
                        ).forEach { (value, label) ->
                            FilterChip(selected = type == value, onClick = { type = value }, label = { Text(label) })
                        }
                    }
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(
                        body,
                        { body = it },
                        Modifier.fillMaxWidth(),
                        label = { Text(if (type == PersonalNoteType.CHECKLIST || type == PersonalNoteType.SHOPPING_LIST) "One item per line" else "Details") },
                        minLines = 3
                    )
                    OutlinedTextField(tagsText, { tagsText = it }, Modifier.fillMaxWidth(), label = { Text("Tags, separated by commas") }, singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = pinned, onCheckedChange = { pinned = it })
                        Text("Pin this memory")
                    }
                    Button(
                        enabled = title.isNotBlank() && body.isNotBlank(),
                        onClick = {
                            runCatching {
                                require(!MayraMemoryPrivacyGuard.looksSensitive("$title $body $tagsText"))
                                val tags = tagsText.split(',').map(String::trim).filter(String::isNotBlank).toSet()
                                val checklist = if (type == PersonalNoteType.CHECKLIST || type == PersonalNoteType.SHOPPING_LIST) {
                                    body.lineSequence().map(String::trim).filter(String::isNotBlank).map { ChecklistItem(text = it) }.toList()
                                } else emptyList()
                                memory.saveNote(
                                    PersonalNote(
                                        type = type,
                                        title = title.trim(),
                                        body = if (checklist.isEmpty()) body.trim() else "",
                                        tags = tags,
                                        checklist = checklist,
                                        pinned = pinned,
                                        sensitive = false
                                    )
                                )
                            }.onSuccess {
                                resetComposer(); refresh++; notice = "Saved to Mayra memory."
                            }.onFailure {
                                notice = "Not saved: sensitive credentials and financial secrets are blocked."
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save memory") }
                }
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Text("Saved memories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            shown.forEach { note ->
                MemoryCard(
                    note = note,
                    onTogglePin = { memory.saveNote(note.copy(pinned = !note.pinned), addTimeline = false); refresh++ },
                    onToggleItem = { itemId, completed -> memory.toggleChecklist(note.id, itemId, completed); refresh++ },
                    onArchive = { memory.archiveNote(note.id); refresh++ }
                )
            }
            if (shown.isEmpty()) Text("No matching memories yet.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Recall preview", fontWeight = FontWeight.SemiBold)
                    Text("Preview what Mayra may recall for the current search. Sensitive memories are excluded.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { preview = recall.promptContext(search.ifBlank { "recent personal context" }) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Preview recalled context")
                    }
                    if (preview.isNotBlank()) Text(preview, style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

@Composable
private fun MemoryCard(
    note: PersonalNote,
    onTogglePin: () -> Unit,
    onToggleItem: (String, Boolean) -> Unit,
    onArchive: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(note.title, fontWeight = FontWeight.SemiBold)
                    Text(note.type.name.replace('_', ' ').lowercase(), style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onTogglePin) { Text(if (note.pinned) "Unpin" else "Pin") }
            }
            if (note.body.isNotBlank()) Text(note.body)
            note.checklist.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.completed, onCheckedChange = { onToggleItem(item.id, it) })
                    Text(item.text, modifier = Modifier.weight(1f))
                }
            }
            if (note.tags.isNotEmpty()) Text(note.tags.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onArchive) { Text("Archive") }
            }
        }
    }
}
