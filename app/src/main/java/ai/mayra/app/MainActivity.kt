package ai.mayra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.mayra.app.chat.ChatViewModel
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.ui.theme.MayraAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraHome() } }
    }
}

@Composable
private fun MayraHome(viewModel: ChatViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, tonalElevation = 6.dp, modifier = Modifier.size(58.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("M", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Mayra AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (state.isThinking) "Thinking…" else "● Ready to help")
                }
            }

            Spacer(Modifier.height(20.dp))
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.messages.isEmpty()) {
                    item { Text("Namaste. I’m Mayra. What can I help you with today?", style = MaterialTheme.typography.titleMedium) }
                }
                items(state.messages) { message ->
                    val label = if (message.sender == MayraMessage.Sender.USER) "You" else "Mayra"
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Text(message.text)
                        }
                    }
                }
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.input,
                onValueChange = viewModel::updateInput,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ask Mayra anything…") },
                enabled = !state.isThinking,
                singleLine = true
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = viewModel::sendMessage,
                enabled = state.input.isNotBlank() && !state.isThinking,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (state.isThinking) "Mayra is thinking…" else "Send to Mayra")
            }
        }
    }
}
