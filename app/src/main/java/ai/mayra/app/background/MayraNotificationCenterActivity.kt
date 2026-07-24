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
        setContent {
            MayraAITheme {
                MayraNotificationCenterScreen(onClose = ::finish)
            }
        }
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
    var notice by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<PendingNotificationReply?>(null) }
    var replyTarget by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }

    fun prepareReply(record: MayraNotificationRecord) {
        val result = MayraNotificationReplyRuntime.prepare(
            notificationId = record.id,
            replyText = replyText,
            policy = privacyStore.policyFor(record.sourcePackage)
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
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
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

            notice?.let {
                Card(Modifier.fillMaxWidth()) {
                    Text(it, modifier = Modifier.padding(12.dp))
                }
            }

            pending?.let { confirmation ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Reply confirmation", fontWeight = FontWeight.SemiBold)
                        Text(confirmation.preview)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    when (val result = MayraNotificationReplyRuntime.confirm(context, confirmation.token)) {
                                        is NotificationReplyResult.Sent -> notice = result.message
                                        is NotificationReplyResult.Blocked -> notice = result.message
                                        is NotificationReplyResult.Failed -> notice = result.message
                                        is NotificationReplyResult.AwaitingConfirmation -> Unit
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
                                    val result = MayraNotificationReplyRuntime.reject(confirmation.token)
                                    notice = when (result) {
                                        is NotificationReplyResult.Blocked -> result.message
                                        is NotificationReplyResult.Failed -> result.message
                                        else -> "Reply cancelled."
                                    }
                                    pending = null
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }

            records.forEach { record ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(record.appLabel, fontWeight = FontWeight.SemiBold)
                            Text(record.sensitivity.name.lowercase())
                        }
                        record.title.takeIf(String::isNotBlank)?.let { Text(it, fontWeight = FontWeight.Medium) }
                        Text(record.text)
                        if (record.replyAvailable) {
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
                                    onClick = {
                                        replyTarget = record.id
                                        replyText = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Reply safely") }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val current = privacyStore.policyFor(record.sourcePackage)
                                    privacyStore.save(current.copy(mode = NotificationPrivacyMode.REDACT_CONTENT))
                                    notice = "Future ${record.appLabel} notification content will be hidden."
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Hide content") }
                            OutlinedButton(
                                onClick = {
                                    val current = privacyStore.policyFor(record.sourcePackage)
                                    privacyStore.save(current.copy(mode = NotificationPrivacyMode.IGNORE, allowReply = false))
                                    notice = "Future ${record.appLabel} notifications will be ignored."
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Ignore app") }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Notification access settings") }
            OutlinedButton(
                onClick = {
                    store.clear()
                    notice = "Captured notification view cleared."
                    refresh++
                },
                enabled = records.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Clear captured notifications") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
