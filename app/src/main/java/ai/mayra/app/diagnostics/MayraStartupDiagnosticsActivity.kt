package ai.mayra.app.diagnostics

import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

class MayraStartupDiagnosticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val snapshot = MayraStartupHealth(this).snapshot()
        setContent {
            MayraAITheme {
                StartupDiagnosticsScreen(snapshot = snapshot, onClose = ::finish)
            }
        }
    }
}

@Composable
private fun StartupDiagnosticsScreen(snapshot: StartupHealthSnapshot, onClose: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Startup Health", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(snapshot.ownerSummary())

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Last startup", fontWeight = FontWeight.SemiBold)
                    Text("Started: ${formatTime(snapshot.lastStartAt)}")
                    Text("Completed: ${formatTime(snapshot.lastCompletedAt)}")
                    Text("Status: ${if (snapshot.lastStartCompleted) "completed" else "interrupted"}")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Degraded components", fontWeight = FontWeight.SemiBold)
                    if (snapshot.failedSteps.isEmpty()) {
                        Text("No non-critical startup failures recorded.")
                    } else {
                        snapshot.failedSteps.forEach { Text("• $it") }
                    }
                }
            }

            if (snapshot.lastErrorStep != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Most recent failure", fontWeight = FontWeight.SemiBold)
                        Text("Step: ${snapshot.lastErrorStep}")
                        Text("Type: ${snapshot.lastErrorType.orEmpty()}")
                        Text("Message: ${snapshot.lastErrorMessage.orEmpty()}")
                        Text("Time: ${formatTime(snapshot.lastErrorAt)}")
                    }
                }
            }

            Text(
                "These diagnostics never store conversation, contact, reminder, notification or backup content.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

private fun formatTime(value: Long): String = if (value <= 0L) {
    "Not recorded"
} else {
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM).format(Date(value))
}
