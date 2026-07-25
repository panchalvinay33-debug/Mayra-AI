package ai.mayra.app.diagnostics

import android.content.Context

/**
 * Persists bounded, owner-visible startup diagnostics without storing user content.
 * A non-critical subsystem failure must be recorded instead of crashing Mayra's launcher.
 */
class MayraStartupHealth(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun begin(now: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_LAST_START_AT, now)
            .putBoolean(KEY_LAST_START_COMPLETED, false)
            .putStringSet(KEY_FAILED_STEPS, emptySet())
            .apply()
    }

    fun complete(now: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_LAST_COMPLETED_AT, now)
            .putBoolean(KEY_LAST_START_COMPLETED, true)
            .apply()
    }

    fun recordFailure(step: String, throwable: Throwable, now: Long = System.currentTimeMillis()) {
        val normalizedStep = step.trim().take(80).ifBlank { "unknown" }
        val failures = preferences.getStringSet(KEY_FAILED_STEPS, emptySet()).orEmpty().toMutableSet()
        failures += normalizedStep
        preferences.edit()
            .putStringSet(KEY_FAILED_STEPS, failures.take(MAX_FAILURES).toSet())
            .putString(KEY_LAST_ERROR_STEP, normalizedStep)
            .putString(KEY_LAST_ERROR_TYPE, throwable::class.java.simpleName.take(80))
            .putString(KEY_LAST_ERROR_MESSAGE, throwable.message.orEmpty().replace('\n', ' ').take(240))
            .putLong(KEY_LAST_ERROR_AT, now)
            .apply()
    }

    fun snapshot(): StartupHealthSnapshot = StartupHealthSnapshot(
        lastStartAt = preferences.getLong(KEY_LAST_START_AT, 0L),
        lastCompletedAt = preferences.getLong(KEY_LAST_COMPLETED_AT, 0L),
        lastStartCompleted = preferences.getBoolean(KEY_LAST_START_COMPLETED, true),
        failedSteps = preferences.getStringSet(KEY_FAILED_STEPS, emptySet()).orEmpty().sorted(),
        lastErrorStep = preferences.getString(KEY_LAST_ERROR_STEP, null),
        lastErrorType = preferences.getString(KEY_LAST_ERROR_TYPE, null),
        lastErrorMessage = preferences.getString(KEY_LAST_ERROR_MESSAGE, null),
        lastErrorAt = preferences.getLong(KEY_LAST_ERROR_AT, 0L)
    )

    inline fun safeStep(name: String, block: () -> Unit): Boolean = try {
        block()
        true
    } catch (error: Throwable) {
        recordFailure(name, error)
        false
    }

    private companion object {
        const val FILE_NAME = "mayra_startup_health"
        const val KEY_LAST_START_AT = "last_start_at"
        const val KEY_LAST_COMPLETED_AT = "last_completed_at"
        const val KEY_LAST_START_COMPLETED = "last_start_completed"
        const val KEY_FAILED_STEPS = "failed_steps"
        const val KEY_LAST_ERROR_STEP = "last_error_step"
        const val KEY_LAST_ERROR_TYPE = "last_error_type"
        const val KEY_LAST_ERROR_MESSAGE = "last_error_message"
        const val KEY_LAST_ERROR_AT = "last_error_at"
        const val MAX_FAILURES = 20
    }
}

data class StartupHealthSnapshot(
    val lastStartAt: Long,
    val lastCompletedAt: Long,
    val lastStartCompleted: Boolean,
    val failedSteps: List<String>,
    val lastErrorStep: String?,
    val lastErrorType: String?,
    val lastErrorMessage: String?,
    val lastErrorAt: Long
) {
    val degraded: Boolean get() = failedSteps.isNotEmpty()
    val previousStartInterrupted: Boolean get() = lastStartAt > 0L && !lastStartCompleted

    fun ownerSummary(): String = when {
        previousStartInterrupted -> "Previous Mayra startup did not finish. Core chat should remain available while diagnostics are reviewed."
        degraded -> "Mayra started in degraded mode. Unavailable parts: ${failedSteps.joinToString()}."
        else -> "Mayra startup completed normally."
    }
}
