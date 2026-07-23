package ai.mayra.app.device

import android.content.Context
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

data class DeviceWorkGate(
    val allowed: Boolean,
    val reasons: List<String>,
    val retryWhenConnected: Boolean = false,
    val retryAfterCooldown: Boolean = false
)

data class DeviceRuntimeSnapshot(
    val latest: DeviceAnalysis?,
    val recentInsights: List<DeviceInsight>,
    val diagnostics: DeviceRuntimeDiagnostics
)

data class DeviceRuntimeDiagnostics(
    val captures: Long,
    val captureFailures: Long,
    val throttledCaptures: Long,
    val actionLaunches: Long,
    val lastCaptureAt: Long?,
    val intelligence: DeviceDiagnostics
)

class MayraDeviceRuntime(
    private val provider: DeviceStateProvider,
    private val intelligence: MayraDeviceIntelligence = MayraDeviceIntelligence(),
    private val actionLauncher: ((DeviceAction) -> Boolean)? = null,
    private val minCaptureIntervalMs: Long = 15_000,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val insights = ArrayDeque<DeviceInsight>()
    private val captures = AtomicLong(0)
    private val failures = AtomicLong(0)
    private val throttled = AtomicLong(0)
    private val actionLaunches = AtomicLong(0)
    @Volatile private var latest: DeviceAnalysis? = null
    @Volatile private var lastCaptureAt: Long? = null

    init { require(minCaptureIntervalMs in 0..60 * 60 * 1000L) }

    @Synchronized
    fun capture(force: Boolean = false): Result<DeviceAnalysis> {
        val timestamp = now()
        val previous = lastCaptureAt
        if (!force && previous != null && timestamp - previous < minCaptureIntervalMs) {
            throttled.incrementAndGet()
            return latest?.let(Result.Companion::success)
                ?: Result.failure(IllegalStateException("Device capture is throttled."))
        }
        return runCatching {
            val analysis = intelligence.ingest(provider.capture())
            latest = analysis
            lastCaptureAt = timestamp
            captures.incrementAndGet()
            analysis.insights.forEach(insights::addLast)
            while (insights.size > MAX_INSIGHTS) insights.removeFirst()
            analysis
        }.onFailure { failures.incrementAndGet() }
    }

    fun latest(): DeviceAnalysis? = latest

    @Synchronized
    fun recentInsights(limit: Int = 20): List<DeviceInsight> =
        insights.toList().takeLast(limit.coerceIn(1, MAX_INSIGHTS)).reversed()

    fun gateHeavyWork(requireNetwork: Boolean, allowMetered: Boolean = false): DeviceWorkGate {
        val current = latest ?: return DeviceWorkGate(false, listOf("Device state has not been captured yet."))
        val reasons = mutableListOf<String>()
        var retryNetwork = false
        var retryThermal = false
        if (current.snapshot.battery.levelPercent <= 10 && !current.snapshot.battery.charging) {
            reasons += "Battery is critically low."
        }
        if (current.snapshot.memory.lowMemory) reasons += "Android reports low memory."
        if (current.snapshot.thermal in setOf(ThermalState.SEVERE, ThermalState.CRITICAL, ThermalState.EMERGENCY, ThermalState.SHUTDOWN)) {
            reasons += "Device thermal state is too high."
            retryThermal = true
        }
        if (requireNetwork && (!current.snapshot.network.connected || !current.snapshot.network.validated)) {
            reasons += "Validated internet connection is unavailable."
            retryNetwork = true
        }
        if (requireNetwork && current.snapshot.network.metered && !allowMetered) {
            reasons += "Only a metered network is available."
        }
        return DeviceWorkGate(reasons.isEmpty(), reasons, retryNetwork, retryThermal)
    }

    fun launchSuggestedAction(action: DeviceAction, confirmed: Boolean): Boolean {
        if (!confirmed) return false
        val launched = actionLauncher?.invoke(action) == true
        if (launched) actionLaunches.incrementAndGet()
        return launched
    }

    fun snapshot(): DeviceRuntimeSnapshot = DeviceRuntimeSnapshot(
        latest = latest,
        recentInsights = recentInsights(),
        diagnostics = DeviceRuntimeDiagnostics(
            captures = captures.get(),
            captureFailures = failures.get(),
            throttledCaptures = throttled.get(),
            actionLaunches = actionLaunches.get(),
            lastCaptureAt = lastCaptureAt,
            intelligence = intelligence.diagnostics()
        )
    )

    @Synchronized
    fun clearHistory() {
        insights.clear()
        intelligence.clear()
        latest = null
        lastCaptureAt = null
    }

    companion object { private const val MAX_INSIGHTS = 100 }
}

fun androidDeviceRuntime(context: Context): MayraDeviceRuntime {
    val appContext = context.applicationContext
    val intents = DeviceActionIntentFactory(appContext)
    return MayraDeviceRuntime(
        provider = AndroidDeviceStateProvider(appContext),
        actionLauncher = { action ->
            intents.intentFor(action)?.let { intent ->
                runCatching { appContext.startActivity(intent) }.isSuccess
            } ?: false
        }
    )
}
