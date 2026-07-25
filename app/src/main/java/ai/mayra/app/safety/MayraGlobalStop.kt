package ai.mayra.app.safety

import android.content.Context

/**
 * Persistent owner-controlled safety boundary.
 *
 * Global stop survives process death, reboot and app update. It intentionally stores only the
 * boolean stop state and bounded operational metadata; no conversation or personal content.
 */
class MayraGlobalStopStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun snapshot(): MayraGlobalStopSnapshot = MayraGlobalStopSnapshot(
        stopped = preferences.getBoolean(KEY_STOPPED, false),
        changedAt = preferences.getLong(KEY_CHANGED_AT, 0L),
        reason = preferences.getString(KEY_REASON, null)?.take(160),
        generation = preferences.getLong(KEY_GENERATION, 0L)
    )

    @Synchronized
    fun stop(reason: String = DEFAULT_REASON, now: Long = System.currentTimeMillis()): MayraGlobalStopSnapshot {
        val previous = snapshot()
        preferences.edit()
            .putBoolean(KEY_STOPPED, true)
            .putLong(KEY_CHANGED_AT, now)
            .putString(KEY_REASON, normalizeReason(reason))
            .putLong(KEY_GENERATION, previous.generation + 1L)
            .commit()
        return snapshot()
    }

    @Synchronized
    fun resume(reason: String = "Owner resumed Mayra.", now: Long = System.currentTimeMillis()): MayraGlobalStopSnapshot {
        val previous = snapshot()
        preferences.edit()
            .putBoolean(KEY_STOPPED, false)
            .putLong(KEY_CHANGED_AT, now)
            .putString(KEY_REASON, normalizeReason(reason))
            .putLong(KEY_GENERATION, previous.generation + 1L)
            .commit()
        return snapshot()
    }

    fun isStopped(): Boolean = preferences.getBoolean(KEY_STOPPED, false)

    private fun normalizeReason(value: String): String = value
        .replace('\n', ' ')
        .trim()
        .take(160)
        .ifBlank { DEFAULT_REASON }

    private companion object {
        const val FILE_NAME = "mayra_global_stop"
        const val KEY_STOPPED = "stopped"
        const val KEY_CHANGED_AT = "changed_at"
        const val KEY_REASON = "reason"
        const val KEY_GENERATION = "generation"
        const val DEFAULT_REASON = "Owner stopped Mayra."
    }
}

data class MayraGlobalStopSnapshot(
    val stopped: Boolean,
    val changedAt: Long,
    val reason: String?,
    val generation: Long
) {
    fun ownerSummary(): String = if (stopped) {
        "Mayra is globally stopped. Phone actions and automatic companion startup remain blocked until the owner resumes."
    } else {
        "Mayra global stop is not active. Normal safety and confirmation rules still apply."
    }
}
