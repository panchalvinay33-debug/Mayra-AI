package ai.mayra.app.runtime

import ai.mayra.app.MayraRuntime
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

internal data class RuntimeAttentionScheduleState(
    val enabled: Boolean,
    val intervalMinutes: Long
) {
    fun status(): String = if (enabled) {
        "Background scans every ${runtimeAttentionIntervalMinutes(intervalMinutes)} min"
    } else {
        "Background scans are off"
    }
}

internal fun runtimeAttentionIntervalMinutes(requestedMinutes: Long): Long =
    requestedMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES)

internal class RuntimeAttentionSchedulePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): RuntimeAttentionScheduleState = RuntimeAttentionScheduleState(
        enabled = preferences.getBoolean(KEY_ENABLED, true),
        intervalMinutes = runtimeAttentionIntervalMinutes(
            preferences.getLong(KEY_INTERVAL_MINUTES, RuntimeAttentionScheduler.DEFAULT_INTERVAL_MINUTES)
        )
    )

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setIntervalMinutes(intervalMinutes: Long) {
        preferences.edit()
            .putLong(KEY_INTERVAL_MINUTES, runtimeAttentionIntervalMinutes(intervalMinutes))
            .apply()
    }

    private companion object {
        const val PREFS = "runtime_attention_schedule"
        const val KEY_ENABLED = "enabled"
        const val KEY_INTERVAL_MINUTES = "interval_minutes"
    }
}

object RuntimeAttentionScheduler {
    private const val UNIQUE_PERIODIC_WORK = "mayra-runtime-attention"
    private const val UNIQUE_IMMEDIATE_WORK = "mayra-runtime-attention-now"

    fun sync(context: Context, intervalMinutes: Long? = null) {
        val appContext = context.applicationContext
        val preferences = RuntimeAttentionSchedulePreferences(appContext)
        intervalMinutes?.let(preferences::setIntervalMinutes)
        val state = preferences.read()
        val workManager = WorkManager.getInstance(appContext)

        if (!state.enabled) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
            return
        }

        val request = PeriodicWorkRequestBuilder<RuntimeAttentionWorker>(
            state.intervalMinutes,
            TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        RuntimeAttentionSchedulePreferences(context).setEnabled(enabled)
        sync(context)
    }

    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<RuntimeAttentionWorker>().build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    const val DEFAULT_INTERVAL_MINUTES = 15L
}

class RuntimeAttentionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val diagnostics = RuntimeAttentionDiagnostics(applicationContext)
        if (!MayraRuntime.installed) {
            diagnostics.record(RuntimeAttentionScanOutcome.RUNTIME_UNAVAILABLE)
            return Result.retry()
        }
        return runCatching {
            val posted = RuntimeAttentionNotifier.scanAndNotify(
                applicationContext,
                MayraRuntime.controlCenter.snapshot()
            )
            diagnostics.record(
                if (posted) RuntimeAttentionScanOutcome.ALERT_POSTED
                else RuntimeAttentionScanOutcome.NO_NEW_ALERT
            )
            Result.success()
        }.getOrElse {
            diagnostics.record(RuntimeAttentionScanOutcome.SNAPSHOT_FAILED)
            Result.retry()
        }
    }
}

private const val MIN_PERIODIC_INTERVAL_MINUTES = 15L
