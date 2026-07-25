package ai.mayra.app.background

import ai.mayra.app.floating.FloatingMayraPreferences
import ai.mayra.app.floating.FloatingMayraService
import ai.mayra.app.reminder.MayraReminderRuntime
import ai.mayra.app.runtime.RuntimeAttentionScheduler
import ai.mayra.app.safety.MayraGlobalStopStore
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

class MayraBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val appContext = context.applicationContext
        val globallyStopped = MayraGlobalStopStore(appContext).isStopped()

        // Owner-created reminders remain commitments and are rescheduled even while Global Stop is active.
        runCatching { MayraReminderRuntime.rescheduleAll(appContext) }

        if (globallyStopped) {
            FloatingMayraPreferences(appContext).enabled = false
            appContext.stopService(Intent(appContext, FloatingMayraService::class.java))
            return
        }

        runCatching { MayraBackgroundRuntime.initialize(appContext) }
        runCatching { RuntimeAttentionScheduler.sync(appContext) }

        val floatingPreferences = FloatingMayraPreferences(appContext)
        if (floatingPreferences.enabled && Settings.canDrawOverlays(appContext)) {
            runCatching {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, FloatingMayraService::class.java)
                        .setAction(FloatingMayraService.ACTION_START)
                )
            }.onFailure {
                floatingPreferences.enabled = false
            }
        }
    }
}
