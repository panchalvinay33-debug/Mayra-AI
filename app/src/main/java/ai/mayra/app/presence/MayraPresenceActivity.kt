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
import ai.mayra.app.testing.MayraDeviceTestActivity
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Calendar

class MayraPresenceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settingsStore = MayraSettingsStore(this)
        if (!settingsStore.read().onboardingCompleted) {
            startActivity(Intent(this, SettingsActivity::class.java).putExtra(SettingsActivity.EXTRA_ONBOARDING, true))
        }
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
                    onDeviceTest = { startActivity(Intent(this, MayraDeviceTestActivity::class.java)) },
                    onSettings = { startActivity(Intent(this, SettingsActivity::class.java)) }
                )
            }
        }
    }
}

@Composable
private fun MayraPresenceHome(
    userName: String,
    ownerSummary: String,
    onChat: () -> Unit,
    onRuntime: () -> Unit,
    onPulse: () -> Unit,
    onNotifications: () -> Unit,
    onActionControls: () -> Unit,
    onOwnerSetup: () -> Unit,
    onPeople: () -> Unit,
    onReminders: () -> Unit,
    onAgenda: () -> Unit,
    onDeviceTest: () -> Unit,
    onSettings: () -> Unit
) {
    var pulse by remember { mutableStateOf(buildMayraPulseState(MayraRuntime.deviceRuntime.latest())) }
    var menuExpanded by remember { mutableStateOf(false) }
    val state = pulse.toPresenceState()
    val greeting = proactiveGreeting(userName, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    LaunchedEffect(Unit) {
        MayraRuntime.deviceRuntime.capture(force = true)
        pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest())
        while (true) {
            delay(15_000)
            MayraRuntime.deviceRuntime.capture(force = false)
            pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest())
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.055f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                LivingHomeHeader(
                    onMenu = { menuExpanded = true },
                    menuExpanded = menuExpanded,
                    onDismissMenu = { menuExpanded = false },
                    onAgenda = onAgenda,
                    onReminders = onReminders,
                    onNotifications = onNotifications,
                    onPeople = onPeople,
                    onPulse = onPulse,
                    onRuntime = onRuntime,
                    onActionControls = onActionControls,
                    onDeviceTest = onDeviceTest,
                    onOwnerSetup = onOwnerSetup,
                    onSettings = onSettings
                )

                Spacer(Modifier.height(2.dp))
                MayraCharacterPresence(
                    state = state,
                    modifier = Modifier.clickable(onClick = onChat)
                )
                Text(
                    state.label,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    greeting,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    state.prompt,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Talk to Mayra", fontWeight = FontWeight.SemiBold)
                }

                CompactAccessCard(ownerSummary = ownerSummary, onOpen = onOwnerSetup)

                LivingInfoCard(
                    eyebrow = "TODAY",
                    title = pulse.message.ifBlank { "Your day is ready" },
                    detail = pulse.healthScore?.let { score ->
                        "Phone health $score / 100 · ${pulse.batteryText} battery · ${pulse.networkText}"
                    } ?: "Open your agenda for reminders, events and follow-ups.",
                    actionLabel = "Open my day",
                    onAction = onAgenda
                )

                val insight = pulse.suggestions.firstOrNull()
                LivingInfoCard(
                    eyebrow = "MAYRA NOTICED",
                    title = insight?.title ?: "Everything looks calm",
                    detail = insight?.message ?: "I’ll surface something here only when it can genuinely help.",
                    actionLabel = "Phone pulse",
                    onAction = onPulse
                )

                Text(
                    "Mayra stays useful offline, respects Android boundaries and never reports an action as complete unless it can verify it.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LivingHomeHeader(
    onMenu: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onAgenda: () -> Unit,
    onReminders: () -> Unit,
    onNotifications: () -> Unit,
    onPeople: () -> Unit,
    onPulse: () -> Unit,
    onRuntime: () -> Unit,
    onActionControls: () -> Unit,
    onDeviceTest: () -> Unit,
    onOwnerSetup: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("MAYRA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("Living companion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            Surface(
                modifier = Modifier.clickable(onClick = onMenu),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Text("⋮", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                MenuLabel("MY DAY")
                MenuItem("Personal agenda", onAgenda, onDismissMenu)
                MenuItem("Reminders & follow-ups", onReminders, onDismissMenu)
                MenuItem("Notification intelligence", onNotifications, onDismissMenu)
                MenuLabel("MY PEOPLE")
                MenuItem("People & relationships", onPeople, onDismissMenu)
                MenuLabel("PHONE CONTROL")
                MenuItem("Phone pulse", onPulse, onDismissMenu)
                MenuItem("Live activity", onRuntime, onDismissMenu)
                MenuItem("Action safety", onActionControls, onDismissMenu)
                MenuItem("Personal device check", onDeviceTest, onDismissMenu)
                MenuLabel("MAYRA ACCESS")
                MenuItem("Permissions & owner setup", onOwnerSetup, onDismissMenu)
                MenuLabel("SETTINGS")
                MenuItem("Language, voice, AI & privacy", onSettings, onDismissMenu)
            }
        }
    }
}

@Composable
private fun MenuLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MenuItem(label: String, action: () -> Unit, dismiss: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = {
            dismiss()
            action()
        }
    )
}

@Composable
private fun CompactAccessCard(ownerSummary: String, onOpen: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Personal Owner Mode", fontWeight = FontWeight.SemiBold)
                Text(ownerSummary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            TextButton(onClick = onOpen) { Text("Access") }
        }
    }
}

@Composable
private fun LivingInfoCard(
    eyebrow: String,
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) { Text(actionLabel) }
        }
    }
}

internal fun MayraPulseState.toPresenceState(): MayraPresenceState = when (presence) {
    MayraPresence.CALM -> MayraPresenceState.IDLE
    MayraPresence.ATTENTIVE, MayraPresence.CONCERNED -> MayraPresenceState.NEEDS_ATTENTION
    MayraPresence.BUSY -> MayraPresenceState.THINKING
    MayraPresence.OFFLINE -> MayraPresenceState.OFFLINE
}