package ai.mayra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.mayra.app.ui.theme.MayraAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraHome() } }
    }
}

@Composable
private fun MayraHome() {
    var message by remember { mutableStateOf("") }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(48.dp))
                Surface(shape = CircleShape, tonalElevation = 8.dp, modifier = Modifier.size(132.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("M", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text("Mayra AI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Your personal AI companion", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(28.dp))
                AssistChip(onClick = {}, label = { Text("● Ready to help") })
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ask Mayra anything…") },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Talk to Mayra")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
