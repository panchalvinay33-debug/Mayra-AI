package ai.mayra.app.testing

import ai.mayra.app.MainActivity
import ai.mayra.app.ai.AiProviderActivity
import ai.mayra.app.background.MayraNotificationCenterActivity
import ai.mayra.app.calendar.MayraAgendaActivity
import ai.mayra.app.document.MayraDocumentActivity
import ai.mayra.app.floating.FloatingMayraActivity
import ai.mayra.app.identity.MayraIdentityActivity
import ai.mayra.app.memory.MayraMemoryActivity
import ai.mayra.app.memory.MayraMemoryBackupActivity
import ai.mayra.app.memory.MayraVoiceNotesActivity
import ai.mayra.app.owner.MayraOwnerCapabilityInspector
import ai.mayra.app.owner.MayraOwnerSetupActivity
import ai.mayra.app.owner.OwnerAccessState
import ai.mayra.app.presence.MayraPresenceActivity
import ai.mayra.app.pulse.MayraPulseActivity
import ai.mayra.app.reminder.MayraReminderActivity
import ai.mayra.app.runtime.RuntimeControlActivity
import ai.mayra.app.settings.SettingsActivity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Personal sideloaded-build test states. They are deliberately user-confirmed, not fabricated. */
enum class DeviceTestState { NOT_TESTED, PASSED, FAILED, BLOCKED }

enum class DeviceTestId {
    ONBOARDING_SETTINGS,
    LIVING_PRESENCE,
    TEXT_CHAT,
    VOICE_INPUT_OUTPUT,
    PHONE_PULSE,
    OPEN_APP_ACTION,
    CONTACT_CALL_FLOW,
    MESSAGE_DRAFT_FLOW,
    REMINDER_CREATE_ALERT,
    AGENDA_CREATE_MANAGE,
    NOTIFICATION_CAPTURE_SUMMARY,
    NOTIFICATION_REPLY,
    BACKGROUND_RUNTIME,
    ONLINE_AI_PROVIDER,
    FLOATING_MAYRA,
    ASSISTIVE_CONTEXT,
    MEMORY_NOTES,
    MEMORY_BACKUP_RESTORE,
    VOICE_NOTES,
    DOCUMENT_LIBRARY
}

data class DeviceTestDefinition(
    val id: DeviceTestId,
    val title: String,
    val instruction: String,
    val destination: Class<out ComponentActivity>,
    val mandatory: Boolean = true
)

data class DeviceTestSummary(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val blocked: Int,
    val notTested: Int,
    val mandatoryTotal: Int,
    val mandatoryPassed: Int,
    val mandatoryFailed: Int,
    val accessReady: Int,
    val accessTotal: Int
) {
    val tested: Int get() = passed + failed + blocked
    val passPercent: Int get() = if (total == 0) 0 else passed * 100 / total
    val mandatoryPercent: Int get() = if (mandatoryTotal == 0) 0 else mandatoryPassed * 100 / mandatoryTotal
    val accessPercent: Int get() = if (accessTotal == 0) 0 else accessReady * 100 / accessTotal

    fun headline(): String = when {
        mandatoryFailed > 0 -> "$mandatoryFailed mandatory checks failed · fix before daily use"
        failed > 0 -> "$failed checks failed · review before daily use"
        blocked > 0 -> "$blocked checks blocked by permission/device setup"
        passed == total && total > 0 -> "Personal alpha check complete"
        tested == 0 -> "Device test has not started"
        else -> "$passed of $total checks passed"
    }

    fun alphaAccepted(): Boolean = mandatoryFailed == 0 && mandatoryPercent >= 80 && failed == 0
}

class MayraDeviceTestStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("mayra_device_alpha_tests", Context.MODE_PRIVATE)

    fun read(id: DeviceTestId): DeviceTestState = runCatching {
        DeviceTestState.valueOf(preferences.getString(id.name, DeviceTestState.NOT_TESTED.name).orEmpty())
    }.getOrDefault(DeviceTestState.NOT_TESTED)

    fun save(id: DeviceTestId, state: DeviceTestState) {
        preferences.edit().putString(id.name, state.name).apply()
    }

    fun snapshot(): Map<DeviceTestId, DeviceTestState> = DeviceTestId.entries.associateWith(::read)
    fun reset() { preferences.edit().clear().apply() }
}

