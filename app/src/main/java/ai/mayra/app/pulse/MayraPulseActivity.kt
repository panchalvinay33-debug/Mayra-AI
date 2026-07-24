package ai.mayra.app.pulse

import ai.mayra.app.MayraRuntime
import ai.mayra.app.device.DeviceSeverity
import ai.mayra.app.ui.theme.MayraAITheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MayraPulseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraPulseScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MayraPulseScreen(onClose: () -> Unit) {
    var pulse by remember { mutableStateOf(buildMayraPulseState(MayraRuntime.deviceRuntime.latest())) }
    var refreshing by remember { mutableStateOf(false) }

    suspend fun refresh(force: Boolean) {
        refreshing = true
        MayraRuntime.deviceRuntime.capture(force = force)
        pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest())
        refreshing = false
    }

    LaunchedEffect(Unit) {
        refresh(force = true)
        while (true) {
            delay(15_000)
            refresh(force = false)
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LivingOrb(pulse.presence)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Mayra Pulse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(pulse.headline, style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(pulse.message)

            pulse.healthScore?.let { score ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Phone health", fontWeight = FontWeight.SemiBold)
                        Text("$score / 100", style = MaterialTheme.typography.headlineMedium)
                        Text(healthMeaning(score), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PulseMetric("Battery", pulse.batteryText, Modifier.weight(1f))
                PulseMetric("Network", pulse.networkText, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PulseMetric("Storage", pulse.storageText, Modifier.weight(1f))
                PulseMetric("Memory", pulse.memoryText, Modifier.weight(1f))
            }
            PulseMetric("Phone senses", "${pulse.capabilityCount} capabilities detected", Modifier.fillMaxWidth())

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("What I notice", fontWeight = FontWeight.SemiBold)
                    if (pulse.suggestions.isEmpty()) {
                        Text("Nothing urgent. I’ll keep watching quietly.")
                    } else {
                        pulse.suggestions.forEach { insight ->
                            Text(
                                text = "${severityMark(insight.severity)} ${insight.title}",
                                fontWeight = FontWeight.Medium
                            )
                            Text(insight.message, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Text(
                "Mayra only reads Android device-health signals available to this app. She does not secretly control the phone; sensitive actions still require permission or confirmation.",
                style = MaterialTheme.typography.bodySmall
            )

            Button(
                onClick = { MayraRuntime.deviceRuntime.capture(force = true); pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest()) },
                enabled = !refreshing,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (refreshing) "Reading phone…" else "Refresh phone awareness") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to Mayra") }
        }
    }
}

@Composable
private fun LivingOrb(presence: MayraPresence) {
    val transition = rememberInfiniteTransition(label = "mayra-pulse")
    val scale by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(if (presence == MayraPresence.CONCERNED) 700 else 1500), RepeatMode.Reverse),
        label = "pulse-scale"
    )
    Surface(
        modifier = Modifier.size(68.dp).scale(scale),
        shape = CircleShape,
        tonalElevation = 10.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("M", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PulseMetric(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun healthMeaning(score: Int): String = when (score.coerceIn(0, 100)) {
    in 85..100 -> "Healthy and ready"
    in 65..84 -> "Mostly healthy; keep an eye on small issues"
    in 40..64 -> "Some conditions may slow the phone"
    else -> "Phone needs attention before heavy work"
}

private fun severityMark(severity: DeviceSeverity): String = when (severity) {
    DeviceSeverity.CRITICAL -> "🔴"
    DeviceSeverity.HIGH -> "🟠"
    DeviceSeverity.MEDIUM -> "🟡"
    DeviceSeverity.LOW -> "🔵"
    DeviceSeverity.INFO -> "•"
}
