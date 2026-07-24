package ai.mayra.app.reminder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

enum class ReminderState { SCHEDULED, DUE, SNOOZED, COMPLETED, CANCELLED, MISSED }
enum class ReminderPriority { NORMAL, IMPORTANT }

data class MayraReminder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val detail: String? = null,
    val dueAt: Long,
    val createdAt: Long,
    val updatedAt: Long = createdAt,
    val state: ReminderState = ReminderState.SCHEDULED,
    val priority: ReminderPriority = ReminderPriority.NORMAL,
    val followUpEnabled: Boolean = true,
    val notificationCount: Int = 0,
    val lastNotifiedAt: Long? = null
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank())
        require(dueAt >= 0L)
        require(createdAt >= 0L)
    }
}

sealed interface ReminderParseResult {
    data class Parsed(val title: String, val detail: String?, val dueAt: Long) : ReminderParseResult
    data class NeedsClarification(val message: String) : ReminderParseResult
    data class Invalid(val message: String) : ReminderParseResult
}

class MayraReminderParser(
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun parse(raw: String): ReminderParseResult {
        val original = raw.trim().replace(Regex("\\s+"), " ")
        if (original.isBlank()) return ReminderParseResult.Invalid("What should I remind you about?")
        val normalized = original.lowercase(Locale.ROOT)
        val now = LocalDateTime.ofInstant(clock.instant(), zoneId)

        parseRelativeMinutes(normalized, now)?.let { due ->
            return parsedWithTimeRemoved(original, RELATIVE_MINUTES, due)
        }
        parseRelativeHours(normalized, now)?.let { due ->
            return parsedWithTimeRemoved(original, RELATIVE_HOURS, due)
        }

        val day = when {
            normalized.containsAny("day after tomorrow", "parso", "परसों") -> now.toLocalDate().plusDays(2)
            normalized.containsAny("tomorrow", "kal", "कल") -> now.toLocalDate().plusDays(1)
            normalized.containsAny("today", "aaj", "आज") -> now.toLocalDate()
            else -> null
        }
        val time = parseClockTime(normalized)
        if (day != null && time == null) {
            return ReminderParseResult.NeedsClarification("What time should I remind you?")
        }
        if (day != null && time != null) {
            var due = LocalDateTime.of(day, time)
            if (!due.isAfter(now)) due = due.plusDays(1)
            return parsedWithTimeRemoved(original, DAY_AND_TIME, due)
        }

        if (time != null) {
            var due = LocalDateTime.of(now.toLocalDate(), time)
            if (!due.isAfter(now)) due = due.plusDays(1)
            return parsedWithTimeRemoved(original, CLOCK_TIME, due)
        }

        return ReminderParseResult.NeedsClarification(
            "I understood the reminder, but not the time. Try ‘in 20 minutes’, ‘tomorrow at 8 PM’, or ‘kal subah 7 baje’."
        )
    }

    private fun parseRelativeMinutes(text: String, now: LocalDateTime): LocalDateTime? {
        val match = RELATIVE_MINUTES.find(text) ?: return null
        val amount = match.groupValues[1].toLongOrNull()?.coerceIn(1, 43_200) ?: return null
        return now.plusMinutes(amount)
    }

    private fun parseRelativeHours(text: String, now: LocalDateTime): LocalDateTime? {
        val match = RELATIVE_HOURS.find(text) ?: return null
        val amount = match.groupValues[1].toLongOrNull()?.coerceIn(1, 720) ?: return null
        return now.plusHours(amount)
    }

    private fun parseClockTime(text: String): LocalTime? {
        val match = CLOCK_TIME.find(text)
        if (match != null) {
            val rawMatch = match.value.lowercase(Locale.ROOT)
            val hourToken = match.groupValues[1]
            val hasTimeSignal = rawMatch.contains(':') || rawMatch.contains('.') ||
                rawMatch.contains("am") || rawMatch.contains("pm") ||
                rawMatch.contains("baje") || rawMatch.contains("बजे") ||
                text.containsAny("at $hourToken", "ko $hourToken", "subah", "सुबह", "shaam", "शाम", "raat", "रात", "evening", "morning", "night", "afternoon")
            if (!hasTimeSignal) return null

            var hour = hourToken.toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val marker = match.groupValues[3].lowercase(Locale.ROOT)
            if (hour !in 0..23 || minute !in 0..59) return null
            if (marker == "pm" && hour in 1..11) hour += 12
            if (marker == "am" && hour == 12) hour = 0
            if (marker.isBlank() && hour in 1..11) {
                when {
                    text.containsAny("evening", "shaam", "शाम", "night", "raat", "रात") -> hour += 12
                    text.containsAny("afternoon", "dopahar", "दोपहर") -> hour += 12
                }
            }
            return LocalTime.of(hour, minute)
        }
        return when {
            text.containsAny("morning", "subah", "सुबह") -> LocalTime.of(8, 0)
            text.containsAny("afternoon", "dopahar", "दोपहर") -> LocalTime.of(15, 0)
            text.containsAny("evening", "shaam", "शाम") -> LocalTime.of(19, 0)
            text.containsAny("night", "raat", "रात") -> LocalTime.of(21, 0)
            else -> null
        }
    }

    private fun parsedWithTimeRemoved(original: String, pattern: Regex, due: LocalDateTime): ReminderParseResult.Parsed {
        val title = original
            .replace(pattern, " ")
            .replace(DAY_WORDS, " ")
            .replace(TIME_FILLERS, " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', ':', '-')
            .ifBlank { "Reminder" }
        return ReminderParseResult.Parsed(
            title = title.take(160),
            detail = original.takeIf { it != title }?.take(500),
            dueAt = due.atZone(zoneId).toInstant().toEpochMilli()
        )
    }

    private companion object {
        val RELATIVE_MINUTES = Regex("(?i)\\b(?:in\\s+)?(\\d{1,5})\\s*(?:minute|minutes|min|mins|मिनट)(?:\\s+(?:mein|me|में|ke\\s+baad|के\\s+बाद|baad|बाद))?\\b")
        val RELATIVE_HOURS = Regex("(?i)\\b(?:in\\s+)?(\\d{1,3})\\s*(?:hour|hours|hr|hrs|ghante|घंटे)(?:\\s+(?:mein|me|में|ke\\s+baad|के\\s+बाद|baad|बाद))?\\b")
        val CLOCK_TIME = Regex("(?i)\\b([01]?\\d|2[0-3])(?:[:.]([0-5]\\d))?\\s*(am|pm)?(?:\\s*(?:baje|बजे))?\\b")
        val DAY_WORDS = Regex("(?i)\\b(day after tomorrow|tomorrow|today|parso|kal|aaj|परसों|कल|आज)\\b")
        val DAY_AND_TIME = Regex("(?i)\\b(day after tomorrow|tomorrow|today|parso|kal|aaj|परसों|कल|आज)\\b.*?\\b([01]?\\d|2[0-3])(?:[:.]([0-5]\\d))?\\s*(am|pm)?(?:\\s*(?:baje|बजे))?\\b")
        val TIME_FILLERS = Regex("(?i)\\b(at|ko|में|me|mein|baje|बजे|morning|subah|सुबह|afternoon|dopahar|दोपहर|evening|shaam|शाम|night|raat|रात|ka|ki|ke)\\b")
    }
}

class MayraReminderStore(context: Context, private val maxEntries: Int = 500) {
    private val preferences = context.applicationContext.getSharedPreferences("mayra_owned_reminders", Context.MODE_PRIVATE)

    @Synchronized
    fun upsert(reminder: MayraReminder) {
        val values = all().filterNot { it.id == reminder.id }.plus(reminder)
            .sortedByDescending(MayraReminder::updatedAt)
            .take(maxEntries)
        write(values)
    }

    @Synchronized fun find(id: String): MayraReminder? = all().firstOrNull { it.id == id }
    @Synchronized fun delete(id: String): Boolean {
        val current = all()
        val next = current.filterNot { it.id == id }
        if (next.size == current.size) return false
        write(next)
        return true
    }

    @Synchronized fun all(): List<MayraReminder> = runCatching {
        val array = JSONArray(preferences.getString(KEY_ITEMS, "[]"))
        buildList {
            repeat(array.length()) { index -> add(array.getJSONObject(index).toReminder()) }
        }
    }.getOrDefault(emptyList())

    fun active(now: Long = System.currentTimeMillis()): List<MayraReminder> = all()
        .filter { it.state in setOf(ReminderState.SCHEDULED, ReminderState.SNOOZED, ReminderState.DUE, ReminderState.MISSED) }
        .sortedBy { it.dueAt }

    fun due(now: Long = System.currentTimeMillis()): List<MayraReminder> = active(now).filter { it.dueAt <= now }
    fun complete(id: String, now: Long = System.currentTimeMillis()): MayraReminder? = update(id) {
        it.copy(state = ReminderState.COMPLETED, updatedAt = now)
    }
    fun cancel(id: String, now: Long = System.currentTimeMillis()): MayraReminder? = update(id) {
        it.copy(state = ReminderState.CANCELLED, updatedAt = now)
    }
    fun snooze(id: String, duration: Duration, now: Long = System.currentTimeMillis()): MayraReminder? = update(id) {
        it.copy(state = ReminderState.SNOOZED, dueAt = now + duration.toMillis(), updatedAt = now)
    }
    fun markNotified(id: String, now: Long = System.currentTimeMillis()): MayraReminder? = update(id) {
        it.copy(state = ReminderState.DUE, updatedAt = now, notificationCount = it.notificationCount + 1, lastNotifiedAt = now)
    }
    fun markMissed(id: String, now: Long = System.currentTimeMillis()): MayraReminder? = update(id) {
        it.copy(state = ReminderState.MISSED, updatedAt = now)
    }

    @Synchronized
    private fun update(id: String, transform: (MayraReminder) -> MayraReminder): MayraReminder? {
        val item = find(id) ?: return null
        val updated = transform(item)
        upsert(updated)
        return updated
    }

    private fun write(items: List<MayraReminder>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    private fun MayraReminder.toJson() = JSONObject()
        .put("id", id).put("title", title).put("detail", detail)
        .put("dueAt", dueAt).put("createdAt", createdAt).put("updatedAt", updatedAt)
        .put("state", state.name).put("priority", priority.name)
        .put("followUp", followUpEnabled).put("notificationCount", notificationCount)
        .put("lastNotifiedAt", lastNotifiedAt)

    private fun JSONObject.toReminder() = MayraReminder(
        id = getString("id"), title = getString("title"), detail = optString("detail").takeIf(String::isNotBlank),
        dueAt = getLong("dueAt"), createdAt = getLong("createdAt"), updatedAt = getLong("updatedAt"),
        state = runCatching { ReminderState.valueOf(getString("state")) }.getOrDefault(ReminderState.SCHEDULED),
        priority = runCatching { ReminderPriority.valueOf(getString("priority")) }.getOrDefault(ReminderPriority.NORMAL),
        followUpEnabled = optBoolean("followUp", true), notificationCount = optInt("notificationCount", 0),
        lastNotifiedAt = if (isNull("lastNotifiedAt")) null else optLong("lastNotifiedAt")
    )

    private companion object { const val KEY_ITEMS = "items" }
}

private fun String.containsAny(vararg values: String): Boolean = values.any { contains(it, ignoreCase = true) }
