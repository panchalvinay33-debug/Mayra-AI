package ai.mayra.app.context

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import java.time.LocalDateTime

/** Lightweight J6 system-context collector. It must never initialize Mayra's model/provider stack. */
fun collectMayraContext(context: Context, now: LocalDateTime = LocalDateTime.now()): MayraContextSnapshot {
    return MayraContextSnapshot(
        capturedAt = now,
        dayPart = deriveDayPart(now.hour),
        connectivity = readConnectivity(context),
        power = readPower(context)
    )
}

private fun readConnectivity(context: Context): ContextValue<ConnectivityState> {
    return runCatching {
        val manager = context.getSystemService(ConnectivityManager::class.java)
            ?: return ContextValue.Unavailable
        val network = manager.activeNetwork ?: return ContextValue.Available(
            ConnectivityState.OFFLINE,
            ContextSource.CONNECTIVITY_MANAGER
        )
        val capabilities = manager.getNetworkCapabilities(network)
        val online = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        ContextValue.Available(
            if (online) ConnectivityState.ONLINE else ConnectivityState.OFFLINE,
            ContextSource.CONNECTIVITY_MANAGER
        )
    }.getOrElse { ContextValue.Unavailable }
}

private fun readPower(context: Context): ContextValue<PowerState> {
    return runCatching {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return ContextValue.Unavailable
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else null
        ContextValue.Available(
            PowerState(
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL,
                batteryPercent = percent
            ),
            ContextSource.BATTERY_MANAGER
        )
    }.getOrElse { ContextValue.Unavailable }
}

fun MayraContextSnapshot.summaryLines(): List<String> {
    val lines = mutableListOf(dayPart.displayLabel())
    when (val value = power) {
        is ContextValue.Available -> {
            val percent = value.value.batteryPercent?.let { "$it%" } ?: "Battery"
            lines += if (value.value.isCharging) "$percent · charging" else percent
        }
        ContextValue.NotGranted -> lines += "Battery permission not granted"
        ContextValue.Unavailable -> lines += "Battery unavailable"
    }
    when (val value = connectivity) {
        is ContextValue.Available -> lines += when (value.value) {
            ConnectivityState.ONLINE -> "Online"
            ConnectivityState.OFFLINE -> "Offline"
        }
        ContextValue.NotGranted -> lines += "Network permission not granted"
        ContextValue.Unavailable -> lines += "Network state unavailable"
    }
    return lines
}

private fun DayPart.displayLabel(): String = when (this) {
    DayPart.MORNING -> "Morning"
    DayPart.AFTERNOON -> "Afternoon"
    DayPart.EVENING -> "Evening"
    DayPart.NIGHT -> "Night"
}
