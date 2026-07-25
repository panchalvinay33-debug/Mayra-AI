package ai.mayra.app.file

import ai.mayra.app.safety.MayraGlobalStopStore
import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MayraFileInventoryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (MayraGlobalStopStore(applicationContext).isStopped()) {
            return@withContext Result.success(outputData(STOPPED_KEY to true))
        }
        runCatching {
            val store = MayraEncryptedFileIndexStore(applicationContext)
            val registry = MayraFileGrantRegistry(applicationContext)
            val scanner = MayraFileInventoryScanner(applicationContext)
            val mediaFiles = scanner.scanMediaStore()
            val grants = registry.list()
            val treeFiles = grants.flatMap { grant ->
                if (registry.hasPersistedReadAccess(grant.treeUri)) {
                    scanner.scanTree(Uri.parse(grant.treeUri))
                } else emptyList()
            }
            val now = System.currentTimeMillis()
            val refreshedGrants = grants.map { grant ->
                grant.copy(lastScanAt = now, enabled = registry.hasPersistedReadAccess(grant.treeUri))
            }
            val snapshot = store.merge(mediaFiles + treeFiles, refreshedGrants)
            Result.success(outputData(
                FILE_COUNT_KEY to snapshot.files.size,
                GRANT_COUNT_KEY to snapshot.grants.count { it.enabled },
                GENERATION_KEY to snapshot.generation
            ))
        }.getOrElse { error ->
            Result.failure(outputData(ERROR_KEY to error.javaClass.simpleName.take(80)))
        }
    }

    private fun outputData(vararg values: Pair<String, Any>): androidx.work.Data =
        androidx.work.Data.Builder().apply {
            values.forEach { (key, value) ->
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is String -> putString(key, value)
                }
            }
        }.build()

    companion object {
        const val UNIQUE_WORK = "mayra_file_inventory"
        const val FILE_COUNT_KEY = "file_count"
        const val GRANT_COUNT_KEY = "grant_count"
        const val GENERATION_KEY = "generation"
        const val STOPPED_KEY = "global_stop"
        const val ERROR_KEY = "error"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<MayraFileInventoryWorker>()
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE_WORK, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}
