package ai.mayra.app.context

import java.time.LocalDateTime

/**
 * J6 Context Fabric foundation.
 *
 * Context is typed and provenance-carrying. The AI may consume this snapshot, but free-form model
 * output is never itself promoted into trusted context.
 */
data class MayraContextSnapshot(
    val capturedAt: LocalDateTime,
    val dayPart: DayPart,
    val connectivity: ContextValue<ConnectivityState> = ContextValue.Unavailable,
    val power: ContextValue<PowerState> = ContextValue.Unavailable
)

enum class DayPart {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

enum class ConnectivityState {
    ONLINE,
    OFFLINE
}

data class PowerState(
    val isCharging: Boolean,
    val batteryPercent: Int?
)

sealed interface ContextValue<out T> {
    data class Available<T>(
        val value: T,
        val source: ContextSource
    ) : ContextValue<T>

    data object NotGranted : ContextValue<Nothing>
    data object Unavailable : ContextValue<Nothing>
}

enum class ContextSource {
    SYSTEM_CLOCK,
    CONNECTIVITY_MANAGER,
    BATTERY_MANAGER,
    REMINDERS,
    NOTIFICATION_ACCESS,
    CONTACTS,
    SESSION
}

internal fun deriveDayPart(hourOfDay: Int): DayPart {
    require(hourOfDay in 0..23) { "hourOfDay must be between 0 and 23" }
    return when (hourOfDay) {
        in 5..11 -> DayPart.MORNING
        in 12..16 -> DayPart.AFTERNOON
        in 17..20 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }
}
