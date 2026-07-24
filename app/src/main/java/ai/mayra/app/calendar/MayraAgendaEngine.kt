package ai.mayra.app.calendar

import ai.mayra.app.reminder.MayraReminderRuntime
import ai.mayra.app.reminder.MayraReminderStore
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class AgendaEventState { SCHEDULED, COMPLETED, CANCELLED }
enum class AgendaRecurrence { NONE, DAILY, WEEKLY, MONTHLY }

data class MayraAgendaEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String? = null,
    val startsAt: Long,
    val endsAt: Long,
    val location: String? = null,
    val recurrence: AgendaRecurrence = AgendaRecurrence.NONE,
    val state: AgendaEventState = AgendaEventState.SCHEDULED,
    val createdAt: Long,
    val updatedAt: Long = createdAt
) {
    init { require(title.isNotBlank()); require(endsAt >= startsAt) }
}

class MayraAgendaStore(context: Context, private val maxEntries: Int = 500) {
    private val preferences = context.applicationContext.getSharedPreferences("mayra_owned_agenda", Context.MODE_PRIVATE)
    @Synchronized fun upsert(event: MayraAgendaEvent) { write(all().filterNot { it.id == event.id }.plus(event).sortedByDescending { it.updatedAt }.take(maxEntries)) }
    @Synchronized fun all(): List<MayraAgendaEvent> = runCatching {
        val array = JSONArray(preferences.getString(KEY_EVENTS, "[]"))
        buildList { repeat(array.length()) { add(array.getJSONObject(it).toEvent()) } }
    }.getOrDefault(emptyList())
    fun active(): List<MayraAgendaEvent> = all().filter { it.state == AgendaEventState.SCHEDULED }.sortedBy { it.startsAt }
    fun onDate(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<MayraAgendaEvent> = active().filter { Instant.ofEpochMilli(it.startsAt).atZone(zone).toLocalDate() == date }
    fun complete(id: String, now: Long = System.currentTimeMillis()) = update(id) { it.copy(state = AgendaEventState.COMPLETED, updatedAt = now) }
    fun cancel(id: String, now: Long = System.currentTimeMillis()) = update(id) { it.copy(state = AgendaEventState.CANCELLED, updatedAt = now) }
    fun move(id: String, startsAt: Long, endsAt: Long, now: Long = System.currentTimeMillis()) = update(id) { it.copy(startsAt = startsAt, endsAt = endsAt, updatedAt = now) }
    @Synchronized private fun update(id: String, transform: (MayraAgendaEvent) -> MayraAgendaEvent): MayraAgendaEvent? {
        val event = all().firstOrNull { it.id == id } ?: return null
        return transform(event).also(::upsert)
    }
    private fun write(events: List<MayraAgendaEvent>) {
        val array = JSONArray(); events.forEach { event -> array.put(JSONObject()
            .put("id", event.id).put("title", event.title).put("detail", event.detail)
            .put("startsAt", event.startsAt).put("endsAt", event.endsAt).put("location", event.location)
            .put("recurrence", event.recurrence.name).put("state", event.state.name)
            .put("createdAt", event.createdAt).put("updatedAt", event.updatedAt)) }
        preferences.edit().putString(KEY_EVENTS, array.toString()).apply()
    }
    private fun JSONObject.toEvent() = MayraAgendaEvent(
        id = getString("id"), title = getString("title"), detail = optString("detail").takeIf(String::isNotBlank),
        startsAt = getLong("startsAt"), endsAt = getLong("endsAt"), location = optString("location").takeIf(String::isNotBlank),
        recurrence = runCatching { AgendaRecurrence.valueOf(getString("recurrence")) }.getOrDefault(AgendaRecurrence.NONE),
        state = runCatching { AgendaEventState.valueOf(getString("state")) }.getOrDefault(AgendaEventState.SCHEDULED),
        createdAt = getLong("createdAt"), updatedAt = getLong("updatedAt")
    )
    private companion object { const val KEY_EVENTS = "events" }
}

sealed interface AgendaParseResult {
    data class Parsed(val event: MayraAgendaEvent) : AgendaParseResult
    data class NeedsClarification(val message: String) : AgendaParseResult
}

class MayraNaturalAgendaParser(private val zone: ZoneId = ZoneId.systemDefault()) {
    fun parse(raw: String, now: Long = System.currentTimeMillis()): AgendaParseResult {
        val clean = raw.trim().replace(Regex("\\s+"), " ")
        if (clean.isBlank()) return AgendaParseResult.NeedsClarification("What event should I add?")
        val normalized = clean.lowercase(Locale.ROOT)
        val current = Instant.ofEpochMilli(now).atZone(zone).toLocalDateTime()
        val date = when {
            normalized.contains("day after tomorrow") || normalized.contains("parso") -> current.toLocalDate().plusDays(2)
            normalized.contains("tomorrow") || normalized.contains("kal") -> current.toLocalDate().plusDays(1)
            normalized.contains("today") || normalized.contains("aaj") -> current.toLocalDate()
            else -> current.toLocalDate()
        }
        val timeMatch = Regex("(?i)\\b([01]?\\d|2[0-3])(?:[:.]([0-5]\\d))?\\s*(am|pm)?(?:\\s*(?:baje|बजे))?\\b").find(normalized)
            ?: return AgendaParseResult.NeedsClarification("What time should I add this event?")
        var hour = timeMatch.groupValues[1].toInt(); val minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
        when (timeMatch.groupValues[3].lowercase(Locale.ROOT)) { "pm" -> if (hour in 1..11) hour += 12; "am" -> if (hour == 12) hour = 0 }
        var start = LocalDateTime.of(date, LocalTime.of(hour, minute))
        if (!start.isAfter(current) && date == current.toLocalDate()) start = start.plusDays(1)
        val recurrence = when {
            normalized.contains("every day") || normalized.contains("daily") || normalized.contains("har din") -> AgendaRecurrence.DAILY
            normalized.contains("every week") || normalized.contains("weekly") || normalized.contains("har hafte") -> AgendaRecurrence.WEEKLY
            normalized.contains("every month") || normalized.contains("monthly") || normalized.contains("har mahine") -> AgendaRecurrence.MONTHLY
            else -> AgendaRecurrence.NONE
        }
        val title = clean
            .replace(timeMatch.value, " ")
            .replace(Regex("(?i)\\b(add|create|schedule|event|meeting|calendar|today|tomorrow|day after tomorrow|aaj|kal|parso|every day|daily|har din|every week|weekly|har hafte|every month|monthly|har mahine|at|ko|baje)\\b"), " ")
            .replace(Regex("\\s+"), " ").trim(' ', ',', '.', ':', '-')
            .ifBlank { "Calendar event" }
        val startMillis = start.atZone(zone).toInstant().toEpochMilli()
        return AgendaParseResult.Parsed(MayraAgendaEvent(title = title.take(160), detail = clean, startsAt = startMillis, endsAt = startMillis + Duration.ofHours(1).toMillis(), recurrence = recurrence, createdAt = now))
    }
}

object MayraAgendaRuntime {
    @Volatile private var appContext: Context? = null
    fun install(context: Context) { appContext = context.applicationContext }
    val installed: Boolean get() = appContext != null
    fun createEvent(raw: String, now: Long = System.currentTimeMillis()): String {
        val context = appContext ?: return "Mayra agenda is not ready yet."
        return when (val parsed = MayraNaturalAgendaParser().parse(raw, now)) {
            is AgendaParseResult.NeedsClarification -> parsed.message
            is AgendaParseResult.Parsed -> { MayraAgendaStore(context).upsert(parsed.event); "Event added: ${parsed.event.title} on ${format(parsed.event.startsAt)}." }
        }
    }
    fun rescheduleReminder(query: String, rawTime: String, now: Long = System.currentTimeMillis()): String {
        val context = appContext ?: return "Mayra agenda is not ready yet."
        val match = findReminder(context, query, now)
        if (match !is Match.One) return match.message(query)
        val parsed = ai.mayra.app.reminder.MayraReminderParser().parse(rawTime)
        if (parsed !is ai.mayra.app.reminder.ReminderParseResult.Parsed) return when (parsed) {
            is ai.mayra.app.reminder.ReminderParseResult.NeedsClarification -> parsed.message
            is ai.mayra.app.reminder.ReminderParseResult.Invalid -> parsed.message
            else -> "I could not understand the new reminder time."
        }
        val store = MayraReminderStore(context)
        val current = store.find(match.id) ?: return "Reminder no longer exists."
        val updated = current.copy(dueAt = parsed.dueAt, updatedAt = now, state = ai.mayra.app.reminder.ReminderState.SCHEDULED)
        store.upsert(updated); MayraReminderRuntime.schedule(context, updated, now)
        return "Moved ${match.name} to ${format(updated.dueAt)}."
    }
    fun todaySummary(now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
        val context = appContext ?: return "Mayra agenda is not ready yet."
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val reminders = MayraReminderStore(context).active(now).filter { Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalDate() == date }
        val events = MayraAgendaStore(context).onDate(date, zone)
        if (reminders.isEmpty() && events.isEmpty()) return "You have no Mayra reminders or events scheduled for today."
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        val lines = buildList {
            events.forEach { add("Event at ${Instant.ofEpochMilli(it.startsAt).atZone(zone).toLocalTime().format(formatter)}: ${it.title}.") }
            reminders.forEach { add("Reminder at ${Instant.ofEpochMilli(it.dueAt).atZone(zone).toLocalTime().format(formatter)}: ${it.title}.") }
        }
        return "Today you have ${events.size} events and ${reminders.size} reminders. ${lines.joinToString(" ")}"
    }
    fun upcomingSummary(now: Long = System.currentTimeMillis(), limit: Int = 5, zone: ZoneId = ZoneId.systemDefault()): String {
        val context = appContext ?: return "Mayra agenda is not ready yet."
        val items = buildList<Pair<Long, String>> {
            MayraAgendaStore(context).active().filter { it.startsAt >= now }.forEach { add(it.startsAt to "Event: ${it.title}") }
            MayraReminderStore(context).active(now).filter { it.dueAt >= now }.forEach { add(it.dueAt to "Reminder: ${it.title}") }
        }.sortedBy { it.first }.take(limit.coerceIn(1, 10))
        if (items.isEmpty()) return "There are no upcoming Mayra reminders or events."
        val formatter = DateTimeFormatter.ofPattern("EEE, d MMM h:mm a", Locale.ENGLISH)
        return items.joinToString(" ") { (time, label) -> "$label on ${Instant.ofEpochMilli(time).atZone(zone).format(formatter)}." }
    }
    fun completeReminder(query: String, now: Long = System.currentTimeMillis()) = operateReminder(query, now) { context, match -> MayraReminderRuntime.complete(context, match.id, now); "Completed reminder: ${match.name}." }
    fun snoozeReminder(query: String, minutes: Long = 10, now: Long = System.currentTimeMillis()) = operateReminder(query, now) { context, match -> MayraReminderRuntime.snooze(context, match.id, Duration.ofMinutes(minutes.coerceIn(1, 10_080)), now); "Snoozed ${match.name} for $minutes minutes." }
    fun cancelReminder(query: String, now: Long = System.currentTimeMillis()) = operateReminder(query, now) { context, match -> MayraReminderRuntime.cancel(context, match.id, now); "Cancelled reminder: ${match.name}." }
    private fun operateReminder(query: String, now: Long, action: (Context, Match.One) -> String): String {
        val context = appContext ?: return "Mayra agenda is not ready yet."
        return when (val match = findReminder(context, query, now)) { is Match.One -> action(context, match); else -> match.message(query) }
    }
    private fun findReminder(context: Context, query: String, now: Long): Match {
        val normalized = normalize(query)
        val candidates = MayraReminderStore(context).active(now).filter { val title = normalize(it.title); title == normalized || title.contains(normalized) || normalized.contains(title) }
        return when (candidates.size) { 0 -> Match.None; 1 -> Match.One(candidates.single().id, candidates.single().title); else -> Match.Ambiguous(candidates.map { it.title }) }
    }
    private fun Match.message(query: String) = when (this) { Match.None -> "I could not find an active reminder matching ‘${query.trim()}’."; is Match.Ambiguous -> "I found multiple matching reminders: ${names.joinToString()}. Please say the exact reminder."; is Match.One -> "" }
    private fun normalize(value: String) = value.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N} ]"), " ").replace(Regex("\\s+"), " ").trim()
    private fun format(value: Long) = DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a").format(Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()))
    private sealed interface Match { data object None : Match; data class One(val id: String, val name: String) : Match; data class Ambiguous(val names: List<String>) : Match }
}

class MayraAgendaEventParser(private val zone: ZoneId = ZoneId.systemDefault()) {
    fun create(title: String, date: LocalDate, time: LocalTime, durationMinutes: Long = 60, recurrence: AgendaRecurrence = AgendaRecurrence.NONE, now: Long = System.currentTimeMillis()): MayraAgendaEvent {
        val start = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
        return MayraAgendaEvent(title = title.trim().take(160), startsAt = start, endsAt = start + Duration.ofMinutes(durationMinutes.coerceIn(5, 1_440)).toMillis(), recurrence = recurrence, createdAt = now)
    }
}