package ai.mayra.app.runtime

import android.content.Context

internal data class RuntimeAttentionPreferenceState(
    val enabled: Boolean,
    val snoozedUntil: Long
) {
    fun canNotify(now: Long): Boolean = enabled && now >= snoozedUntil

    fun status(now: Long): String = when {
        !enabled -> "Runtime alerts are off"
        now < snoozedUntil -> "Runtime alerts snoozed for ${remainingMinutes(now)} min"
        else -> "Runtime alerts are on"
    }

    private fun remainingMinutes(now: Long): Long =
        ((snoozedUntil - now).coerceAtLeast(0L) + 59_999L) / 60_000L
}

internal class RuntimeAttentionPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): RuntimeAttentionPreferenceState = RuntimeAttentionPreferenceState(
        enabled = preferences.getBoolean(KEY_ENABLED, true),
        snoozedUntil = preferences.getLong(KEY_SNOOZED_UNTIL, 0L)
    )

    fun setEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun snooze(now: Long, durationMillis: Long = DEFAULT_SNOOZE_MILLIS) {
        preferences.edit()
            .putLong(KEY_SNOOZED_UNTIL, now + durationMillis.coerceAtLeast(0L))
            .apply()
    }

    fun resume() {
        preferences.edit()
            .putBoolean(KEY_ENABLED, true)
            .remove(KEY_SNOOZED_UNTIL)
            .apply()
    }

    companion object {
        private const val PREFS = "runtime_attention_preferences"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SNOOZED_UNTIL = "snoozed_until"
        const val DEFAULT_SNOOZE_MILLIS = 60L * 60L * 1_000L
    }
}
