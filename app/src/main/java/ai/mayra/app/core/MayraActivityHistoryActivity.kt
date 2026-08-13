package ai.mayra.app.core

import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Context
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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MayraActivityHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                MayraActivityHistoryScreen(
                    onClose = ::finish,
                    onShare = ::shareHistory
                )
            }
        }
    }

    private fun shareHistory(text: String) {
        if (text.isBlank()) return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Mayra activity history")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, "Export Mayra activity history"))
    }
}

@Composable
private fun MayraActivityHistoryScreen(
    onClose: () -> Unit,
    onShare: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val log = remember(context) {
        MayraPersistentActivityLog(
            context.getSharedPreferences(
                MayraAndroidRuntimeComposition.ACTIVITY_PREFERENCES,
                Context.MODE_PRIVATE
            )
        )
    }
    var refresh by remember { mutableIntStateOf(0) }
    val records = remember(refresh) { log.snapshot().asReversed() }
    val formatter = remember {
        DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")
            .withZone(ZoneId.systemDefault())
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Mayra Activity History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Private on-device audit records for routing, confirmations, actions and failures. " +
                    "History is bounded and is never an execution permission."
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onShare(log.exportText()) },
                    enabled = records.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Export") }
                OutlinedButton(
                    onClick = {
                        log.clear()
                        refresh++
                    },
                    enabled = records.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }
            }

            if (records.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "No activity has been recorded yet.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Text("${records.size} recent record${if (records.size == 1) "" else "s"}")
                records.forEach { record ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(record.status.name.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                                Text(formatter.format(record.timestamp), style = MaterialTheme.typography.labelSmall)
                            }
                            Text("${record.outcome} · ${record.capability}", style = MaterialTheme.typography.labelMedium)
                            Text(record.detail)
                            record.idempotencyKey?.let {
                                Text("Action key: ${it.take(12)}…", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}
