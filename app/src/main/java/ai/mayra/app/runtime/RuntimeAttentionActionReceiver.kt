package ai.mayra.app.runtime

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RuntimeAttentionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val preferences = RuntimeAttentionPreferences(context)
        when (intent.action) {
            ACTION_SNOOZE -> preferences.snooze(System.currentTimeMillis())
            ACTION_DISABLE -> preferences.setEnabled(false)
            ACTION_RESUME -> preferences.resume()
            else -> return
        }
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    companion object {
        const val ACTION_SNOOZE = "ai.mayra.app.runtime.action.SNOOZE"
        const val ACTION_DISABLE = "ai.mayra.app.runtime.action.DISABLE"
        const val ACTION_RESUME = "ai.mayra.app.runtime.action.RESUME"
        const val NOTIFICATION_ID = 4102
    }
}
