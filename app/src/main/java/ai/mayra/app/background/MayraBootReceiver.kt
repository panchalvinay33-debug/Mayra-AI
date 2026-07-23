package ai.mayra.app.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MayraBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            MayraBackgroundRuntime.initialize(context.applicationContext)
        }
    }
}
