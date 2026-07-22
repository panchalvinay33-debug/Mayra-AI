package ai.mayra.app.core.reminder

import ai.mayra.app.data.local.ReminderEntity
import ai.mayra.app.data.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow

class ReminderManager(
    private val repository: ReminderRepository,
    private val clock: () -> Long = System::currentTimeMillis
) {
    fun observePending(): Flow<List<ReminderEntity>> = repository.observePending()

    suspend fun create(
        title: String,
        triggerAt: Long,
        description: String? = null,
        repeatRule: String? = null
    ): Long {
        val normalizedTitle = title.trim()
        require(normalizedTitle.isNotEmpty()) { "Reminder title cannot be blank." }
        require(triggerAt >= clock()) { "Reminder time cannot be in the past." }

        return repository.schedule(
            ReminderEntity(
                title = normalizedTitle,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                triggerAt = triggerAt,
                repeatRule = normalizeRepeatRule(repeatRule),
                createdAt = clock()
            )
        )
    }

    suspend fun get(id: Long): ReminderEntity? {
        require(id > 0) { "Reminder id must be positive." }
        return repository.get(id)
    }

    suspend fun due(now: Long = clock()): List<ReminderEntity> = repository.due(now)

    suspend fun complete(id: Long): Boolean {
        require(id > 0) { "Reminder id must be positive." }
        return repository.complete(id)
    }

    suspend fun snooze(id: Long, durationMillis: Long): Boolean {
        require(id > 0) { "Reminder id must be positive." }
        require(durationMillis in MIN_SNOOZE_MILLIS..MAX_SNOOZE_MILLIS) {
            "Snooze duration must be between 1 minute and 7 days."
        }
        val reminder = repository.get(id) ?: return false
        val base = maxOf(clock(), reminder.triggerAt)
        return repository.reschedule(id, base + durationMillis)
    }

    suspend fun cancel(id: Long): Boolean {
        require(id > 0) { "Reminder id must be positive." }
        val reminder = repository.get(id) ?: return false
        repository.cancel(reminder)
        return true
    }

    private fun normalizeRepeatRule(rule: String?): String? {
        val normalized = rule?.trim()?.lowercase()?.takeIf { it.isNotEmpty() } ?: return null
        require(normalized in SUPPORTED_REPEAT_RULES) {
            "Unsupported repeat rule: $normalized"
        }
        return normalized
    }

    companion object {
        const val REPEAT_DAILY = "daily"
        const val REPEAT_WEEKLY = "weekly"
        const val REPEAT_MONTHLY = "monthly"
        const val MIN_SNOOZE_MILLIS = 60_000L
        const val MAX_SNOOZE_MILLIS = 7L * 24 * 60 * 60 * 1000

        val SUPPORTED_REPEAT_RULES = setOf(
            REPEAT_DAILY,
            REPEAT_WEEKLY,
            REPEAT_MONTHLY
        )
    }
}
