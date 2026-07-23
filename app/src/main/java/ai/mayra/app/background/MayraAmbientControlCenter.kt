package ai.mayra.app.background

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class AmbientPreferences(
    val notificationIntelligenceEnabled: Boolean = true,
    val retainLocalHistory: Boolean = true,
    val proactiveSuggestionsEnabled: Boolean = true,
    val morningBriefingEnabled: Boolean = true,
    val eveningBriefingEnabled: Boolean = true,
    val sensitiveContentInBriefings: Boolean = false
)

class AmbientPreferenceStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        FILE_NAME,
        Context.MODE_PRIVATE
    )

    fun read(): AmbientPreferences = AmbientPreferences(
        notificationIntelligenceEnabled = preferences.getBoolean(KEY_NOTIFICATION_INTELLIGENCE, true),
        retainLocalHistory = preferences.getBoolean(KEY_RETAIN_HISTORY, true),
        proactiveSuggestionsEnabled = preferences.getBoolean(KEY_PROACTIVE_SUGGESTIONS, true),
        morningBriefingEnabled = preferences.getBoolean(KEY_MORNING_BRIEFING, true),
        eveningBriefingEnabled = preferences.getBoolean(KEY_EVENING_BRIEFING, true),
        sensitiveContentInBriefings = preferences.getBoolean(KEY_SENSITIVE_BRIEFINGS, false)
    )

    fun update(transform: (AmbientPreferences) -> AmbientPreferences): AmbientPreferences {
        val updated = transform(read())
        preferences.edit()
            .putBoolean(KEY_NOTIFICATION_INTELLIGENCE, updated.notificationIntelligenceEnabled)
            .putBoolean(KEY_RETAIN_HISTORY, updated.retainLocalHistory)
            .putBoolean(KEY_PROACTIVE_SUGGESTIONS, updated.proactiveSuggestionsEnabled)
            .putBoolean(KEY_MORNING_BRIEFING, updated.morningBriefingEnabled)
            .putBoolean(KEY_EVENING_BRIEFING, updated.eveningBriefingEnabled)
            .putBoolean(KEY_SENSITIVE_BRIEFINGS, updated.sensitiveContentInBriefings)
            .apply()
        return updated
    }

    private companion object {
        const val FILE_NAME = "mayra_ambient_preferences"
        const val KEY_NOTIFICATION_INTELLIGENCE = "notification_intelligence"
        const val KEY_RETAIN_HISTORY = "retain_history"
        const val KEY_PROACTIVE_SUGGESTIONS = "proactive_suggestions"
        const val KEY_MORNING_BRIEFING = "morning_briefing"
        const val KEY_EVENING_BRIEFING = "evening_briefing"
        const val KEY_SENSITIVE_BRIEFINGS = "sensitive_briefings"
    }
}

data class AmbientHealthSnapshot(
    val notificationAccessGranted: Boolean,
    val listenerEnabled: Boolean,
    val lastHeartbeat: Long,
    val storedEvents: Int,
    val runtimeHealthy: Boolean
)

object MayraAmbientControlCenter {
    fun health(context: Context, now: Long = System.currentTimeMillis()): AmbientHealthSnapshot {
        val appContext = context.applicationContext
        val component = ComponentName(appContext, MayraNotificationListener::class.java)
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(appContext)
        val listenerEnabled = appContext.packageName in enabledPackages
        val eventStore = AmbientEventStore(appContext)
        val lastHeartbeat = eventStore.lastHeartbeat()
        val heartbeatFresh = lastHeartbeat > 0L && now - lastHeartbeat <= HEALTH_WINDOW_MILLIS
        return AmbientHealthSnapshot(
            notificationAccessGranted = listenerEnabled,
            listenerEnabled = listenerEnabled && component.packageName == appContext.packageName,
            lastHeartbeat = lastHeartbeat,
            storedEvents = eventStore.snapshot().size,
            runtimeHealthy = heartbeatFresh
        )
    }

    fun notificationAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun clearLocalAmbientHistory(context: Context) {
        context.applicationContext.getSharedPreferences("mayra_ambient_events", Context.MODE_PRIVATE)
            .edit()
            .remove("events")
            .apply()
    }

    private const val HEALTH_WINDOW_MILLIS = 45L * 60L * 1000L
}
