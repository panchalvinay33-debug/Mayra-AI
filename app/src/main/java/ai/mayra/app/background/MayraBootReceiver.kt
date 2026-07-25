package ai.mayra.app.background

import ai.mayra.app.floating.FloatingMayraPreferences
import ai.mayra.app.floating.FloatingMayraService
import ai.mayra.app.reminder.MayraReminderRuntime
import ai.mayra.app.runtime.RuntimeAttentionScheduler
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.content.ContextCompat

class MayraBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val appContext = context.applicationContext
            MayraBackgroundRuntime.initialize(appContext)
            RuntimeAttentionScheduler.sync(appContext)
            MayraReminderRuntime.rescheduleAll(appContext)

            val floatingPreferences = FloatingMayraPreferences(appContext)
            if (floatingPreferences.enabled && Settings.canDrawOverlays(appContext)) {
                ContextCompat.startForegroundService(
                    appContext,
                    Intent(appContext, FloatingMayraService::class.java)
                        .setAction(FloatingMayraService.ACTION_START)
                )
            }
        }
    }
}
