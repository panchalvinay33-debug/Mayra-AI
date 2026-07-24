package ai.mayra.app.background

import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

class MayraNotificationCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraNotificationCenterScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MayraNotificationCenterScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = MayraNotificationIntelligenceRuntime.store
    val privacyStore = remember(context) { NotificationPrivacyStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val records = remember(refresh) { store.snapshot() }
    val brief = remember(refresh) { store.summary() }
    val audit = remember(refresh) { MayraNotificationReplyRuntime.auditSnapshot().take(20) }
    var notice by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingNotificationReply?>(null) }
    var replyTarget by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }

    fun updatePolicy(record: MayraNotificationRecord, transform: (NotificationAppPolicy) -> NotificationAppPolicy, message: String) {
        privacyStore.save(transform(privacyStore.policyFor(record.sourcePackage)))
        notice = message
        refresh++
    }

    fun prepareReply(record: MayraNotificationRecord) {
        val result = MayraNotificationReplyRuntime.prepare(
            notificationId = record.id,
            replyText = replyText,
            policy = privacyStore.policyFor(record.sourcePackage),
            sensitivity = record.sensitivity
        )
        when (result) {
            is NotificationReplyResult.AwaitingConfirmation -> {
                pending = result.pending
                notice = "Confirm reply to ${record.appLabel}: ${result.pending.preview}"
            }
            is NotificationReplyResult.Blocked -> notice = result.message
            is NotificationReplyResult.Failed -> notice = result.message
            is NotificationReplyResult.Sent -> notice = result.message
        }
        refresh++
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Notification Center", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Private, grouped notification understanding. OTP and protected content are never shown here in plain text.")

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Unread brief", fontWeight = FontWeight.SemiBold)
                    Text("${brief.total} captured · ${brief.replyableCount} replyable · ${brief.sensitiveCount} protected")
                    if (brief.lines.isEmpty()) Text("No captured notifications yet.")
                    brief.lines.forEach { Text("• $it") }
                }
            }

            notice?.let { Card(Modifier.fillMaxWidth()) { Text(it, modifier = Modifier.padding(12.dp)) } }

            pending?.let { confirmation ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reply confirmation", fontWeight = FontWeight.SemiBold)
                        Text(confirmation.preview)
                        Text("This confirmation expires quickly. The source app controls final delivery.", style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    notice = when (val result = MayraNotificationReplyRuntime.confirm(context, confirmation.token)) {
                                        is NotificationReplyResult.Sent -> result.message
                                        is NotificationReplyResult.Blocked -> result.message
                                        is NotificationReplyResult.Failed -> result.message
                                        is NotificationReplyResult.AwaitingConfirmation -> "Reply still requires confirmation."
                                    }
                                    pending = null
                                    replyTarget = null
                                    replyText = ""
                                    refresh++
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Confirm send") }
                            OutlinedButton(
                                onClick = {
                                    notice = when (val result = MayraNotificationReplyRuntime.reject(confirmation.token)) {
                                        is NotificationReplyResult.Blocked -> result.message
                                        is NotificationReplyResult.Failed -> result.message
                                        else -> "Reply cancelled."
                                    }
                                    pending = null
                                    refresh++
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }

            records.forEach { record ->
                val policy = privacyStore.policyFor(record.sourcePackage)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(record.appLabel, fontWeight = FontWeight.SemiBold)
                            Text(record.sensitivity.name.lowercase())
                        }
                        Text("Privacy: ${policy.mode.name.lowercase()} · replies ${if (policy.allowReply) "on" else "off"} · read aloud ${if (policy.allowReadAloud) "on" else "off"}", style = MaterialTheme.typography.bodySmall)
                        record.title.takeIf(String::isNotBlank)?.let { Text(it, fontWeight = FontWeight.Medium) }
                        Text(record.text)

                        if (record.replyAvailable && policy.allowReply && policy.mode != NotificationPrivacyMode.IGNORE) {
                            if (replyTarget == record.id) {
                                OutlinedTextField(
                                    value = replyText,
                                    onValueChange = { replyText = it.take(1_000) },
                                    label = { Text("Reply") },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(onSend = { prepareReply(record) }),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { prepareReply(record) },
                                    enabled = replyText.isNotBlank() && pending == null,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Review reply") }
                            } else {
                                OutlinedButton(
                                    onClick = { replyTarget = record.id; replyText = "" },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Reply safely") }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { updatePolicy(record, { it.copy(mode = NotificationPrivacyMode.FULL) }, "Future ${record.appLabel} content can be shown after built-in sensitive redaction.") },
                                modifier = Modifier.weight(1f)
                            ) { Text("Full") }
                            OutlinedButton(
                                onClick = { updatePolicy(record, { it.copy(mode = NotificationPrivacyMode.REDACT_CONTENT) }, "Future ${record.appLabel} content will be hidden.") },
                                modifier = Modifier.weight(1f)
                            ) { Text("Hide") }
                            OutlinedButton(
                                onClick = { updatePolicy(record, { it.copy(mode = NotificationPrivacyMode.IGNORE, allowReply = false, allowReadAloud = false) }, "Future ${record.appLabel} notifications will be ignored.") },
                                modifier = Modifier.weight(1f)
                            ) { Text("Ignore") }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { updatePolicy(record, { it.copy(allowReply = !it.allowReply) }, "Replies ${if (policy.allowReply) "disabled" else "enabled"} for ${record.appLabel}.") },
                                enabled = policy.mode != NotificationPrivacyMode.IGNORE,
                                modifier = Modifier.weight(1f)
                            ) { Text(if (policy.allowReply) "Disable replies" else "Enable replies") }
                            OutlinedButton(
                                onClick = { updatePolicy(record, { it.copy(allowReadAloud = !it.allowReadAloud) }, "Private read-aloud ${if (policy.allowReadAloud) "disabled" else "enabled"} for ${record.appLabel}.") },
                                enabled = policy.mode != NotificationPrivacyMode.IGNORE && record.sensitivity != NotificationSensitivity.OTP,
                                modifier = Modifier.weight(1f)
                            ) { Text(if (policy.allowReadAloud) "Mute aloud" else "Allow aloud") }
                        }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Reply safety history", fontWeight = FontWeight.SemiBold)
                    if (audit.isEmpty()) Text("No notification replies have been processed yet.")
                    audit.forEach { event ->
                        Text("• ${event.status.name.replace('_', ' ').lowercase()} · ${event.sourcePackage.ifBlank { "unknown app" }} · ${event.detail}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = { MayraNotificationReplyRuntime.clearAudit(); notice = "Reply safety history cleared."; refresh++ },
                        enabled = audit.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear reply history") }
                }
            }

            OutlinedButton(
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Notification access settings") }
            OutlinedButton(
                onClick = { store.clear(); notice = "Captured notification view cleared."; refresh++ },
                enabled = records.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear captured notifications") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}