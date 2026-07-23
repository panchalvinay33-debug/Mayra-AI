package ai.mayra.app.personal

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.math.roundToInt

enum class PersonalSignalType {
    APP_OPENED,
    COMMAND_USED,
    CONTACT_INTERACTION,
    REMINDER_CREATED,
    REMINDER_COMPLETED,
    NOTE_CREATED,
    TASK_COMPLETED,
    MEETING,
    WAKE_SESSION,
    CUSTOM
}

data class PersonalSignal(
    val id: String = UUID.randomUUID().toString(),
    val type: PersonalSignalType,
    val key: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMillis: Long = 0L,
    val successful: Boolean = true,
    val attributes: Map<String, String> = emptyMap(),
    val sensitive: Boolean = false
) {
    init {
        require(key.isNotBlank())
        require(durationMillis >= 0)
        require(attributes.size <= 30)
    }
}

data class HabitPattern(
    val id: String,
    val type: PersonalSignalType,
    val key: String,
    val preferredHour: Int,
    val preferredDays: Set<DayOfWeek>,
    val observationCount: Int,
    val successfulCount: Int,
    val confidence: Double,
    val averageDurationMillis: Long,
    val lastObservedAt: Long,
    val sensitive: Boolean
) {
    init {
        require(preferredHour in 0..23)
        require(observationCount > 0)
        require(successfulCount in 0..observationCount)
        require(confidence in 0.0..1.0)
    }
}

enum class ProactiveSuggestionType {
    ROUTINE,
    FOLLOW_UP,
    REMINDER,
    PRODUCTIVITY,
    SAFETY,
    CONTINUE_TASK
}

enum class SuggestionDisposition { SHOW, ASK, DEFER, SUPPRESS }

data class ProactiveSuggestion(
    val id: String = UUID.randomUUID().toString(),
    val type: ProactiveSuggestionType,
    val title: String,
    val explanation: String,
    val actionKey: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    val score: Double,
    val disposition: SuggestionDisposition,
    val expiresAt: Long,
    val sensitive: Boolean = false
) {
    init {
        require(title.isNotBlank())
        require(score in 0.0..1.0)
        require(parameters.size <= 20)
    }
}

data class PersonalAssistantContext(
    val now: Long = System.currentTimeMillis(),
    val zoneId: ZoneId = ZoneId.systemDefault(),
    val userAvailable: Boolean = true,
    val quietHours: Boolean = false,
    val batteryPercent: Int? = null,
    val charging: Boolean = false,
    val pendingReminderCount: Int = 0,
    val pendingTaskCount: Int = 0,
    val nextEventTitle: String? = null,
    val nextEventAt: Long? = null
) {
    init {
        batteryPercent?.let { require(it in 0..100) }
        require(pendingReminderCount >= 0)
        require(pendingTaskCount >= 0)
    }
}

