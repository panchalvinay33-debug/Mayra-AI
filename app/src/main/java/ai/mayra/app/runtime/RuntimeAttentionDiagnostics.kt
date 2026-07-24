package ai.mayra.app.runtime

import android.content.Context

internal enum class RuntimeAttentionScanOutcome {
    ALERT_POSTED,
    NO_NEW_ALERT,
    RUNTIME_UNAVAILABLE,
    SNAPSHOT_FAILED
}

internal data class RuntimeAttentionScanState(
    val completedAt: Long,
    val outcome: RuntimeAttentionScanOutcome
) {
    fun status(now: Long): String {
        if (completedAt <= 0L) return "Background scan has not run yet"
        val ageMinutes = ((now - completedAt).coerceAtLeast(0L) / 60_000L)
        val age = when {
            ageMinutes <= 0L -> "just now"
            ageMinutes == 1L -> "1 min ago"
            else -> "$ageMinutes min ago"
        }
        val result = when (outcome) {
            RuntimeAttentionScanOutcome.ALERT_POSTED -> "alert posted"
            RuntimeAttentionScanOutcome.NO_NEW_ALERT -> "no new alert"
            RuntimeAttentionScanOutcome.RUNTIME_UNAVAILABLE -> "runtime unavailable"
            RuntimeAttentionScanOutcome.SNAPSHOT_FAILED -> "scan failed"
        }
        return "Last background scan: $age · $result"
    }
}

internal class RuntimeAttentionDiagnostics(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun read(): RuntimeAttentionScanState? {
        val completedAt = preferences.getLong(KEY_COMPLETED_AT, 0L)
        val outcomeName = preferences.getString(KEY_OUTCOME, null) ?: return null
        val outcome = runCatching { RuntimeAttentionScanOutcome.valueOf(outcomeName) }.getOrNull() ?: return null
        return RuntimeAttentionScanState(completedAt, outcome)
    }

    fun record(outcome: RuntimeAttentionScanOutcome, completedAt: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_COMPLETED_AT, completedAt)
            .putString(KEY_OUTCOME, outcome.name)
            .apply()
    }

    companion object {
        private const val PREFS = "runtime_attention_diagnostics"
        private const val KEY_COMPLETED_AT = "completed_at"
        private const val KEY_OUTCOME = "outcome"
    }
}
