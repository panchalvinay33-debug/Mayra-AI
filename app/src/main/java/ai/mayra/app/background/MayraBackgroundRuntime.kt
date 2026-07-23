package ai.mayra.app.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object MayraBackgroundRuntime {
    private const val PERIODIC_WORK_NAME = "mayra-ambient-maintenance"

    fun initialize(context: Context) {
        val request = PeriodicWorkRequestBuilder<MayraMaintenanceWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

class MayraMaintenanceWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val now = System.currentTimeMillis()
        val eventStore = AmbientEventStore(applicationContext)
        val taskQueue = BackgroundTaskQueue(applicationContext)
        val processor = AmbientTaskProcessor()

        eventStore.recordSystemHeartbeat(now)
        taskQueue.due(now).forEach { task ->
            taskQueue.markRunning(task.id)
            when (val outcome = processor.process(task)) {
                TaskOutcome.Completed -> taskQueue.markCompleted(task.id)
                is TaskOutcome.Retry -> taskQueue.markFailed(
                    id = task.id,
                    error = outcome.reason,
                    retryAt = now + retryDelayMillis(task.attempt)
                )
            }
        }

        val briefing = DailyBriefingEngine().build(
            events = eventStore.snapshot(),
            since = now - TimeUnit.HOURS.toMillis(24)
        )
        AmbientBriefingStore(applicationContext).save(briefing, now)
        eventStore.prune(maxEntries = 300)
        taskQueue.prune(maxEntries = 250)
        Result.success()
    }.getOrElse { Result.retry() }

    private fun retryDelayMillis(attempt: Int): Long {
        val minutes = when (attempt) {
            0 -> 5L
            1 -> 15L
            else -> 60L
        }
        return TimeUnit.MINUTES.toMillis(minutes)
    }
}

sealed interface TaskOutcome {
    data object Completed : TaskOutcome
    data class Retry(val reason: String) : TaskOutcome
}

/**
 * Background processor intentionally handles only work that does not need a visible activity.
 * UI-bound and sensitive actions remain queued for a user-visible confirmation flow.
 */
class AmbientTaskProcessor {
    fun process(task: BackgroundTask): TaskOutcome = when (task.type) {
        "track_delivery", "review_schedule" -> TaskOutcome.Completed
        "reply", "review_security" -> TaskOutcome.Retry("User confirmation is required.")
        else -> TaskOutcome.Retry("No background handler is registered for ${task.type}.")
    }
}

class AmbientBriefingStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mayra_ambient_briefing",
        Context.MODE_PRIVATE
    )

    fun save(text: String, generatedAt: Long) {
        preferences.edit()
            .putString(KEY_TEXT, text)
            .putLong(KEY_GENERATED_AT, generatedAt)
            .apply()
    }

    fun latest(): AmbientBriefing = AmbientBriefing(
        text = preferences.getString(KEY_TEXT, null),
        generatedAt = preferences.getLong(KEY_GENERATED_AT, 0L)
    )

    private companion object {
        const val KEY_TEXT = "text"
        const val KEY_GENERATED_AT = "generated_at"
    }
}

data class AmbientBriefing(val text: String?, val generatedAt: Long)

class AmbientEventStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mayra_ambient_events",
        Context.MODE_PRIVATE
    )

    @Synchronized
    fun append(event: AmbientEvent) {
        val current = preferences.getStringSet(KEY_EVENTS, emptySet()).orEmpty().toMutableSet()
        current += event.encode()
        preferences.edit().putStringSet(KEY_EVENTS, current).apply()
    }

    fun recordSystemHeartbeat(timestamp: Long) {
        preferences.edit().putLong(KEY_LAST_HEARTBEAT, timestamp).apply()
    }

    fun lastHeartbeat(): Long = preferences.getLong(KEY_LAST_HEARTBEAT, 0L)

    @Synchronized
    fun snapshot(): List<AmbientEvent> = preferences
        .getStringSet(KEY_EVENTS, emptySet())
        .orEmpty()
        .mapNotNull(AmbientEvent::decode)
        .sortedByDescending(AmbientEvent::timestamp)

    @Synchronized
    fun prune(maxEntries: Int) {
        require(maxEntries > 0)
        val retained = snapshot().take(maxEntries).map(AmbientEvent::encode).toSet()
        preferences.edit().putStringSet(KEY_EVENTS, retained).apply()
    }

    private companion object {
        const val KEY_EVENTS = "events"
        const val KEY_LAST_HEARTBEAT = "last_heartbeat"
    }
}

data class AmbientEvent(
    val sourcePackage: String,
    val title: String,
    val text: String,
    val timestamp: Long
) {
    fun encode(): String = listOf(sourcePackage, title, text, timestamp.toString())
        .joinToString(SEPARATOR) { it.replace(SEPARATOR, " ") }

    companion object {
        private const val SEPARATOR = "\u001F"

        fun decode(value: String): AmbientEvent? {
            val parts = value.split(SEPARATOR)
            if (parts.size != 4) return null
            return AmbientEvent(
                sourcePackage = parts[0],
                title = parts[1],
                text = parts[2],
                timestamp = parts[3].toLongOrNull() ?: return null
            )
        }
    }
}
