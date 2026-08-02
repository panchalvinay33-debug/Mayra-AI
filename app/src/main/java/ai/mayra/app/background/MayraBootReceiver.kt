package ai.mayra.app.background

import ai.mayra.app.reminder.MayraReminderRuntime
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MayraBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val appContext = context.applicationContext
            runCatching { MayraBackgroundRuntime.initialize(appContext) }
            runCatching { MayraReminderRuntime.rescheduleAll(appContext) }
        }
    }
}
