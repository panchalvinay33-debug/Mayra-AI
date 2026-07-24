package ai.mayra.app.calendar

import ai.mayra.app.reminder.MayraReminderRuntime
import ai.mayra.app.reminder.MayraReminderStore
import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MayraAgendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MayraAgendaRuntime.install(applicationContext)
        setContent { MayraAITheme { AgendaScreen(onClose = ::finish) } }
    }
}

@Composable
private fun AgendaScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var eventText by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val eventStore = remember(context) { MayraAgendaStore(context) }
    val reminderStore = remember(context) { MayraReminderStore(context) }
    val events = remember(refresh) { eventStore.all() }
    val reminders = remember(refresh) { reminderStore.all() }
    val formatter = remember { DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a") }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra Personal Agenda", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Private reminders and events stay on this phone unless you explicitly export them.")
            notice?.let { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) } }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add event naturally", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = eventText,
                        onValueChange = { eventText = it.take(500) },
                        label = { Text("Example: Tomorrow 4 PM team meeting") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { notice = MayraAgendaRuntime.createEvent(eventText); if (!notice.orEmpty().contains("What")) eventText = ""; refresh++ },
                        enabled = eventText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Add to Mayra agenda") }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Today", fontWeight = FontWeight.SemiBold)
                    Text(MayraAgendaRuntime.todaySummary())
                    Text("Upcoming", fontWeight = FontWeight.SemiBold)
                    Text(MayraAgendaRuntime.upcomingSummary())
                }
            }

            Text("Active events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            events.filter { it.state == AgendaEventState.SCHEDULED }.sortedBy { it.startsAt }.forEach { event ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(event.title, fontWeight = FontWeight.SemiBold)
                        Text(formatter.format(Instant.ofEpochMilli(event.startsAt).atZone(ZoneId.systemDefault())))
                        Text("Recurrence: ${event.recurrence.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { eventStore.complete(event.id); notice = "Event completed."; refresh++ }, modifier = Modifier.weight(1f)) { Text("Complete") }
                            OutlinedButton(onClick = { eventStore.cancel(event.id); notice = "Event cancelled."; refresh++ }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                                    .putExtra(CalendarContract.Events.TITLE, event.title)
                                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startsAt)
                                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endsAt)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Review in Android Calendar") }
                    }
                }
            }

            Text("Active reminders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            reminders.filter { it.state.name in setOf("SCHEDULED", "SNOOZED", "DUE", "MISSED") }.sortedBy { it.dueAt }.forEach { reminder ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reminder.title, fontWeight = FontWeight.SemiBold)
                        Text(formatter.format(Instant.ofEpochMilli(reminder.dueAt).atZone(ZoneId.systemDefault())))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { MayraReminderRuntime.complete(context, reminder.id); notice = "Reminder completed."; refresh++ }, modifier = Modifier.weight(1f)) { Text("Complete") }
                            OutlinedButton(onClick = { MayraReminderRuntime.snooze(context, reminder.id, Duration.ofMinutes(10)); notice = "Snoozed 10 minutes."; refresh++ }, modifier = Modifier.weight(1f)) { Text("Snooze") }
                            OutlinedButton(onClick = { MayraReminderRuntime.cancel(context, reminder.id); notice = "Reminder cancelled."; refresh++ }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        }
                    }
                }
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}