package ai.mayra.app.background

import ai.mayra.app.MainActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

enum class BriefingKind { MORNING, EVENING }

object MayraBriefingScheduler {
    private const val MORNING_WORK = "mayra-morning-briefing"
    private const val EVENING_WORK = "mayra-evening-briefing"

    fun sync(context: Context) {
        val appContext = context.applicationContext
        val preferences = AmbientPreferenceStore(appContext).read()
        val workManager = WorkManager.getInstance(appContext)

        if (preferences.morningBriefingEnabled) {
            enqueue(workManager, BriefingKind.MORNING, hour = 8, uniqueName = MORNING_WORK)
        } else {
            workManager.cancelUniqueWork(MORNING_WORK)
        }

        if (preferences.eveningBriefingEnabled) {
            enqueue(workManager, BriefingKind.EVENING, hour = 19, uniqueName = EVENING_WORK)
        } else {
            workManager.cancelUniqueWork(EVENING_WORK)
        }
    }

    private fun enqueue(workManager: WorkManager, kind: BriefingKind, hour: Int, uniqueName: String) {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes().coerceAtLeast(1)

        val request = PeriodicWorkRequestBuilder<MayraBriefingWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(androidx.work.workDataOf(KEY_KIND to kind.name))
            .build()

        workManager.enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    internal const val KEY_KIND = "briefing_kind"
}

class MayraBriefingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val kind = runCatching {
            BriefingKind.valueOf(inputData.getString(MayraBriefingScheduler.KEY_KIND).orEmpty())
        }.getOrDefault(BriefingKind.MORNING)
        val preferences = AmbientPreferenceStore(applicationContext).read()
        val enabled = when (kind) {
            BriefingKind.MORNING -> preferences.morningBriefingEnabled
            BriefingKind.EVENING -> preferences.eveningBriefingEnabled
        }
        if (!enabled) return Result.success()

        val now = System.currentTimeMillis()
        val since = now - 12L * 60L * 60L * 1000L
        val events = AmbientEventStore(applicationContext).snapshot()
        val briefing = DailyBriefingEngine().build(events, since = since, maxItems = 6)
        BriefingCache(applicationContext).save(kind, briefing, now)
        MayraBriefingNotifier.show(applicationContext, kind, briefing)
        TrustAuditStore(applicationContext).append(
            AuditEntry(
                actionId = null,
                actionType = "BRIEFING_${kind.name}",
                outcome = AuditOutcome.EXECUTED,
                summary = "${kind.name.lowercase().replaceFirstChar(Char::uppercase)} briefing prepared",
                timestamp = now
            )
        )
        Result.success()
    }.getOrElse { Result.retry() }
}

data class CachedBriefing(val kind: BriefingKind, val text: String, val generatedAt: Long)

class BriefingCache(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun save(kind: BriefingKind, text: String, generatedAt: Long) {
        preferences.edit()
            .putString("${kind.name}_text", text.take(2000))
            .putLong("${kind.name}_time", generatedAt)
            .apply()
    }

    fun read(kind: BriefingKind): CachedBriefing? {
        val text = preferences.getString("${kind.name}_text", null) ?: return null
        val time = preferences.getLong("${kind.name}_time", 0L)
        return CachedBriefing(kind, text, time)
    }

    private companion object { const val FILE_NAME = "mayra_briefing_cache" }
}

object MayraBriefingNotifier {
    private const val CHANNEL_ID = "mayra_briefings"

    @SuppressLint("MissingPermission")
    fun show(context: Context, kind: BriefingKind, text: String) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        createChannel(appContext)
        val intent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            kind.ordinal + 200,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = when (kind) {
            BriefingKind.MORNING -> "Good morning — Mayra briefing"
            BriefingKind.EVENING -> "Evening summary from Mayra"
        }
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text.take(120))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(appContext).notify(300 + kind.ordinal, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Mayra briefings",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morning and evening summaries prepared by Mayra"
            }
        )
    }
}
