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
import androidx.work.workDataOf
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

internal enum class RuntimeAttentionImmediatePhase {
    IDLE,
    QUEUED,
    RUNNING,
    COMPLETED,
    RETRYING
}

internal data class RuntimeAttentionImmediateState(
    val phase: RuntimeAttentionImmediatePhase,
    val updatedAt: Long
) {
    fun status(now: Long): String {
        if (phase == RuntimeAttentionImmediatePhase.IDLE || updatedAt <= 0L) {
            return "No manual background scan queued"
        }
        val ageMinutes = ((now - updatedAt).coerceAtLeast(0L) / 60_000L)
        val age = when {
            ageMinutes <= 0L -> "just now"
            ageMinutes == 1L -> "1 min ago"
            else -> "$ageMinutes min ago"
        }
        val label = when (phase) {
            RuntimeAttentionImmediatePhase.IDLE -> "idle"
            RuntimeAttentionImmediatePhase.QUEUED -> "queued"
            RuntimeAttentionImmediatePhase.RUNNING -> "running"
            RuntimeAttentionImmediatePhase.COMPLETED -> "completed"
            RuntimeAttentionImmediatePhase.RETRYING -> "retry scheduled"
        }
        return "Manual background scan: $label · $age"
    }
}

internal fun nextBackgroundScanEstimate(
    schedule: RuntimeAttentionScheduleState,
    lastCompletedAt: Long?,
    now: Long
): String {
    if (!schedule.enabled) return "Next background scan: off"
    val interval = runtimeAttentionIntervalMinutes(schedule.intervalMinutes)
    if (lastCompletedAt == null || lastCompletedAt <= 0L) {
        return "First background scan expected within about $interval min"
    }
    val intervalMillis = interval * 60_000L
    val dueAt = lastCompletedAt + intervalMillis
    val remainingMillis = dueAt - now
    if (remainingMillis <= 0L) {
        return "Background scan is due; Android may delay it"
    }
    val remainingMinutes = ((remainingMillis + 59_999L) / 60_000L).coerceAtLeast(1L)
    return "Next background scan in about $remainingMinutes min"
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

internal class RuntimeAttentionImmediatePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): RuntimeAttentionImmediateState {
        val phase = preferences.getString(KEY_PHASE, null)
            ?.let { runCatching { RuntimeAttentionImmediatePhase.valueOf(it) }.getOrNull() }
            ?: RuntimeAttentionImmediatePhase.IDLE
        return RuntimeAttentionImmediateState(
            phase = phase,
            updatedAt = preferences.getLong(KEY_UPDATED_AT, 0L)
        )
    }

    fun record(phase: RuntimeAttentionImmediatePhase, updatedAt: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putString(KEY_PHASE, phase.name)
            .putLong(KEY_UPDATED_AT, updatedAt)
            .apply()
    }

    private companion object {
        const val PREFS = "runtime_attention_immediate"
        const val KEY_PHASE = "phase"
        const val KEY_UPDATED_AT = "updated_at"
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
        ).setInputData(workDataOf(KEY_TRIGGER to TRIGGER_PERIODIC)).build()
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
        val appContext = context.applicationContext
        RuntimeAttentionImmediatePreferences(appContext).record(RuntimeAttentionImmediatePhase.QUEUED)
        val request = OneTimeWorkRequestBuilder<RuntimeAttentionWorker>()
            .setInputData(workDataOf(KEY_TRIGGER to TRIGGER_IMMEDIATE))
            .build()
        WorkManager.getInstance(appContext).enqueueUniqueWork(
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
        val immediate = inputData.getString(KEY_TRIGGER) == TRIGGER_IMMEDIATE
        val immediatePreferences = RuntimeAttentionImmediatePreferences(applicationContext)
        if (immediate) immediatePreferences.record(RuntimeAttentionImmediatePhase.RUNNING)

        if (!MayraRuntime.installed) {
            diagnostics.record(RuntimeAttentionScanOutcome.RUNTIME_UNAVAILABLE)
            if (immediate) immediatePreferences.record(RuntimeAttentionImmediatePhase.RETRYING)
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
            if (immediate) immediatePreferences.record(RuntimeAttentionImmediatePhase.COMPLETED)
            Result.success()
        }.getOrElse {
            diagnostics.record(RuntimeAttentionScanOutcome.SNAPSHOT_FAILED)
            if (immediate) immediatePreferences.record(RuntimeAttentionImmediatePhase.RETRYING)
            Result.retry()
        }
    }
}

private const val KEY_TRIGGER = "runtime_attention_trigger"
private const val TRIGGER_PERIODIC = "periodic"
private const val TRIGGER_IMMEDIATE = "immediate"
private const val MIN_PERIODIC_INTERVAL_MINUTES = 15L
