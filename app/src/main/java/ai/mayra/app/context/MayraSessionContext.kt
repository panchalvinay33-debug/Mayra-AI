package ai.mayra.app.context

import android.content.Context
import ai.mayra.app.MayraEntryContract
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Privacy-safe recent Mayra session hint. Stores only entry surface and timestamp; never stores
 * conversation text, external app history, usage events or private content.
 */
data class SessionContextSnapshot(
    val capturedAt: LocalDateTime,
    val access: ContextValue<SessionAggregate> = ContextValue.Unavailable
)

data class SessionAggregate(
    val source: MayraEntryContract.Source,
    val minutesSinceEntry: Int
) {
    init {
        require(minutesSinceEntry >= 0) { "minutesSinceEntry must be non-negative" }
    }
}

class MayraSessionContextStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun record(source: MayraEntryContract.Source, epochMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putString(KEY_SOURCE, source.wireValue)
            .putLong(KEY_EPOCH_MILLIS, epochMillis)
            .apply()
    }

    fun read(
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SessionContextSnapshot {
        val sourceValue = preferences.getString(KEY_SOURCE, null) ?: return SessionContextSnapshot(
            capturedAt = now,
            access = ContextValue.Unavailable
        )
        val epochMillis = preferences.getLong(KEY_EPOCH_MILLIS, -1L)
        if (epochMillis < 0L) return SessionContextSnapshot(now, ContextValue.Unavailable)

        val source = MayraEntryContract.Source.entries.firstOrNull { it.wireValue == sourceValue }
            ?: MayraEntryContract.Source.OTHER
        val entryTime = java.time.Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()
        val minutes = Duration.between(entryTime, now).toMinutes().coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return SessionContextSnapshot(
            capturedAt = now,
            access = ContextValue.Available(
                SessionAggregate(source = source, minutesSinceEntry = minutes),
                ContextSource.SESSION
            )
        )
    }

    private companion object {
        const val PREFERENCES = "mayra_session_context"
        const val KEY_SOURCE = "entry_source"
        const val KEY_EPOCH_MILLIS = "entry_epoch_millis"
    }
}

fun SessionContextSnapshot.summaryLine(): String? = when (val value = access) {
    ContextValue.NotGranted, ContextValue.Unavailable -> null
    is ContextValue.Available -> {
        if (value.value.minutesSinceEntry > 180) return null
        val source = when (value.value.source) {
            MayraEntryContract.Source.LAUNCHER -> "Home"
            MayraEntryContract.Source.VOICE_SESSION -> "Voice"
            MayraEntryContract.Source.OTHER -> "Mayra"
        }
        "$source session · ${value.value.minutesSinceEntry} min ago"
    }
}
