package ai.mayra.app.background

import android.content.Context
import java.util.Locale
import java.util.UUID

enum class AmbientCategory { MESSAGE, OTP, BANKING, DELIVERY, CALENDAR, SYSTEM, PROMOTION, OTHER }
enum class AmbientPriority { LOW, NORMAL, HIGH, CRITICAL }
enum class ScreenDecision { BACKGROUND_ONLY, NOTIFICATION_ONLY, REQUIRE_CONFIRMATION, OPEN_APP }
enum class BackgroundTaskState { PENDING, RUNNING, COMPLETED, FAILED, CANCELLED }

data class AmbientInsight(
    val category: AmbientCategory,
    val priority: AmbientPriority,
    val summary: String,
    val screenDecision: ScreenDecision,
    val suggestedAction: String? = null
)

class NotificationIntelligenceEngine {
    fun analyze(event: AmbientEvent): AmbientInsight {
        val source = event.sourcePackage.lowercase(Locale.ROOT)
        val text = "${event.title} ${event.text}".lowercase(Locale.ROOT)
        val category = when {
            OTP_WORDS.any(text::contains) -> AmbientCategory.OTP
            BANK_WORDS.any(text::contains) || source.contains("bank") -> AmbientCategory.BANKING
            DELIVERY_WORDS.any(text::contains) -> AmbientCategory.DELIVERY
            CALENDAR_WORDS.any(text::contains) -> AmbientCategory.CALENDAR
            MESSAGE_PACKAGES.any(source::contains) -> AmbientCategory.MESSAGE
            PROMOTION_WORDS.any(text::contains) -> AmbientCategory.PROMOTION
            source.startsWith("android") || source.contains("systemui") -> AmbientCategory.SYSTEM
            else -> AmbientCategory.OTHER
        }
        val priority = when {
            SECURITY_WORDS.any(text::contains) -> AmbientPriority.CRITICAL
            category == AmbientCategory.OTP -> AmbientPriority.HIGH
            category == AmbientCategory.BANKING && MONEY_WORDS.any(text::contains) -> AmbientPriority.HIGH
            category == AmbientCategory.CALENDAR && URGENT_WORDS.any(text::contains) -> AmbientPriority.HIGH
            category == AmbientCategory.PROMOTION -> AmbientPriority.LOW
            else -> AmbientPriority.NORMAL
        }
        val decision = when (priority) {
            AmbientPriority.CRITICAL -> ScreenDecision.REQUIRE_CONFIRMATION
            AmbientPriority.HIGH -> ScreenDecision.NOTIFICATION_ONLY
            AmbientPriority.NORMAL -> ScreenDecision.BACKGROUND_ONLY
            AmbientPriority.LOW -> ScreenDecision.BACKGROUND_ONLY
        }
        return AmbientInsight(
            category = category,
            priority = priority,
            summary = summarize(event, category),
            screenDecision = decision,
            suggestedAction = suggestedAction(category, text)
        )
    }

    private fun summarize(event: AmbientEvent, category: AmbientCategory): String {
        val body = event.text.ifBlank { event.title }.trim().take(180)
        return when (category) {
            AmbientCategory.OTP -> "OTP received: $body"
            AmbientCategory.BANKING -> "Banking update: $body"
            AmbientCategory.DELIVERY -> "Delivery update: $body"
            AmbientCategory.CALENDAR -> "Schedule update: $body"
            AmbientCategory.MESSAGE -> "New message: $body"
            AmbientCategory.PROMOTION -> "Promotional notification"
            AmbientCategory.SYSTEM -> "Device update: $body"
            AmbientCategory.OTHER -> body
        }
    }

    private fun suggestedAction(category: AmbientCategory, text: String): String? = when {
        category == AmbientCategory.MESSAGE -> "reply"
        category == AmbientCategory.DELIVERY -> "track_delivery"
        category == AmbientCategory.CALENDAR -> "review_schedule"
        category == AmbientCategory.BANKING && SECURITY_WORDS.any(text::contains) -> "review_security"
        else -> null
    }

    private companion object {
        val MESSAGE_PACKAGES = listOf("whatsapp", "telegram", "messaging", "sms", "signal")
        val OTP_WORDS = listOf("otp", "one time password", "verification code", "security code")
        val BANK_WORDS = listOf("bank", "upi", "account", "card", "debit", "credit")
        val MONEY_WORDS = listOf("debited", "credited", "transaction", "payment", "withdrawn", "spent")
        val DELIVERY_WORDS = listOf("delivered", "out for delivery", "shipped", "order", "courier")
        val CALENDAR_WORDS = listOf("meeting", "appointment", "calendar", "event", "reminder")
        val PROMOTION_WORDS = listOf("sale", "offer", "discount", "coupon", "cashback", "deal")
        val URGENT_WORDS = listOf("now", "urgent", "starting", "missed", "overdue")
        val SECURITY_WORDS = listOf("fraud", "suspicious", "unauthorized", "blocked", "security alert")
    }
}

