package ai.mayra.app.presence

import ai.mayra.app.core.MayraMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

enum class UnifiedMayraVisualState {
    READY,
    LISTENING,
    UNDERSTANDING,
    THINKING,
    SPEAKING,
    OFFLINE,
    ERROR
}

@Composable
fun MayraUnifiedChatSurface(
    userName: String,
    messages: List<MayraMessage>,
    input: String,
    visualState: UnifiedMayraVisualState,
    partialTranscript: String,
    error: String?,
    sendEnabled: Boolean,
    voiceButtonLabel: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    onClear: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenRuntime: () -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    val listState = rememberLazyListState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold { scaffoldPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UnifiedHeader(
                    expanded = menuExpanded,
                    hasMessages = messages.isNotEmpty(),
                    onExpand = { menuExpanded = true },
                    onDismiss = { menuExpanded = false },
                    onClear = onClear,
                    onOpenWorkspace = onOpenWorkspace,
                    onOpenFiles = onOpenFiles,
                    onOpenSettings = onOpenSettings,
                    onOpenDevice = onOpenDevice,
                    onOpenRuntime = onOpenRuntime
                )

                CompactMayraPresence(userName, visualState)

                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) item { EmptyConversationPrompt(userName) }
                    items(messages, key = { it.timestamp }) { message -> UnifiedMessageBubble(message) }
                }

                if (partialTranscript.isNotBlank()) {
                    Text(
                        "Heard: $partialTranscript",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                error?.takeIf(String::isNotBlank)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                QuickPromptRow(onQuickPrompt)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = onInputChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Talk to Mayra…") },
                        enabled = sendEnabled || input.isBlank(),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { if (sendEnabled) onSend() })
                    )
                    OutlinedButton(
                        onClick = onVoice,
                        shape = CircleShape,
                        modifier = Modifier.height(56.dp)
                    ) { Text(voiceButtonLabel, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = onSend,
                        enabled = sendEnabled,
                        shape = CircleShape,
                        modifier = Modifier.height(56.dp)
                    ) { Text("➤") }
                }
            }
        }
    }
}

@Composable
private fun UnifiedHeader(
    expanded: Boolean,
    hasMessages: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onOpenWorkspace: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDevice: () -> Unit,
    onOpenRuntime: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("MAYRA", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text("Living companion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)) {
                TextButton(onClick = onExpand) { Text("⋮", style = MaterialTheme.typography.titleLarge) }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                DropdownMenuItem(text = { Text("Personal Workspace") }, onClick = { onDismiss(); onOpenWorkspace() })
                DropdownMenuItem(text = { Text("File Intelligence") }, onClick = { onDismiss(); onOpenFiles() })
                DropdownMenuItem(text = { Text("Device readiness") }, onClick = { onDismiss(); onOpenDevice() })
                DropdownMenuItem(text = { Text("Runtime") }, onClick = { onDismiss(); onOpenRuntime() })
                DropdownMenuItem(text = { Text("Settings") }, onClick = { onDismiss(); onOpenSettings() })
                if (hasMessages) {
                    DropdownMenuItem(text = { Text("Clear conversation") }, onClick = { onDismiss(); onClear() })
                }
            }
        }
    }
}

@Composable
private fun CompactMayraPresence(userName: String, visualState: UnifiedMayraVisualState) {
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        MayraCharacterPresence(
            state = visualState.toPresenceState(),
            modifier = Modifier.fillMaxWidth().height(154.dp)
        )
        Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)) {
            Text(
                "${visualState.statusDot()} ${visualState.statusLabel()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
        Text(
            if (userName.isBlank()) "Hello. I’m here with you." else "Hello, $userName. I’m here with you.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyConversationPrompt(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(if (userName.isBlank()) "What would you like to do?" else "What do you need, $userName?", fontWeight = FontWeight.SemiBold)
            Text(
                "Type naturally or tap the microphone. Structured file, table and document work can continue inside Personal Workspace.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UnifiedMessageBubble(message: MayraMessage) {
    val user = message.sender == MayraMessage.Sender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 340.dp),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = if (user) 22.dp else 7.dp,
                bottomEnd = if (user) 7.dp else 22.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (user) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
            )
        ) {
            Column(Modifier.padding(horizontal = 15.dp, vertical = 11.dp)) {
                Text(
                    if (user) "You" else "Mayra",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(message.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun QuickPromptRow(onQuickPrompt: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        AssistChip(
            onClick = { onQuickPrompt("Aaj ka din plan karo") },
            label = { Text("Plan my day", maxLines = 1) },
            modifier = Modifier.weight(1f)
        )
        AssistChip(
            onClick = { onQuickPrompt("Mujhe reminder banana hai") },
            label = { Text("Remind me", maxLines = 1) },
            modifier = Modifier.weight(1f)
        )
        AssistChip(
            onClick = { onQuickPrompt("XYZ ka bill dekho") },
            label = { Text("Workspace", maxLines = 1) },
            modifier = Modifier.weight(1f)
        )
    }
}

private fun UnifiedMayraVisualState.toPresenceState(): MayraPresenceState = when (this) {
    UnifiedMayraVisualState.READY,
    UnifiedMayraVisualState.LISTENING,
    UnifiedMayraVisualState.SPEAKING -> MayraPresenceState.IDLE
    UnifiedMayraVisualState.UNDERSTANDING,
    UnifiedMayraVisualState.THINKING -> MayraPresenceState.THINKING
    UnifiedMayraVisualState.OFFLINE -> MayraPresenceState.OFFLINE
    UnifiedMayraVisualState.ERROR -> MayraPresenceState.NEEDS_ATTENTION
}

private fun UnifiedMayraVisualState.statusLabel(): String = when (this) {
    UnifiedMayraVisualState.READY -> "Ready to listen"
    UnifiedMayraVisualState.LISTENING -> "Listening"
    UnifiedMayraVisualState.UNDERSTANDING -> "Understanding"
    UnifiedMayraVisualState.THINKING -> "Thinking"
    UnifiedMayraVisualState.SPEAKING -> "Speaking"
    UnifiedMayraVisualState.OFFLINE -> "Offline mode"
    UnifiedMayraVisualState.ERROR -> "Needs attention"
}

private fun UnifiedMayraVisualState.statusDot(): String = when (this) {
    UnifiedMayraVisualState.READY,
    UnifiedMayraVisualState.LISTENING,
    UnifiedMayraVisualState.SPEAKING -> "●"
    UnifiedMayraVisualState.UNDERSTANDING,
    UnifiedMayraVisualState.THINKING -> "◉"
    UnifiedMayraVisualState.OFFLINE -> "○"
    UnifiedMayraVisualState.ERROR -> "!"
}