class PersonalHabitEngine(
    private val maxSignals: Int = 2_000,
    private val patternWindowDays: Int = 45
) {
    private val signals = ArrayDeque<PersonalSignal>()

    @Synchronized
    fun record(signal: PersonalSignal) {
        signals += signal.copy(
            key = normalizeKey(signal.key),
            attributes = signal.attributes.entries.take(30).associate { normalizeKey(it.key) to it.value.trim().take(500) }
        )
        while (signals.size > maxSignals) signals.removeFirst()
    }

    @Synchronized
    fun recordAll(items: Collection<PersonalSignal>) = items.forEach(::record)

    @Synchronized
    fun recent(limit: Int = 100, includeSensitive: Boolean = false): List<PersonalSignal> = signals
        .asSequence()
        .filter { includeSensitive || !it.sensitive }
        .takeLast(limit.coerceIn(1, maxSignals))
        .toList()
        .sortedByDescending(PersonalSignal::timestamp)

    @Synchronized
    fun inferPatterns(
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        minimumObservations: Int = 3,
        includeSensitive: Boolean = false
    ): List<HabitPattern> {
        require(minimumObservations in 2..50)
        val cutoff = now - patternWindowDays * DAY_MS
        return signals.asSequence()
            .filter { it.timestamp >= cutoff }
            .filter { includeSensitive || !it.sensitive }
            .groupBy { it.type to it.key }
            .mapNotNull { (group, observations) ->
                if (observations.size < minimumObservations) return@mapNotNull null
                val hourCounts = observations.groupingBy { hourOf(it.timestamp, zoneId) }.eachCount()
                val preferredHour = hourCounts.maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { -it.key })?.key ?: 12
                val dayCounts = observations.groupingBy { dayOfWeek(it.timestamp, zoneId) }.eachCount()
                val dayThreshold = maxOf(2, observations.size / 4)
                val preferredDays = dayCounts.filterValues { it >= dayThreshold }.keys
                val timingConsistency = hourCounts.getValue(preferredHour).toDouble() / observations.size
                val frequencyScore = (observations.size / 12.0).coerceAtMost(1.0)
                val successRate = observations.count(PersonalSignal::successful).toDouble() / observations.size
                val confidence = (timingConsistency * 0.45 + frequencyScore * 0.30 + successRate * 0.25).coerceIn(0.0, 1.0)
                HabitPattern(
                    id = "${group.first.name}:${group.second}",
                    type = group.first,
                    key = group.second,
                    preferredHour = preferredHour,
                    preferredDays = preferredDays,
                    observationCount = observations.size,
                    successfulCount = observations.count(PersonalSignal::successful),
                    confidence = confidence,
                    averageDurationMillis = observations.map(PersonalSignal::durationMillis).average().roundToInt().toLong(),
                    lastObservedAt = observations.maxOf(PersonalSignal::timestamp),
                    sensitive = observations.any(PersonalSignal::sensitive)
                )
            }
            .sortedWith(compareByDescending<HabitPattern> { it.confidence }.thenByDescending { it.observationCount })
    }

    fun suggestions(
        context: PersonalAssistantContext,
        patterns: List<HabitPattern> = inferPatterns(context.now, context.zoneId),
        limit: Int = 8
    ): List<ProactiveSuggestion> {
        require(limit in 1..30)
        val currentHour = hourOf(context.now, context.zoneId)
        val currentDay = dayOfWeek(context.now, context.zoneId)
        val result = mutableListOf<ProactiveSuggestion>()

        patterns.filter { pattern ->
            pattern.confidence >= 0.58 &&
                circularHourDistance(pattern.preferredHour, currentHour) <= 1 &&
                (pattern.preferredDays.isEmpty() || currentDay in pattern.preferredDays)
        }.take(12).forEach { pattern ->
            val recencyDays = ((context.now - pattern.lastObservedAt).coerceAtLeast(0) / DAY_MS).toInt()
            val score = (pattern.confidence * 0.75 + (1.0 - (recencyDays / 14.0).coerceAtMost(1.0)) * 0.25).coerceIn(0.0, 1.0)
            result += ProactiveSuggestion(
                type = ProactiveSuggestionType.ROUTINE,
                title = routineTitle(pattern),
                explanation = "Aapne ye routine ${pattern.observationCount} baar lagbhag isi samay use ki hai.",
                actionKey = actionFor(pattern),
                parameters = mapOf("key" to pattern.key),
                score = score,
                disposition = disposition(context, pattern.sensitive, score),
                expiresAt = context.now + 90 * 60 * 1000L,
                sensitive = pattern.sensitive
            )
        }

        if (context.batteryPercent != null && context.batteryPercent <= 20 && !context.charging) {
            result += ProactiveSuggestion(
                type = ProactiveSuggestionType.SAFETY,
                title = "Battery ${context.batteryPercent}% hai",
                explanation = "Charger lagana ya battery settings dekhna useful ho sakta hai.",
                actionKey = "device.open_battery_settings",
                score = if (context.batteryPercent <= 10) 0.96 else 0.82,
                disposition = disposition(context, false, 0.9),
                expiresAt = context.now + 45 * 60 * 1000L
            )
        }

        if (context.pendingReminderCount > 0) {
            val score = (0.60 + context.pendingReminderCount.coerceAtMost(5) * 0.06).coerceAtMost(0.90)
            result += ProactiveSuggestion(
                type = ProactiveSuggestionType.REMINDER,
                title = "${context.pendingReminderCount} reminder pending hain",
                explanation = "Mayra pending reminders ka quick review dikha sakti hai.",
                actionKey = "personal.show_reminders",
                score = score,
                disposition = disposition(context, false, score),
                expiresAt = context.now + 3 * 60 * 60 * 1000L
            )
        }

        context.nextEventAt?.takeIf { it in (context.now + 1)..(context.now + 2 * 60 * 60 * 1000L) }?.let { eventAt ->
            val minutes = ((eventAt - context.now) / 60_000L).coerceAtLeast(1)
            result += ProactiveSuggestion(
                type = ProactiveSuggestionType.FOLLOW_UP,
                title = context.nextEventTitle ?: "Upcoming event",
                explanation = "Ye event lagbhag $minutes minute me hai.",
                actionKey = "personal.show_calendar",
                score = 0.88,
                disposition = disposition(context, false, 0.88),
                expiresAt = eventAt
            )
        }

        return result
            .distinctBy { listOf(it.type, it.title.lowercase(), it.actionKey) }
            .filterNot { it.disposition == SuggestionDisposition.SUPPRESS }
            .sortedByDescending(ProactiveSuggestion::score)
            .take(limit)
    }

    @Synchronized
    fun clear() = signals.clear()

    private fun disposition(context: PersonalAssistantContext, sensitive: Boolean, score: Double): SuggestionDisposition = when {
        sensitive -> SuggestionDisposition.ASK
        context.quietHours -> SuggestionDisposition.DEFER
        !context.userAvailable -> SuggestionDisposition.DEFER
        score < 0.50 -> SuggestionDisposition.SUPPRESS
        else -> SuggestionDisposition.SHOW
    }

    private fun routineTitle(pattern: HabitPattern): String = when (pattern.type) {
        PersonalSignalType.APP_OPENED -> "${pattern.key} kholna hai?"
        PersonalSignalType.CONTACT_INTERACTION -> "${pattern.key} se follow-up karein?"
        PersonalSignalType.COMMAND_USED -> "Routine ‘${pattern.key}’ chalayein?"
        PersonalSignalType.REMINDER_CREATED -> "${pattern.key} ka reminder lagayein?"
        else -> "${pattern.key} continue karein?"
    }

    private fun actionFor(pattern: HabitPattern): String = when (pattern.type) {
        PersonalSignalType.APP_OPENED -> "device.open_app"
        PersonalSignalType.CONTACT_INTERACTION -> "communication.open_contact"
        PersonalSignalType.REMINDER_CREATED -> "personal.create_reminder"
        else -> "personal.repeat_routine"
    }

    private fun normalizeKey(value: String): String = value.trim().replace(Regex("\\s+"), " ").take(160)
    private fun hourOf(timestamp: Long, zoneId: ZoneId) = Instant.ofEpochMilli(timestamp).atZone(zoneId).hour
    private fun dayOfWeek(timestamp: Long, zoneId: ZoneId) = Instant.ofEpochMilli(timestamp).atZone(zoneId).dayOfWeek
    private fun circularHourDistance(a: Int, b: Int): Int = minOf(kotlin.math.abs(a - b), 24 - kotlin.math.abs(a - b))

    private companion object { const val DAY_MS = 24L * 60 * 60 * 1000 }
}

