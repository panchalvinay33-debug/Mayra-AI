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

/**
 * Schedules Mayra's durable background maintenance.
 *
 * This is intentionally event-driven rather than a permanently spinning process. Android may kill
 * ordinary background processes, while WorkManager restores persistent work when constraints allow.
 */
object MayraBackgroundRuntime {
    private const val PERIODIC_WORK_NAME = "mayra-ambient-maintenance"

    fun initialize(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<MayraMaintenanceWorker>(
            15,
            TimeUnit.MINUTES
        ).setConstraints(constraints).build()

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
        val store = AmbientEventStore(applicationContext)
        store.recordSystemHeartbeat(System.currentTimeMillis())
        store.prune(maxEntries = 200)
        Result.success()
    }.getOrElse { Result.retry() }
}

/** Small durable event inbox used by notification, boot and scheduled background components. */
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