fun buildDeviceTestSummary(
    states: Map<DeviceTestId, DeviceTestState>,
    accessReady: Int,
    accessTotal: Int,
    definitions: List<DeviceTestDefinition> = DEVICE_TESTS
): DeviceTestSummary {
    val mandatoryIds = definitions.filter { it.mandatory }.mapTo(mutableSetOf()) { it.id }
    return DeviceTestSummary(
        total = DeviceTestId.entries.size,
        passed = states.values.count { it == DeviceTestState.PASSED },
        failed = states.values.count { it == DeviceTestState.FAILED },
        blocked = states.values.count { it == DeviceTestState.BLOCKED },
        notTested = states.values.count { it == DeviceTestState.NOT_TESTED },
        mandatoryTotal = mandatoryIds.size,
        mandatoryPassed = mandatoryIds.count { states[it] == DeviceTestState.PASSED },
        mandatoryFailed = mandatoryIds.count { states[it] == DeviceTestState.FAILED },
        accessReady = accessReady.coerceIn(0, accessTotal.coerceAtLeast(0)),
        accessTotal = accessTotal.coerceAtLeast(0)
    )
}

class MayraDeviceTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraDeviceTestScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MayraDeviceTestScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MayraDeviceTestStore(context) }
    val inspector = remember(context) { MayraOwnerCapabilityInspector(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val access = remember(refresh) { inspector.snapshot() }
    val states = remember(refresh) { store.snapshot() }
    val summary = remember(refresh) {
        buildDeviceTestSummary(states, access.count { it.state == OwnerAccessState.READY }, access.size)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Personal Device Test", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Run all 20 checks on the owner phone. Mark Pass only after you actually see the feature work; never convert a blocked or untested item into a pass.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(summary.headline(), fontWeight = FontWeight.SemiBold)
                    Text("Feature pass: ${summary.passed}/${summary.total} · ${summary.passPercent}%")
                    Text("Mandatory pass: ${summary.mandatoryPassed}/${summary.mandatoryTotal} · ${summary.mandatoryPercent}%")
                    Text("Access ready: ${summary.accessReady}/${summary.accessTotal} · ${summary.accessPercent}%")
                    Text("Failed ${summary.failed} · blocked ${summary.blocked} · not tested ${summary.notTested}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Automatic access checks", fontWeight = FontWeight.SemiBold)
                    access.forEach { item ->
                        Text("• ${item.title}: ${item.state.name.replace('_', ' ').lowercase()} — ${item.detail}")
                    }
                    Button(
                        onClick = { context.startActivity(Intent(context, MayraOwnerSetupActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Fix phone access") }
                    OutlinedButton(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Refresh access checks") }
                }
            }

            DEVICE_TESTS.forEachIndexed { index, test ->
                val state = states[test.id] ?: DeviceTestState.NOT_TESTED
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("${index + 1}. ${test.title}${if (test.mandatory) " · mandatory" else " · extended"}", fontWeight = FontWeight.SemiBold)
                        Text(test.instruction, style = MaterialTheme.typography.bodySmall)
                        Text("Result: ${state.name.replace('_', ' ').lowercase()}")
                        Button(
                            onClick = { context.startActivity(Intent(context, test.destination)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Open test") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { store.save(test.id, DeviceTestState.PASSED); refresh++ }, modifier = Modifier.weight(1f)) { Text("Pass") }
                            OutlinedButton(onClick = { store.save(test.id, DeviceTestState.FAILED); refresh++ }, modifier = Modifier.weight(1f)) { Text("Fail") }
                            OutlinedButton(onClick = { store.save(test.id, DeviceTestState.BLOCKED); refresh++ }, modifier = Modifier.weight(1f)) { Text("Blocked") }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Alpha decision", fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            summary.mandatoryFailed > 0 -> "Reject this APK for daily use. Fix every mandatory failure first."
                            summary.failed > 0 -> "Keep the build in controlled testing until all recorded failures are fixed."
                            summary.alphaAccepted() -> "Eligible for controlled Personal Alpha V0.1 use. Keep critical actions supervised and retain the rollback APK."
                            else -> "Continue testing until at least 80% of mandatory checks pass with no recorded failure."
                        }
                    )
                }
            }

            OutlinedButton(onClick = { store.reset(); refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("Reset test session") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

internal val DEVICE_TESTS = listOf(
    DeviceTestDefinition(DeviceTestId.ONBOARDING_SETTINGS, "Onboarding & settings", "Complete profile setup, close the screen, reopen Settings and confirm values persist.", SettingsActivity::class.java),
    DeviceTestDefinition(DeviceTestId.LIVING_PRESENCE, "Living Mayra presence", "Open Living Home and confirm the animated presence, greeting, Today card and phone-aware state render without a crash.", MayraPresenceActivity::class.java),
    DeviceTestDefinition(DeviceTestId.TEXT_CHAT, "Offline text chat", "Send Hindi, Hinglish and English messages; verify local replies and conversation context.", MainActivity::class.java),
    DeviceTestDefinition(DeviceTestId.VOICE_INPUT_OUTPUT, "Voice input & spoken reply", "Grant microphone access, speak a command, interrupt speech once and confirm transcript plus spoken reply.", MainActivity::class.java),
    DeviceTestDefinition(DeviceTestId.PHONE_PULSE, "Phone Pulse", "Refresh battery, network, storage and memory awareness; compare with actual phone state.", MayraPulseActivity::class.java),
    DeviceTestDefinition(DeviceTestId.OPEN_APP_ACTION, "Open-app action", "Say or type ‘YouTube kholo’ and verify the requested installed app opens.", MainActivity::class.java),
    DeviceTestDefinition(DeviceTestId.CONTACT_CALL_FLOW, "Contact & call flow", "Create a relationship alias, request a call and verify correct-contact resolution plus safe confirmation/handoff.", MayraIdentityActivity::class.java),
    DeviceTestDefinition(DeviceTestId.MESSAGE_DRAFT_FLOW, "Message draft flow", "Prepare a message for a mapped contact and verify Mayra opens a reviewed draft without claiming delivery.", MainActivity::class.java),
    DeviceTestDefinition(DeviceTestId.REMINDER_CREATE_ALERT, "Reminder create & alert", "Create a reminder 2–5 minutes ahead; verify notification, Complete and Snooze actions, then retest after reboot.", MayraReminderActivity::class.java),
    DeviceTestDefinition(DeviceTestId.AGENDA_CREATE_MANAGE, "Personal agenda", "Create an event, query today’s agenda, move a reminder and review Android Calendar export.", MayraAgendaActivity::class.java),
    DeviceTestDefinition(DeviceTestId.NOTIFICATION_CAPTURE_SUMMARY, "Notification capture & summary", "Enable Notification Access, receive a safe test notification and verify grouped summary plus OTP/sensitive redaction.", MayraNotificationCenterActivity::class.java),
    DeviceTestDefinition(DeviceTestId.NOTIFICATION_REPLY, "Supported notification reply", "Use a notification with a Reply action; review, confirm once and verify duplicate-send protection.", MayraNotificationCenterActivity::class.java),
    DeviceTestDefinition(DeviceTestId.BACKGROUND_RUNTIME, "Background runtime", "Run an immediate background scan, leave the app, return and verify diagnostics/attention state update.", RuntimeControlActivity::class.java),
    DeviceTestDefinition(DeviceTestId.ONLINE_AI_PROVIDER, "Online AI provider", "Configure your own key, test connection and verify online chat falls back safely when disconnected.", AiProviderActivity::class.java, mandatory = false),
    DeviceTestDefinition(DeviceTestId.FLOATING_MAYRA, "Floating Mayra", "Grant overlay access; start, drag, edge-dock, expand, minimize, reopen after app close and stop from the persistent notification.", FloatingMayraActivity::class.java),
    DeviceTestDefinition(DeviceTestId.ASSISTIVE_CONTEXT, "Assistive screen context", "Open owner access setup, explicitly enable Accessibility, verify safe visible-text context and confirm password/PIN/OTP content is excluded.", MayraOwnerSetupActivity::class.java, mandatory = false),
    DeviceTestDefinition(DeviceTestId.MEMORY_NOTES, "Memory, notes & briefing", "Create a note, idea, shopping list and checklist; pin/archive items and verify the Living Home briefing excludes sensitive content.", MayraMemoryActivity::class.java),
    DeviceTestDefinition(DeviceTestId.MEMORY_BACKUP_RESTORE, "Memory backup & restore", "Create an encrypted backup, inspect its summary, restore into a controlled test state and verify no duplicate or silent overwrite.", MayraMemoryBackupActivity::class.java),
    DeviceTestDefinition(DeviceTestId.VOICE_NOTES, "Review-first voice notes", "Record a short voice note, review transcript/title before saving and confirm cancellation leaves no unwanted memory.", MayraVoiceNotesActivity::class.java, mandatory = false),
    DeviceTestDefinition(DeviceTestId.DOCUMENT_LIBRARY, "Local document library", "Import a safe test document, open its local excerpt, confirm unsupported files fail honestly and verify no automatic cloud upload.", MayraDocumentActivity::class.java, mandatory = false)
)
