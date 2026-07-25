package ai.mayra.app.action

import ai.mayra.app.floating.FloatingMayraPreferences
import ai.mayra.app.floating.FloatingMayraService
import ai.mayra.app.safety.MayraGlobalStopStore
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
        val stopStore = MayraGlobalStopStore(applicationContext)
        setContent {
            MayraAITheme {
                MayraActionControlScreen(
                    context = applicationContext,
                    engine = engine,
                    stopStore = stopStore,
                    onClose = ::finish
                )
            }
        }
    }
}

@Composable
private fun MayraActionControlScreen(
    context: Context,
    engine: MayraActionEngine,
    stopStore: MayraGlobalStopStore,
    onClose: () -> Unit
) {
    var refresh by remember { mutableIntStateOf(0) }
    val stopSnapshot = remember(refresh) { stopStore.snapshot() }
    val stopped = stopSnapshot.stopped || engine.isStopped()
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
            Text("Mayra Action Controls", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (stopped) {
                    "Global Stop is active and survives app restart, phone reboot and app update. New phone actions and automatic Floating Mayra startup remain blocked."
                } else {
                    "Phone actions are enabled and protected by capability, permission, risk and confirmation checks."
                }
            )

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Persistent emergency kill switch", fontWeight = FontWeight.SemiBold)
                    Text("Stop immediately disables new Mayra phone actions and prevents the floating companion from automatically returning after restart.")
                    stopSnapshot.reason?.takeIf(String::isNotBlank)?.let {
                        Text("Last change: $it", style = MaterialTheme.typography.bodySmall)
                    }
                    if (stopped) {
                        Button(
                            onClick = {
                                MayraActionRuntime.resume("Owner resumed Mayra from Action Controls.")
                                refresh++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Resume Mayra actions") }
                    } else {
                        OutlinedButton(
                            onClick = {
                                MayraActionRuntime.stopAll("Owner activated Global Stop from Action Controls.")
                                FloatingMayraPreferences(context).enabled = false
                                context.stopService(Intent(context, FloatingMayraService::class.java))
                                refresh++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Stop all Mayra actions") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Visible action history", fontWeight = FontWeight.SemiBold)
                        Text("${events.size} shown")
                    }
                    Text(
                        "Only bounded action metadata and safe status text are shown here. Passwords, OTPs and raw secrets must never be written to this log.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (events.isEmpty()) {
                        Text("No actions have been processed by the safety engine yet.")
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
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
