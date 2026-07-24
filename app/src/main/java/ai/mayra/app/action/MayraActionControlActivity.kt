package ai.mayra.app.action

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

class MayraActionControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val engine = MayraActionRuntime.install(applicationContext)
        setContent {
            MayraAITheme {
                MayraActionControlScreen(engine = engine, onClose = ::finish)
            }
        }
    }
}

@Composable
private fun MayraActionControlScreen(
    engine: MayraActionEngine,
    onClose: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val stopped = remember(refresh) { engine.isStopped() }
    val events = remember(refresh) { engine.auditSnapshot().takeLast(50).reversed() }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Mayra Action Controls",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (stopped) {
                    "All new phone actions are stopped. Chat and non-action features remain available."
                } else {
                    "Phone actions are enabled and protected by capability, permission, risk and confirmation checks."
                }
            )

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Emergency kill switch", fontWeight = FontWeight.SemiBold)
                    Text("Use this whenever you want Mayra to stop launching new phone actions immediately.")
                    if (stopped) {
                        Button(
                            onClick = {
                                engine.resume()
                                refresh++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Resume Mayra actions") }
                    } else {
                        OutlinedButton(
                            onClick = {
                                engine.stopAll()
                                refresh++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop all Mayra actions") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Visible action history", fontWeight = FontWeight.SemiBold)
                        Text("${events.size} shown")
                    }
                    Text(
                        "Only bounded action metadata and safe status text are shown here. Passwords, OTPs and raw secrets must never be written to this log.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (events.isEmpty()) {
                        Text("No actions have been processed by the new safety engine yet.")
                    } else {
                        events.forEach { event ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(event.type.name.replace('_', ' '), fontWeight = FontWeight.Medium)
                                    Text(event.status.name.replace('_', ' '))
                                    event.detail?.takeIf(String::isNotBlank)?.let {
                                        Text(it, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            engine.clearAudit()
                            refresh++
                        },
                        enabled = events.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear action history") }
                }
            }

            Text(
                "Opening an app, dialer, message composer or reminder screen is a user-visible handoff—not proof that the final action completed.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}