data class BackgroundTask(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val payload: String,
    val createdAt: Long,
    val runAfter: Long = createdAt,
    val attempt: Int = 0,
    val maxAttempts: Int = 3,
    val state: BackgroundTaskState = BackgroundTaskState.PENDING,
    val lastError: String? = null
) {
    val canRun: Boolean get() = state == BackgroundTaskState.PENDING && attempt < maxAttempts
}

class BackgroundTaskQueue(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("mayra_background_tasks", Context.MODE_PRIVATE)

    @Synchronized
    fun enqueue(task: BackgroundTask) {
        val tasks = snapshot().filterNot { it.id == task.id } + task
        save(tasks)
    }

    @Synchronized
    fun due(now: Long, limit: Int = 20): List<BackgroundTask> = snapshot()
        .filter { it.canRun && it.runAfter <= now }
        .sortedBy(BackgroundTask::createdAt)
        .take(limit)

    @Synchronized
    fun markRunning(id: String) = update(id) { it.copy(state = BackgroundTaskState.RUNNING) }

    @Synchronized
    fun markCompleted(id: String) = update(id) { it.copy(state = BackgroundTaskState.COMPLETED, lastError = null) }

    @Synchronized
    fun markFailed(id: String, error: String, retryAt: Long) = update(id) {
        val nextAttempt = it.attempt + 1
        it.copy(
            state = if (nextAttempt >= it.maxAttempts) BackgroundTaskState.FAILED else BackgroundTaskState.PENDING,
            attempt = nextAttempt,
            runAfter = retryAt,
            lastError = error.take(200)
        )
    }

    @Synchronized
    fun prune(maxEntries: Int = 200) {
        val retained = snapshot().sortedByDescending(BackgroundTask::createdAt).take(maxEntries)
        save(retained)
    }

    fun snapshot(): List<BackgroundTask> = preferences.getStringSet(KEY_TASKS, emptySet()).orEmpty()
        .mapNotNull(::decode)

    private fun update(id: String, transform: (BackgroundTask) -> BackgroundTask) {
        save(snapshot().map { if (it.id == id) transform(it) else it })
    }

    private fun save(tasks: List<BackgroundTask>) {
        preferences.edit().putStringSet(KEY_TASKS, tasks.map(::encode).toSet()).apply()
    }

    private fun encode(task: BackgroundTask): String = listOf(
        task.id, task.type, task.payload, task.createdAt, task.runAfter, task.attempt,
        task.maxAttempts, task.state.name, task.lastError.orEmpty()
    ).joinToString(SEPARATOR) { it.toString().replace(SEPARATOR, " ") }

    private fun decode(value: String): BackgroundTask? {
        val parts = value.split(SEPARATOR)
        if (parts.size != 9) return null
        return BackgroundTask(
            id = parts[0], type = parts[1], payload = parts[2],
            createdAt = parts[3].toLongOrNull() ?: return null,
            runAfter = parts[4].toLongOrNull() ?: return null,
            attempt = parts[5].toIntOrNull() ?: return null,
            maxAttempts = parts[6].toIntOrNull() ?: return null,
            state = runCatching { BackgroundTaskState.valueOf(parts[7]) }.getOrNull() ?: return null,
            lastError = parts[8].ifBlank { null }
        )
    }

    private companion object {
        const val KEY_TASKS = "tasks"
        const val SEPARATOR = "\u001E"
    }
}

class DailyBriefingEngine(private val intelligence: NotificationIntelligenceEngine = NotificationIntelligenceEngine()) {
    fun build(events: List<AmbientEvent>, since: Long, maxItems: Int = 8): String {
        val insights = events.filter { it.timestamp >= since }.map(intelligence::analyze)
        if (insights.isEmpty()) return "No important updates since your last briefing."
        val selected = insights.sortedWith(compareByDescending<AmbientInsight> { it.priority.ordinal }.thenBy { it.category.name })
            .take(maxItems)
        return buildString {
            append("Mayra briefing: ")
            append(selected.joinToString(" | ") { it.summary })
            val hidden = insights.size - selected.size
            if (hidden > 0) append(" | $hidden more updates")
        }
    }
}
