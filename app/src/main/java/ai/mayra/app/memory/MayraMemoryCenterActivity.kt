package ai.mayra.app.memory

import ai.mayra.app.MayraRuntime
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import java.time.Instant
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
        var query by remember { mutableStateOf("") }
        var category by remember { mutableStateOf<MayraMemoryCategory?>(null) }
        var deleteId by remember { mutableStateOf<String?>(null) }
        var editMemory by remember { mutableStateOf<MayraPersonalMemory?>(null) }
        var expiryMemory by remember { mutableStateOf<MayraPersonalMemory?>(null) }
        var clearAll by remember { mutableStateOf(false) }
        var migrationMessage by remember { mutableStateOf<String?>(null) }
        val allMemories = remember(refresh) { MayraRuntime.personalMemory.activeMemories() }
        val pending = remember(refresh) { MayraRuntime.personalMemory.pendingProposals() }
        val health = remember(refresh) { AndroidMayraMemoryStorageHealthReader(this).read() }
        val memories = allMemories.filter { memory ->
            (category == null || memory.category == category) &&
                (query.isBlank() || memory.key.contains(query, true) || memory.value.contains(query, true))
        }

        Scaffold { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Memory Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Only memories you explicitly approved are stored locally and protected on this device.")
                StorageHealthCard(health, onRetry = {
                    migrationMessage = runCatching {
                        MayraRuntime.personalMemoryStore.all()
                        MayraRuntime.personalMemory.pendingProposals()
                        refresh++
                        val after = AndroidMayraMemoryStorageHealthReader(this).read()
                        when (after.state) {
                            MayraMemoryStorageState.HEALTHY -> "Protected storage is healthy."
                            MayraMemoryStorageState.EMPTY -> "There are no stored memory records."
                            MayraMemoryStorageState.MIGRATION_NEEDED -> "Some legacy records still need migration. No records were deleted."
                            MayraMemoryStorageState.DEGRADED -> "Some records remain unreadable. Mayra did not reset keys or delete data."
                        }
                    }.getOrElse { "Migration retry failed safely. Existing records were not cleared." }
                })
                migrationMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search memories") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All") })
                    MayraMemoryCategory.entries.forEach { item ->
                        FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item.name.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { share(MayraRuntime.personalMemoryStore.exportText()) }, modifier = Modifier.weight(1f)) { Text("Export") }
                    OutlinedButton(onClick = { clearAll = true }, enabled = allMemories.isNotEmpty() || pending.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Clear all") }
                }

                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pending.isNotEmpty()) {
                        item { Text("Pending approval (${pending.size})", fontWeight = FontWeight.Bold) }
                        items(pending, key = { "pending-${it.id}" }) { proposal ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(proposal.candidate.key, fontWeight = FontWeight.Bold)
                                    Text("Proposed: ${proposal.candidate.value}")
                                    proposal.conflictingMemoryId?.let { id -> allMemories.firstOrNull { it.id == id }?.let { Text("Current: ${it.value}") } }
                                    proposal.candidate.expiresAt?.let { Text("Will expire ${format(it)}") }
                                    Text("Requested ${format(proposal.createdAt)}", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = { MayraRuntime.personalMemory.approve(proposal.id); refresh++ }) { Text(if (proposal.conflictingMemoryId == null) "Save" else "Replace") }
                                        TextButton(onClick = { MayraRuntime.personalMemory.reject(proposal.id); refresh++ }) { Text("Not now") }
                                    }
                                }
                            }
                        }
                    }
                    if (memories.isEmpty()) {
                        item { Text(if (allMemories.isEmpty()) "No approved memories yet." else "No memories match this filter.") }
                    } else {
                        item { Text("Approved memories (${memories.size})", fontWeight = FontWeight.Bold) }
                        items(memories, key = { it.id }) { memory ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(memory.key, fontWeight = FontWeight.Bold)
                                    Text(memory.value)
                                    Text("Category: ${memory.category.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                                    Text("Source: ${memory.provenance.sourceType} · ${memory.provenance.sourceReference}", style = MaterialTheme.typography.bodySmall)
                                    Text("Revision ${memory.revision} · updated ${format(memory.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                                    Text(memory.expiresAt?.let { "Expires ${format(it)}" } ?: "No expiry", style = MaterialTheme.typography.bodySmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { editMemory = memory }) { Text("Edit") }
                                        TextButton(onClick = { expiryMemory = memory }) { Text("Expiry") }
                                        TextButton(onClick = { deleteId = memory.id }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        editMemory?.let { memory ->
            var value by remember(memory.id) { mutableStateOf(memory.value) }
            AlertDialog(onDismissRequest = { editMemory = null }, title = { Text("Edit ${memory.key}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Value") }); Text("Sensitive or prohibited replacements are rejected.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(enabled = value.isNotBlank() && value.trim() != memory.value, onClick = { MayraRuntime.personalMemory.update(memory.id, value, MayraMemoryProvenance("memory-center", "owner-edit", Instant.now())); editMemory = null; refresh++ }) { Text("Save changes") } }, dismissButton = { TextButton(onClick = { editMemory = null }) { Text("Cancel") } })
        }
        expiryMemory?.let { memory ->
            AlertDialog(onDismissRequest = { expiryMemory = null }, title = { Text("Set expiry for ${memory.key}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Current: ${memory.expiresAt?.let(::format) ?: "No expiry"}"); Text("Choose how long Mayra may keep using this memory.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Column { listOf("1 day" to 86_400L, "7 days" to 604_800L, "30 days" to 2_592_000L).forEach { (label, seconds) -> TextButton(onClick = { replaceExpiry(memory, Instant.now().plusSeconds(seconds)); expiryMemory = null; refresh++ }) { Text(label) } } } }, dismissButton = { Column { TextButton(onClick = { replaceExpiry(memory, null); expiryMemory = null; refresh++ }) { Text("Never expire") }; TextButton(onClick = { expiryMemory = null }) { Text("Cancel") } } })
        }
        deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete this memory?") }, text = { Text("Mayra will stop using it immediately.") }, confirmButton = { Button(onClick = { MayraRuntime.personalMemory.delete(id); deleteId = null; refresh++ }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }) }
        if (clearAll) AlertDialog(onDismissRequest = { clearAll = false }, title = { Text("Clear all memories and proposals?") }, text = { Text("This removes every approved memory and pending proposal from this device.") }, confirmButton = { Button(onClick = { MayraRuntime.personalMemory.clear(); clearAll = false; refresh++ }) { Text("Clear all") } }, dismissButton = { TextButton(onClick = { clearAll = false }) { Text("Cancel") } })
    }

    @Composable
    private fun StorageHealthCard(health: MayraMemoryStorageHealth, onRetry: () -> Unit) {
        val title = when (health.state) {
            MayraMemoryStorageState.HEALTHY -> "Protected storage healthy"
            MayraMemoryStorageState.EMPTY -> "Memory storage empty"
            MayraMemoryStorageState.MIGRATION_NEEDED -> "Legacy records need migration"
            MayraMemoryStorageState.DEGRADED -> "Memory storage needs attention"
        }
        val explanation = when (health.state) {
            MayraMemoryStorageState.HEALTHY -> "Approved and pending records are readable in protected form."
            MayraMemoryStorageState.EMPTY -> "There are no approved memories or pending proposals on this device."
            MayraMemoryStorageState.MIGRATION_NEEDED -> "Valid older records remain readable and can be rewritten into protected form."
            MayraMemoryStorageState.DEGRADED -> "Some records cannot currently be decrypted or decoded. Mayra will not reset keys or delete them automatically."
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(explanation, style = MaterialTheme.typography.bodySmall)
                Text("Approved: ${health.approvedProtected} protected · ${health.approvedLegacy} legacy · ${health.approvedUnreadable} unreadable", style = MaterialTheme.typography.bodySmall)
                Text("Pending: ${health.pendingProtected} protected · ${health.pendingLegacy} legacy · ${health.pendingUnreadable} unreadable", style = MaterialTheme.typography.bodySmall)
                if (health.state == MayraMemoryStorageState.MIGRATION_NEEDED || health.state == MayraMemoryStorageState.DEGRADED) {
                    OutlinedButton(onClick = onRetry) { Text("Retry safe migration") }
                }
            }
        }
    }

    private fun replaceExpiry(memory: MayraPersonalMemory, expiresAt: Instant?) {
        val proposal = MayraRuntime.personalMemory.propose(MayraMemoryCandidate(memory.key, memory.value, memory.category, MayraMemoryProvenance("memory-center", "owner-expiry", Instant.now()), expiresAt))
        if (proposal is MayraMemoryProposalResult.ApprovalRequired) MayraRuntime.personalMemory.approve(proposal.proposalId)
    }
    private fun share(text: String) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Mayra personal memories"); putExtra(Intent.EXTRA_TEXT, text) }, "Export memories")) }
    private fun format(instant: Instant): String = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(instant)
}
