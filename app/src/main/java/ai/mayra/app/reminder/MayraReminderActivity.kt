package ai.mayra.app.reminder

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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MayraReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                ReminderCenter(
                    focusId = intent.getStringExtra(EXTRA_ID),
                    onClose = ::finish
                )
            }
        }
    }

    companion object { const val EXTRA_ID = "reminder_id" }
}

@Composable
private fun ReminderCenter(focusId: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MayraReminderStore(context) }
    val parser = remember { MayraReminderParser() }
    var refresh by remember { mutableIntStateOf(0) }
    var input by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val reminders = remember(refresh) { store.all() }
    val active = reminders.filter { it.state in setOf(ReminderState.SCHEDULED, ReminderState.SNOOZED, ReminderState.DUE, ReminderState.MISSED) }
        .sortedBy { it.dueAt }
    val history = reminders.filter { it.state in setOf(ReminderState.COMPLETED, ReminderState.CANCELLED) }
        .sortedByDescending { it.updatedAt }

    fun create() {
        when (val result = parser.parse(input)) {
            is ReminderParseResult.Parsed -> {
                val reminder = MayraReminderRuntime.create(context, result)
                notice = "Reminder saved for ${formatDue(reminder.dueAt)}."
                input = ""
                refresh++
            }
            is ReminderParseResult.NeedsClarification -> notice = result.message
            is ReminderParseResult.Invalid -> notice = result.message
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Reminders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Mayra saves, schedules and follows up on reminders locally on this phone.")

            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(500) },
                label = { Text("Example: Medicine tomorrow at 8 PM") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { create() }),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = ::create, enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("Create reminder")
            }
            notice?.let { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) } }

            Text("Active", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (active.isEmpty()) Text("No active reminders.")
            active.forEach { reminder ->
                val focused = reminder.id == focusId
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(reminder.title, fontWeight = FontWeight.SemiBold)
                        Text("${reminder.state.name.lowercase()} · ${formatDue(reminder.dueAt)}")
                        reminder.detail?.takeIf { it != reminder.title }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        if (focused) Text("Opened from reminder notification", fontWeight = FontWeight.Medium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { MayraReminderRuntime.complete(context, reminder.id); notice = "Completed: ${reminder.title}"; refresh++ },
                                modifier = Modifier.weight(1f)
                            ) { Text("Complete") }
                            OutlinedButton(
                                onClick = { MayraReminderRuntime.snooze(context, reminder.id, Duration.ofMinutes(10)); notice = "Snoozed 10 minutes."; refresh++ },
                                modifier = Modifier.weight(1f)
                            ) { Text("Snooze") }
                        }
                        OutlinedButton(
                            onClick = { MayraReminderRuntime.cancel(context, reminder.id); notice = "Cancelled: ${reminder.title}"; refresh++ },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Cancel") }
                    }
                }
            }

            Text("History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (history.isEmpty()) Text("No completed or cancelled reminders yet.")
            history.take(50).forEach { reminder ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(reminder.title, fontWeight = FontWeight.Medium)
                            Text(reminder.state.name.lowercase(), style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = { store.delete(reminder.id); refresh++ }) { Text("Delete") }
                    }
                }
            }

            OutlinedButton(onClick = { MayraReminderRuntime.rescheduleAll(context); notice = "Reminder schedule refreshed." }, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh reminder schedule")
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

private fun formatDue(value: Long): String = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")
    .format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
