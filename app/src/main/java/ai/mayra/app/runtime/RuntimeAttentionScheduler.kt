package ai.mayra.app.runtime

import ai.mayra.app.MayraRuntime
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

internal fun runtimeAttentionIntervalMinutes(requestedMinutes: Long): Long =
    requestedMinutes.coerceAtLeast(MIN_PERIODIC_INTERVAL_MINUTES)

object RuntimeAttentionScheduler {
    private const val UNIQUE_WORK = "mayra-runtime-attention"

    fun sync(context: Context, intervalMinutes: Long = DEFAULT_INTERVAL_MINUTES) {
        val appContext = context.applicationContext
        val request = PeriodicWorkRequestBuilder<RuntimeAttentionWorker>(
            runtimeAttentionIntervalMinutes(intervalMinutes),
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    const val DEFAULT_INTERVAL_MINUTES = 15L
}

class RuntimeAttentionWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        if (!MayraRuntime.installed) return Result.retry()
        RuntimeAttentionNotifier.scanAndNotify(
            applicationContext,
            MayraRuntime.controlCenter.snapshot()
        )
        Result.success()
    }.getOrElse { Result.retry() }
}

private const val MIN_PERIODIC_INTERVAL_MINUTES = 15L