enum class BriefingPeriod { MORNING, AFTERNOON, EVENING, NIGHT }

data class BriefingItem(
    val title: String,
    val detail: String,
    val priority: Int,
    val actionKey: String? = null
) {
    init { require(priority in 1..5) }
}

data class PersonalBriefing(
    val period: BriefingPeriod,
    val greeting: String,
    val summary: String,
    val items: List<BriefingItem>,
    val generatedAt: Long
)

class MayraBriefingEngine {
    fun build(
        context: PersonalAssistantContext,
        suggestions: List<ProactiveSuggestion>,
        completedToday: Int = 0,
        focusMinutesToday: Int = 0
    ): PersonalBriefing {
        require(completedToday >= 0 && focusMinutesToday >= 0)
        val hour = Instant.ofEpochMilli(context.now).atZone(context.zoneId).hour
        val period = when (hour) {
            in 5..11 -> BriefingPeriod.MORNING
            in 12..16 -> BriefingPeriod.AFTERNOON
            in 17..21 -> BriefingPeriod.EVENING
            else -> BriefingPeriod.NIGHT
        }
        val items = buildList {
            context.nextEventTitle?.let { title ->
                add(BriefingItem(title, "Calendar ka agla event", 5, "personal.show_calendar"))
            }
            if (context.pendingReminderCount > 0) add(
                BriefingItem("Pending reminders", "${context.pendingReminderCount} reminder review karne hain", 4, "personal.show_reminders")
            )
            if (context.pendingTaskCount > 0) add(
                BriefingItem("Aaj ke tasks", "${context.pendingTaskCount} task pending hain", 4, "personal.show_tasks")
            )
            suggestions.filter { it.disposition == SuggestionDisposition.SHOW }.take(3).forEach {
                add(BriefingItem(it.title, it.explanation, (it.score * 5).roundToInt().coerceIn(1, 5), it.actionKey))
            }
            if (period == BriefingPeriod.EVENING && (completedToday > 0 || focusMinutesToday > 0)) add(
                BriefingItem("Aaj ki progress", "$completedToday task complete hue aur $focusMinutesToday focus minutes record hue", 3)
            )
        }.distinctBy { it.title.lowercase() }.sortedByDescending(BriefingItem::priority).take(8)

        val summary = when {
            items.isEmpty() -> "Abhi koi urgent item nahi hai."
            items.size == 1 -> "Aaj ek important item hai."
            else -> "${items.size} useful updates ready hain."
        }
        return PersonalBriefing(period, greeting(period), summary, items, context.now)
    }

