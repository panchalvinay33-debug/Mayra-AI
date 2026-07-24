package ai.mayra.app.presence

import ai.mayra.app.MainActivity
import ai.mayra.app.MayraRuntime
import ai.mayra.app.action.MayraActionControlActivity
import ai.mayra.app.background.MayraNotificationCenterActivity
import ai.mayra.app.calendar.MayraAgendaActivity
import ai.mayra.app.identity.MayraIdentityActivity
import ai.mayra.app.owner.MayraOwnerModeStore
import ai.mayra.app.owner.MayraOwnerSetupActivity
import ai.mayra.app.owner.ownerModeSafetySummary
import ai.mayra.app.pulse.MayraPresence
import ai.mayra.app.pulse.MayraPulseActivity
import ai.mayra.app.pulse.MayraPulseState
import ai.mayra.app.pulse.buildMayraPulseState
import ai.mayra.app.reminder.MayraReminderActivity
import ai.mayra.app.runtime.RuntimeControlActivity
import ai.mayra.app.settings.MayraSettingsStore
import ai.mayra.app.settings.SettingsActivity
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar

class MayraPresenceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = MayraSettingsStore(this)
        if (!settingsStore.read().onboardingCompleted) startActivity(Intent(this, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_ONBOARDING, true))
        setContent {
            MayraAITheme {
                MayraPresenceHome(
                    userName = remember { mutableStateOf(settingsStore.read().normalizedName) }.value,
                    ownerSummary = remember { MayraOwnerModeStore(this).read() }.let(::ownerModeSafetySummary),
                    onChat = { startActivity(Intent(this, MainActivity::class.java)) },
                    onRuntime = { startActivity(Intent(this, RuntimeControlActivity::class.java)) },
                    onPulse = { startActivity(Intent(this, MayraPulseActivity::class.java)) },
                    onNotifications = { startActivity(Intent(this, MayraNotificationCenterActivity::class.java)) },
                    onActionControls = { startActivity(Intent(this, MayraActionControlActivity::class.java)) },
                    onOwnerSetup = { startActivity(Intent(this, MayraOwnerSetupActivity::class.java)) },
                    onPeople = { startActivity(Intent(this, MayraIdentityActivity::class.java)) },
                    onReminders = { startActivity(Intent(this, MayraReminderActivity::class.java)) },
                    onAgenda = { startActivity(Intent(this, MayraAgendaActivity::class.java)) },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun MayraPresenceHome(
    userName: String, ownerSummary: String,
    onChat: () -> Unit, onRuntime: () -> Unit, onPulse: () -> Unit,
    onNotifications: () -> Unit, onActionControls: () -> Unit, onOwnerSetup: () -> Unit,
    onPeople: () -> Unit, onReminders: () -> Unit, onAgenda: () -> Unit, onSettings: () -> Unit
) {
    var pulse by remember { mutableStateOf(buildMayraPulseState(MayraRuntime.deviceRuntime.latest())) }
    val state = pulse.toPresenceState()
    val greeting = proactiveGreeting(userName, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    LaunchedEffect(Unit) {
        MayraRuntime.deviceRuntime.capture(force = true); pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest())
        while (true) { delay(15_000); MayraRuntime.deviceRuntime.capture(force = false); pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest()) }
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(8.dp)); Text("MAYRA", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            MayraPresenceOrb(state = state); Text(state.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(greeting, style = MaterialTheme.typography.titleMedium); Text(pulse.message, style = MaterialTheme.typography.bodyMedium)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Personal Owner Mode", fontWeight = FontWeight.SemiBold); Text(ownerSummary, style = MaterialTheme.typography.bodySmall)
                Button(onClick = onOwnerSetup, modifier = Modifier.fillMaxWidth()) { Text("Complete phone access setup") }
            } }
            pulse.healthScore?.let { score -> Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Phone health", fontWeight = FontWeight.SemiBold); Text("$score / 100", fontWeight = FontWeight.Bold) }
                Text("${pulse.batteryText} battery · ${pulse.networkText}"); Text("${pulse.storageText} storage · ${pulse.memoryText} memory", style = MaterialTheme.typography.bodySmall)
            } } }
            pulse.suggestions.firstOrNull()?.let { insight -> Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("I noticed this", fontWeight = FontWeight.SemiBold); Text(insight.title, fontWeight = FontWeight.Medium); Text(insight.message, style = MaterialTheme.typography.bodySmall)
            } } }
            Button(onClick = onChat, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Talk to Mayra") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onPulse, modifier = Modifier.weight(1f)) { Text("Phone pulse") }
                OutlinedButton(onClick = onRuntime, modifier = Modifier.weight(1f)) { Text("Live activity") }
            }
            OutlinedButton(onClick = onAgenda, modifier = Modifier.fillMaxWidth()) { Text("Personal agenda") }
            OutlinedButton(onClick = onReminders, modifier = Modifier.fillMaxWidth()) { Text("Reminders & follow-ups") }
            OutlinedButton(onClick = onPeople, modifier = Modifier.fillMaxWidth()) { Text("People & relationships") }
            OutlinedButton(onClick = onNotifications, modifier = Modifier.fillMaxWidth()) { Text("Notification intelligence") }
            OutlinedButton(onClick = onActionControls, modifier = Modifier.fillMaxWidth()) { Text("Action safety") }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
            Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your phone, with a mind", fontWeight = FontWeight.SemiBold)
                Text("• Understand phone health and connection"); Text("• Remember people and relationships")
                Text("• Run a private agenda, reminders and follow-ups"); Text("• Summarise notifications and protect sensitive content")
                Text("• Speak naturally in Hindi, Hinglish or English"); Text("• Open apps, prepare calls and messages safely")
                Text("• Stop all action execution instantly"); Text("• Stay useful offline and become smarter online")
            } }
            Text("Owner Mode maximizes access you explicitly grant. Android secure boundaries and critical-action protections are not secretly bypassed.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun MayraPulseState.toPresenceState(): MayraPresenceState = when (presence) {
    MayraPresence.CALM -> MayraPresenceState.IDLE
    MayraPresence.ATTENTIVE, MayraPresence.CONCERNED -> MayraPresenceState.NEEDS_ATTENTION
    MayraPresence.BUSY -> MayraPresenceState.THINKING
    MayraPresence.OFFLINE -> MayraPresenceState.OFFLINE
}