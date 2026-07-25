package ai.mayra.app.workspace

import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MayraWorkspaceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                MayraWorkspaceScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
private fun MayraWorkspaceScreen(
    onClose: () -> Unit,
    viewModel: MayraWorkspaceViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val session = state.session
    val active = session.tasks.firstOrNull { it.id == session.activeTaskId }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Mayra Workspace", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            state.isSaving -> "Encrypted autosave…"
                            state.lastSavedAt > 0L -> "Encrypted autosave ready"
                            else -> "Private local session"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onClose) { Text("Close") }
            }

            active?.let { ActiveTaskCard(it) }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (session.tasks.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Start a structured task", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Try: ‘XYZ ka bill dekho’, ‘Naam, saman, quantity aur rate ka table banao’, or ‘Isko PDF mein export karo’. Unsupported tools stay visibly pending instead of pretending completion.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                session.table?.let { table -> item { WorkspaceTablePreview(table) } }

                if (session.transcript.isNotEmpty()) {
                    item {
                        Text("Task history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                items(session.tasks.reversed(), key = { it.id }) { task -> WorkspaceTaskCard(task) }
            }

            OutlinedTextField(
                value = session.notes,
                onValueChange = viewModel::updateNotes,
                modifier = Modifier.fillMaxWidth().height(92.dp),
                label = { Text("Workspace notes") },
                placeholder = { Text("Mayra can organise task notes here.") }
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = viewModel::pauseActiveTask, modifier = Modifier.weight(1f), enabled = active != null) {
                    Text("Pause")
                }
                OutlinedButton(onClick = viewModel::continueActiveTask, modifier = Modifier.weight(1f), enabled = active != null) {
                    Text("Continue")
                }
                OutlinedButton(onClick = viewModel::cancelActiveTask, modifier = Modifier.weight(1f), enabled = active != null) {
                    Text("Cancel")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = viewModel::updateInput,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tell Mayra what to work on…") },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.submitInput() })
                )
                Button(
                    onClick = viewModel::submitInput,
                    enabled = state.input.isNotBlank(),
                    shape = RoundedCornerShape(18.dp)
                ) { Text("Add task") }
            }

            TextButton(onClick = viewModel::clearSession, modifier = Modifier.align(Alignment.End)) {
                Text("Clear workspace")
            }
        }
    }
}

@Composable
private fun ActiveTaskCard(task: MayraWorkspaceTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Current task · ${task.intent.action.name.replace('_', ' ')}", fontWeight = FontWeight.Bold)
            Text(task.statusMessage)
            Text("${task.progress}% · ${task.state.name.replace('_', ' ')}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun WorkspaceTaskCard(task: MayraWorkspaceTask) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(task.intent.rawText, fontWeight = FontWeight.SemiBold)
            Text(task.intent.action.name.replace('_', ' '), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(task.statusMessage, style = MaterialTheme.typography.bodySmall)
            if (task.intent.requiresConfirmation) {
                Text("Confirmation required before external action.", style = MaterialTheme.typography.bodySmall)
            }
            task.sources.forEach { source ->
                Text(
                    "Source: ${source.displayName}${source.page?.let { ", page $it" }.orEmpty()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun WorkspaceTablePreview(table: MayraWorkspaceTable) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(table.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Revision ${table.revision}", style = MaterialTheme.typography.bodySmall)
            HorizontalDivider()
            Text(table.columns.joinToString("  |  ").ifBlank { "No columns yet" }, fontWeight = FontWeight.SemiBold)
            table.rows.take(5).forEach { row -> Text(row.joinToString("  |  ")) }
            if (table.rows.size > 5) Text("+ ${table.rows.size - 5} more rows", style = MaterialTheme.typography.bodySmall)
        }
    }
}
