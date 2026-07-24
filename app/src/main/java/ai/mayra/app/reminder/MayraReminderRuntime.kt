package ai.mayra.app.reminder

import ai.mayra.app.presence.MayraPresenceActivity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.util.concurrent.TimeUnit

object MayraReminderRuntime {
    private const val WORK_PREFIX = "mayra-reminder-"

    fun create(context: Context, parsed: ReminderParseResult.Parsed, now: Long = System.currentTimeMillis()): MayraReminder {
        val reminder = MayraReminder(
            title = parsed.title,
            detail = parsed.detail,
            dueAt = parsed.dueAt,
            createdAt = now
        )
        MayraReminderStore(context).upsert(reminder)
        schedule(context, reminder, now)
        return reminder
    }

    fun schedule(context: Context, reminder: MayraReminder, now: Long = System.currentTimeMillis()) {
        if (reminder.state !in setOf(ReminderState.SCHEDULED, ReminderState.SNOOZED, ReminderState.MISSED)) return
        val delay = (reminder.dueAt - now).coerceAtLeast(0L)
        val request = OneTimeWorkRequestBuilder<MayraReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putString(MayraReminderWorker.KEY_ID, reminder.id).build())
            .addTag(WORK_PREFIX + reminder.id)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_PREFIX + reminder.id,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, id: String, now: Long = System.currentTimeMillis()): MayraReminder? {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_PREFIX + id)
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        return MayraReminderStore(context).cancel(id, now)
    }

    fun complete(context: Context, id: String, now: Long = System.currentTimeMillis()): MayraReminder? {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_PREFIX + id)
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        return MayraReminderStore(context).complete(id, now)
    }

    fun snooze(context: Context, id: String, duration: Duration, now: Long = System.currentTimeMillis()): MayraReminder? {
        val updated = MayraReminderStore(context).snooze(id, duration, now) ?: return null
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        schedule(context, updated, now)
        return updated
    }

    fun rescheduleAll(context: Context, now: Long = System.currentTimeMillis()) {
        val store = MayraReminderStore(context)
        store.active(now).forEach { reminder ->
            val normalized = if (reminder.dueAt < now && reminder.state != ReminderState.DUE) {
                store.markMissed(reminder.id, now) ?: reminder
            } else reminder
            schedule(context, normalized, now)
        }
    }
}

class MayraReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {
    override fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val store = MayraReminderStore(applicationContext)
        val reminder = store.find(id) ?: return Result.success()
        if (reminder.state in setOf(ReminderState.COMPLETED, ReminderState.CANCELLED)) return Result.success()
        val now = System.currentTimeMillis()
        if (reminder.dueAt > now + 2_000L) {
            MayraReminderRuntime.schedule(applicationContext, reminder, now)
            return Result.success()
        }
        val updated = store.markNotified(id, now) ?: return Result.success()
        return runCatching {
            MayraReminderNotifier.show(applicationContext, updated)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object { const val KEY_ID = "reminder_id" }
}

object MayraReminderNotifier {
    private const val CHANNEL_ID = "mayra_reminders"

    fun show(context: Context, reminder: MayraReminder) {
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MayraReminderActivity::class.java).putExtra(MayraReminderActivity.EXTRA_ID, reminder.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val completeIntent = actionIntent(context, reminder.id, MayraReminderActionReceiver.ACTION_COMPLETE, 1)
        val snoozeIntent = actionIntent(context, reminder.id, MayraReminderActionReceiver.ACTION_SNOOZE, 2)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Mayra reminder")
            .setContentText(reminder.title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.detail ?: reminder.title))
            .setContentIntent(openIntent)
            .setAutoCancel(false)
            .setOnlyAlertOnce(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Complete", completeIntent)
            .addAction(0, "Snooze 10 min", snoozeIntent)
            .build()
        NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
    }

    private fun actionIntent(context: Context, id: String, action: String, salt: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.hashCode() * 31 + salt,
            Intent(context, MayraReminderActionReceiver::class.java).setAction(action).putExtra(MayraReminderActionReceiver.EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mayra reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders created and followed up by Mayra"
            }
        )
    }
}

class MayraReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_ID) ?: return
        when (intent.action) {
            ACTION_COMPLETE -> MayraReminderRuntime.complete(context, id)
            ACTION_SNOOZE -> MayraReminderRuntime.snooze(context, id, Duration.ofMinutes(10))
            ACTION_CANCEL -> MayraReminderRuntime.cancel(context, id)
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val ACTION_COMPLETE = "ai.mayra.app.reminder.COMPLETE"
        const val ACTION_SNOOZE = "ai.mayra.app.reminder.SNOOZE"
        const val ACTION_CANCEL = "ai.mayra.app.reminder.CANCEL"
    }
}