    private fun greeting(period: BriefingPeriod): String = when (period) {
        BriefingPeriod.MORNING -> "Good morning. Aaj ka plan dekhte hain."
        BriefingPeriod.AFTERNOON -> "Good afternoon. Chaliye progress check karte hain."
        BriefingPeriod.EVENING -> "Good evening. Aaj ka quick recap ready hai."
        BriefingPeriod.NIGHT -> "Raat ka quiet summary ready hai."
    }
}

data class ProductivityInputs(
    val tasksCreated: Int,
    val tasksCompleted: Int,
    val remindersDue: Int,
    val remindersCompleted: Int,
    val focusMinutes: Int,
    val interruptions: Int,
    val overdueItems: Int,
    val routineConsistency: Double
) {
    init {
        require(listOf(tasksCreated, tasksCompleted, remindersDue, remindersCompleted, focusMinutes, interruptions, overdueItems).all { it >= 0 })
        require(routineConsistency in 0.0..1.0)
    }
}

data class PersonalDashboardSnapshot(
    val productivityScore: Int,
    val completionRate: Double,
    val reminderReliability: Double,
    val focusScore: Double,
    val consistencyScore: Double,
    val attentionPenalty: Double,
    val headline: String,
    val insights: List<String>
)

class MayraPersonalDashboard {
    fun calculate(input: ProductivityInputs): PersonalDashboardSnapshot {
        val completionRate = ratio(input.tasksCompleted, input.tasksCreated)
        val reminderReliability = ratio(input.remindersCompleted, input.remindersDue)
        val focusScore = (input.focusMinutes / 120.0).coerceIn(0.0, 1.0)
        val consistency = input.routineConsistency
        val attentionPenalty = ((input.interruptions * 0.025) + (input.overdueItems * 0.08)).coerceAtMost(0.45)
        val raw = completionRate * 0.35 + reminderReliability * 0.20 + focusScore * 0.20 + consistency * 0.25 - attentionPenalty
        val score = (raw.coerceIn(0.0, 1.0) * 100).roundToInt()
        val insights = buildList {
            if (completionRate >= 0.8) add("Task completion strong hai.")
            else if (input.tasksCreated > 0) add("Pending tasks ko chhote steps me todna useful ho sakta hai.")
            if (reminderReliability < 0.6 && input.remindersDue >= 3) add("Reminder timing ya frequency adjust karni chahiye.")
            if (input.focusMinutes < 30) add("Ek short focus block productivity improve kar sakta hai.")
            if (input.overdueItems > 0) add("${input.overdueItems} overdue item pehle review karein.")
            if (consistency >= 0.75) add("Routine consistency achhi hai.")
        }.take(5)
        val headline = when {
            score >= 85 -> "Excellent momentum"
            score >= 70 -> "Strong day"
            score >= 50 -> "Steady progress"
            else -> "Reset and simplify"
        }
        return PersonalDashboardSnapshot(score, completionRate, reminderReliability, focusScore, consistency, attentionPenalty, headline, insights)
    }

    private fun ratio(done: Int, total: Int): Double = if (total <= 0) 1.0 else (done.toDouble() / total).coerceIn(0.0, 1.0)
}
