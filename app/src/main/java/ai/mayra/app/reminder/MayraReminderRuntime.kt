package ai.mayra.app.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ai.mayra.app.MainActivity
import java.time.Duration
import java.util.concurrent.TimeUnit

object MayraReminderRuntime {
    private const val WORK_PREFIX = "mayra-reminder-"
    private const val FOLLOW_UP_PREFIX = "mayra-reminder-follow-up-"

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
            .setInputData(
                Data.Builder()
                    .putString(MayraReminderWorker.KEY_ID, reminder.id)
                    .putLong(MayraReminderWorker.KEY_REVISION, reminder.revision)
                    .putLong(MayraReminderWorker.KEY_DUE_AT, reminder.dueAt)
                    .build()
            )
            .addTag(WORK_PREFIX + reminder.id)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_PREFIX + reminder.id,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleFollowUp(context: Context, reminder: MayraReminder) {
        if (!reminder.followUpEnabled || reminder.state != ReminderState.DUE) return
        val request = OneTimeWorkRequestBuilder<MayraReminderFollowUpWorker>()
            .setInitialDelay(30, TimeUnit.MINUTES)
            .setInputData(
                Data.Builder()
                    .putString(MayraReminderWorker.KEY_ID, reminder.id)
                    .putLong(MayraReminderWorker.KEY_REVISION, reminder.revision)
                    .build()
            )
            .addTag(FOLLOW_UP_PREFIX + reminder.id)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            FOLLOW_UP_PREFIX + reminder.id,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, id: String, now: Long = System.currentTimeMillis()): MayraReminder? {
        val updated = MayraReminderStore(context).cancel(id, now) ?: return null
        cancelWork(context, id)
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        return updated
    }

    fun complete(context: Context, id: String, now: Long = System.currentTimeMillis()): MayraReminder? {
        val updated = MayraReminderStore(context).complete(id, now) ?: return null
        cancelWork(context, id)
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        return updated
    }

    fun snooze(context: Context, id: String, duration: Duration, now: Long = System.currentTimeMillis()): MayraReminder? {
        val updated = MayraReminderStore(context).snooze(id, duration, now) ?: return null
        val manager = WorkManager.getInstance(context.applicationContext)
        manager.cancelUniqueWork(WORK_PREFIX + id)
        manager.cancelUniqueWork(FOLLOW_UP_PREFIX + id)
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        schedule(context, updated, now)
        return updated
    }

    fun rescheduleAll(context: Context, now: Long = System.currentTimeMillis()) {
        val store = MayraReminderStore(context)
        store.active().forEach { reminder ->
            when (ReminderRecoveryPolicy.decide(reminder, now)) {
                ReminderRecoveryAction.SCHEDULE -> schedule(context, reminder, now)
                ReminderRecoveryAction.SCHEDULE_FOLLOW_UP -> scheduleFollowUp(context, reminder)
                ReminderRecoveryAction.MARK_MISSED_AND_NOTIFY -> {
                    val missed = store.markMissed(reminder.id, now) ?: return@forEach
                    if (MayraReminderNotifier.canNotify(context)) {
                        MayraReminderNotifier.show(context, missed, followUp = true)
                    }
                }
                ReminderRecoveryAction.LEAVE_MISSED,
                ReminderRecoveryAction.IGNORE -> Unit
            }
        }
    }

    private fun cancelWork(context: Context, id: String) {
        val manager = WorkManager.getInstance(context.applicationContext)
        manager.cancelUniqueWork(WORK_PREFIX + id)
        manager.cancelUniqueWork(FOLLOW_UP_PREFIX + id)
    }
}

class MayraReminderWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val id = inputData.getString(KEY_ID) ?: return Result.failure()
        val expectedRevision = inputData.getLong(KEY_REVISION, MISSING_LONG)
        val expectedDueAt = inputData.getLong(KEY_DUE_AT, MISSING_LONG)
        if (expectedRevision == MISSING_LONG || expectedDueAt == MISSING_LONG) return Result.success()

        val store = MayraReminderStore(applicationContext)
        val reminder = store.find(id) ?: return Result.success()
        if (reminder.revision != expectedRevision || reminder.dueAt != expectedDueAt) return Result.success()
        if (!ReminderLifecyclePolicy.canNotify(reminder.state)) return Result.success()

        val now = System.currentTimeMillis()
        if (reminder.dueAt > now + EARLY_TOLERANCE_MILLIS) {
            MayraReminderRuntime.schedule(applicationContext, reminder, now)
            return Result.success()
        }

        val updated = store.markNotified(
            id = id,
            expectedRevision = expectedRevision,
            expectedDueAt = expectedDueAt,
            now = now
        ) ?: return Result.success()

        if (MayraReminderNotifier.canNotify(applicationContext)) {
            MayraReminderNotifier.show(applicationContext, updated)
        }
        MayraReminderRuntime.scheduleFollowUp(applicationContext, updated)
        return Result.success()
    }

    companion object {
        const val KEY_ID = "reminder_id"
        const val KEY_REVISION = "reminder_revision"
        const val KEY_DUE_AT = "reminder_due_at"
        private const val MISSING_LONG = Long.MIN_VALUE
        private const val EARLY_TOLERANCE_MILLIS = 2_000L
    }
}

class MayraReminderFollowUpWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val id = inputData.getString(MayraReminderWorker.KEY_ID) ?: return Result.failure()
        val expectedRevision = inputData.getLong(MayraReminderWorker.KEY_REVISION, Long.MIN_VALUE)
        if (expectedRevision == Long.MIN_VALUE) return Result.success()

        val store = MayraReminderStore(applicationContext)
        val reminder = store.find(id) ?: return Result.success()
        if (reminder.revision != expectedRevision || reminder.state != ReminderState.DUE) return Result.success()

        val missed = store.markMissed(id) ?: return Result.success()
        if (MayraReminderNotifier.canNotify(applicationContext)) {
            MayraReminderNotifier.show(applicationContext, missed, followUp = true)
        }
        return Result.success()
    }
}

object MayraReminderNotifier {
    private const val CHANNEL_ID = "mayra_reminders"

    fun canNotify(context: Context): Boolean {
        val runtimePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return runtimePermission && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, reminder: MayraReminder, followUp: Boolean = false) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val openIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val completeIntent = actionIntent(context, reminder, MayraReminderActionReceiver.ACTION_COMPLETE, 1)
        val snoozeIntent = actionIntent(context, reminder, MayraReminderActionReceiver.ACTION_SNOOZE, 2)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(if (followUp) "Mayra follow-up" else "Mayra reminder")
            .setContentText(reminder.title)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    if (followUp) "You have not completed this yet: ${reminder.title}"
                    else reminder.detail ?: reminder.title
                )
            )
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

    private fun actionIntent(
        context: Context,
        reminder: MayraReminder,
        action: String,
        salt: Int
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        reminder.id.hashCode() * 31 + salt,
        Intent(context, MayraReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(MayraReminderActionReceiver.EXTRA_ID, reminder.id)
            .putExtra(MayraReminderActionReceiver.EXTRA_REVISION, reminder.revision),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Mayra reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Reminders created and followed up by Mayra"
            }
        )
    }
}

class MayraReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_ID) ?: return
        val expectedRevision = intent.getLongExtra(EXTRA_REVISION, Long.MIN_VALUE)
        val current = MayraReminderStore(context).find(id) ?: return
        if (expectedRevision == Long.MIN_VALUE || current.revision != expectedRevision) return

        when (intent.action) {
            ACTION_COMPLETE -> MayraReminderRuntime.complete(context, id)
            ACTION_SNOOZE -> MayraReminderRuntime.snooze(context, id, Duration.ofMinutes(10))
            ACTION_CANCEL -> MayraReminderRuntime.cancel(context, id)
        }
    }

    companion object {
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_REVISION = "reminder_revision"
        const val ACTION_COMPLETE = "ai.mayra.app.reminder.COMPLETE"
        const val ACTION_SNOOZE = "ai.mayra.app.reminder.SNOOZE"
        const val ACTION_CANCEL = "ai.mayra.app.reminder.CANCEL"
    }
}
