package ai.mayra.app.memory

import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var pendingEncrypted by remember { mutableStateOf<String?>(null) }
    var importedPayload by remember { mutableStateOf<MayraMemoryBackupEngine.BackupPayload?>(null) }
    var importPreview by remember { mutableStateOf<MayraMemoryBackupEngine.ImportPreview?>(null) }
    var refresh by remember { mutableStateOf(0) }
    val diagnostics = remember(refresh) { memory.diagnostics() }
    val filename = remember { "mayra-memory-${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.mayrabackup" }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val encrypted = pendingEncrypted
        pendingEncrypted = null
        if (uri != null && encrypted != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(encrypted) }
                    ?: error("No writable stream")
            }.onSuccess {
                notice = "Encrypted Mayra backup saved. Keep the password separately; Mayra cannot recover it."
            }.onFailure {
                notice = "Mayra could not save this encrypted backup."
            }
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importedPayload = null
        importPreview = null
        if (uri != null) {
            runCatching {
                require(password.isNotBlank()) { "Enter the backup password before selecting a file" }
                val envelope = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("No readable stream")
                MayraMemoryBackupEngine.decrypt(envelope, password.toCharArray())
            }.onSuccess { payload ->
                importedPayload = payload
                importPreview = MayraMemoryBackupEngine.preview(payload, memory)
                password = ""
                confirmation = ""
                notice = "Backup decrypted. Review the counts below before restoring."
            }.onFailure { error ->
                notice = error.message ?: "Mayra could not open this backup."
            }
        }
    }

    fun createEncryptedBackup() {
        runCatching {
            require(password == confirmation) { "Backup passwords do not match" }
            val payload = MayraMemoryBackupEngine.export(memory)
            MayraMemoryBackupEngine.encrypt(payload, password.toCharArray())
        }.onSuccess { encrypted ->
            pendingEncrypted = encrypted
            password = ""
            confirmation = ""
            createDocument.launch(filename)
        }.onFailure { error ->
            notice = error.message ?: "Mayra could not prepare this backup."
        }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Encrypted Memory Backup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Create a portable owner-controlled Mayra backup protected with AES-256-GCM and a password-derived key.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Protected scope", fontWeight = FontWeight.SemiBold)
                    Text("${diagnostics.activeNotes} active notes · ${diagnostics.timelineEvents} timeline events")
                    Text("Sensitive-marked items, credentials and secure references are excluded before encryption.", style = MaterialTheme.typography.bodySmall)
                }
            }

            OutlinedTextField(
                value = password,
                onValueChange = { password = it.take(128) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Backup password") },
                supportingText = { Text("Minimum 8 characters with at least one letter and one number") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            OutlinedTextField(
                value = confirmation,
                onValueChange = { confirmation = it.take(128) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Confirm password for export") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )

            Button(onClick = ::createEncryptedBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Save encrypted Mayra backup")
            }
            OutlinedButton(
                onClick = {
                    if (password.isBlank()) notice = "Enter the backup password first."
                    else openDocument.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open encrypted backup for review") }

            importPreview?.let { preview ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Restore preview", fontWeight = FontWeight.SemiBold)
                        Text("Backup created: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(preview.generatedAt))}")
                        Text("Notes: ${preview.notesInBackup} total · ${preview.newNotes} new · ${preview.duplicateNotes} already present")
                        Text("Timeline: ${preview.eventsInBackup} total · ${preview.newEvents} new · ${preview.duplicateEvents} already present")
                        Text("Restore is additive. Existing IDs are skipped; nothing is deleted or silently overwritten.", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = {
                                val payload = importedPayload ?: return@Button
                                val result = MayraMemoryBackupEngine.restore(payload, memory)
                                refresh++
                                importedPayload = null
                                importPreview = null
                                notice = "Restore complete: ${result.notesAdded} notes and ${result.eventsAdded} timeline events added; ${result.notesSkipped + result.eventsSkipped} duplicates skipped."
                            },
                            enabled = preview.newNotes + preview.newEvents > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Confirm additive restore") }
                        OutlinedButton(
                            onClick = {
                                importedPayload = null
                                importPreview = null
                                notice = "Restore cancelled. No Mayra memory was changed."
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cancel restore") }
                    }
                }
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Recovery rules", fontWeight = FontWeight.SemiBold)
                    Text("Mayra never stores or uploads the backup password. A wrong password or modified file is rejected by authenticated encryption.", style = MaterialTheme.typography.bodySmall)
                    Text("Keep one tested backup and its password in separate safe locations. Automatic Android app backup is disabled for Mayra.", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, MayraMemoryPrivacyActivity::class.java)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Privacy & forget controls") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

/** Human-readable diagnostic formatter retained for tests and on-device review; it is never exported unencrypted. */
object MayraMemoryBackupFormatter {
    fun format(memory: MayraPersonalMemory, now: Long = System.currentTimeMillis()): String {
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(now))
        val notes = memory.notes(includeArchived = true).filterNot { it.sensitive }
        val events = memory.timeline(includeSensitive = false, limit = 1_500)
        return buildString {
            appendLine("Mayra Memory Backup Preview")
            appendLine("Generated: $date")
            appendLine("Sensitive-marked items excluded: yes")
            appendLine()
            appendLine("NOTES")
            notes.forEach { note ->
                appendLine("- [${note.type.name}] ${note.title}")
                if (note.body.isNotBlank()) appendLine("  ${note.body.replace('\n', ' ')}")
                if (note.tags.isNotEmpty()) appendLine("  Tags: ${note.tags.joinToString(", ")}")
                note.checklist.forEach { item -> appendLine("  [${if (item.completed) "x" else " "}] ${item.text}") }
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
