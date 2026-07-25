package ai.mayra.app.presence

import ai.mayra.app.MainActivity
import ai.mayra.app.MayraRuntime
import ai.mayra.app.action.MayraActionControlActivity
import ai.mayra.app.background.MayraNotificationCenterActivity
import ai.mayra.app.calendar.MayraAgendaActivity
import ai.mayra.app.floating.FloatingMayraActivity
import ai.mayra.app.identity.MayraIdentityActivity
import ai.mayra.app.knowledge.MayraPersonalBriefing
import ai.mayra.app.memory.MayraMemoryActivity
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                    userName = settingsStore.read().normalizedName,
                    ownerSummary = ownerModeSafetySummary(MayraOwnerModeStore(this).read()),
                    briefingProvider = { MayraPersonalBriefing(this).compose() },
                    onChat = { startActivity(Intent(this, MainActivity::class.java)) },
                    onRuntime = { startActivity(Intent(this, RuntimeControlActivity::class.java)) },
                    onPulse = { startActivity(Intent(this, MayraPulseActivity::class.java)) },
                    onNotifications = { startActivity(Intent(this, MayraNotificationCenterActivity::class.java)) },
                    onActionControls = { startActivity(Intent(this, MayraActionControlActivity::class.java)) },
                    onOwnerSetup = { startActivity(Intent(this, MayraOwnerSetupActivity::class.java)) },
                    onFloatingMayra = { startActivity(Intent(this, FloatingMayraActivity::class.java)) },
                    onMemory = { startActivity(Intent(this, MayraMemoryActivity::class.java)) },
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
    briefingProvider: () -> ai.mayra.app.knowledge.PersonalBriefing,
    onChat: () -> Unit,
    onRuntime: () -> Unit,
    onPulse: () -> Unit,
    onNotifications: () -> Unit,
    onActionControls: () -> Unit,
    onOwnerSetup: () -> Unit,
    onFloatingMayra: () -> Unit,
    onMemory: () -> Unit,
    onPeople: () -> Unit,
    onReminders: () -> Unit,
    onAgenda: () -> Unit,
    onDeviceTest: () -> Unit,
    onSettings: () -> Unit
) {
    var pulse by remember { mutableStateOf(buildMayraPulseState(MayraRuntime.deviceRuntime.latest())) }
    var menuExpanded by remember { mutableStateOf(false) }
    var briefing by remember { mutableStateOf(briefingProvider()) }
    val state = pulse.toPresenceState()
    val greeting = proactiveGreeting(userName, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    LaunchedEffect(Unit) {
        MayraRuntime.deviceRuntime.capture(force = true)
        pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest())
        briefing = briefingProvider()
        while (true) {
            delay(15_000)
            MayraRuntime.deviceRuntime.capture(force = false)
            pulse = buildMayraPulseState(MayraRuntime.deviceRuntime.latest())
        }
    }

    Scaffold { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
        ) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LivingHomeHeader(
                    expanded = menuExpanded,
                    onExpand = { menuExpanded = true },
                    onDismiss = { menuExpanded = false },
                    onAgenda = onAgenda,
                    onReminders = onReminders,
                    onNotifications = onNotifications,
                    onMemory = onMemory,
                    onPeople = onPeople,
                    onPulse = onPulse,
                    onRuntime = onRuntime,
                    onActionControls = onActionControls,
                    onDeviceTest = onDeviceTest,
                    onOwnerSetup = onOwnerSetup,
                    onFloatingMayra = onFloatingMayra,
                    onSettings = onSettings
                )

                MayraCharacterPresence(
                    state = state,
                    modifier = Modifier.fillMaxWidth().height(238.dp).clickable(onClick = onChat)
                )

                PresenceStatusPill(state.label)
                Text(greeting, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(
                    state.prompt,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Talk to Mayra", fontWeight = FontWeight.SemiBold)
                }

                QuickAccessRow(
                    onReminders = onReminders,
                    onPeople = onPeople,
                    onMemory = onMemory
                )

                CompactAccessCard(ownerSummary, onOwnerSetup)

                LivingInfoCard(
                    eyebrow = "MY DAY",
                    title = briefing.title,
                    detail = buildString {
                        append(briefing.summary)
                        briefing.highlights.firstOrNull()?.let { append("\n$it") }
                    },
                    actionLabel = "Open agenda",
                    onAction = onAgenda
                )

                LivingInfoCard(
                    eyebrow = "PHONE PULSE",
                    title = pulse.message.ifBlank { "Your phone looks calm" },
                    detail = pulse.healthScore?.let {
                        "Phone health $it / 100 · ${pulse.batteryText} battery · ${pulse.networkText}"
                    } ?: "Open Phone Pulse for current device health.",
                    actionLabel = "View pulse",
                    onAction = onPulse
                )

                Text(
                    "Mayra keeps sensitive actions review-first and never says something happened unless it can verify it.",
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
private fun PresenceStatusPill(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
    ) {
        Text(
            text = "• $label",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun QuickAccessRow(
    onReminders: () -> Unit,
    onPeople: () -> Unit,
    onMemory: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onReminders, modifier = Modifier.weight(1f)) { Text("Reminders", maxLines = 1) }
        OutlinedButton(onClick = onPeople, modifier = Modifier.weight(1f)) { Text("People", maxLines = 1) }
        OutlinedButton(onClick = onMemory, modifier = Modifier.weight(1f)) { Text("Memory", maxLines = 1) }
    }
}

@Composable
private fun LivingHomeHeader(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onAgenda: () -> Unit,
    onReminders: () -> Unit,
    onNotifications: () -> Unit,
    onMemory: () -> Unit,
    onPeople: () -> Unit,
    onPulse: () -> Unit,
    onRuntime: () -> Unit,
    onActionControls: () -> Unit,
    onDeviceTest: () -> Unit,
    onOwnerSetup: () -> Unit,
    onFloatingMayra: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("MAYRA", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Text("Living companion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            Surface(
                modifier = Modifier.clickable(onClick = onExpand),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
            ) {
                Text("⋮", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 15.dp, vertical = 6.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
                MenuLabel("MY DAY")
                MenuItem("Personal agenda", onAgenda, onDismiss)
                MenuItem("Reminders & follow-ups", onReminders, onDismiss)
                MenuItem("Notification intelligence", onNotifications, onDismiss)
                MenuLabel("MEMORY")
                MenuItem("Memory & notes", onMemory, onDismiss)
                MenuItem("People & relationships", onPeople, onDismiss)
                MenuLabel("PHONE CONTROL")
                MenuItem("Phone pulse", onPulse, onDismiss)
                MenuItem("Live activity", onRuntime, onDismiss)
                MenuItem("Action safety", onActionControls, onDismiss)
                MenuItem("Personal device check", onDeviceTest, onDismiss)
                MenuLabel("MAYRA ACCESS")
                MenuItem("Permissions & owner setup", onOwnerSetup, onDismiss)
                MenuItem("Floating Mayra", onFloatingMayra, onDismiss)
                MenuLabel("SETTINGS")
                MenuItem("Language, voice, AI & privacy", onSettings, onDismiss)
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
    DropdownMenuItem(text = { Text(label) }, onClick = { dismiss(); action() })
}

@Composable
private fun CompactAccessCard(ownerSummary: String, onOpen: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Personal Owner Mode", fontWeight = FontWeight.SemiBold)
                Text(
                    ownerSummary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TextButton(onClick = onOpen) { Text("Review") }
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
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
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
