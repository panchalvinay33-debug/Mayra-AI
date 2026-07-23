package ai.mayra.app.device

import java.util.ArrayDeque
import java.util.UUID
import kotlin.math.roundToInt

enum class BatteryHealthState { GOOD, WARM, HOT, OVERHEATED, UNKNOWN }
enum class ChargingSource { NONE, AC, USB, WIRELESS, DOCK, UNKNOWN }
enum class ThermalState { NONE, LIGHT, MODERATE, SEVERE, CRITICAL, EMERGENCY, SHUTDOWN, UNKNOWN }
enum class NetworkTransport { NONE, WIFI, CELLULAR, ETHERNET, VPN, BLUETOOTH, OTHER }
enum class DeviceSeverity { INFO, LOW, MEDIUM, HIGH, CRITICAL }
enum class DeviceAction {
    OPEN_BATTERY_SETTINGS,
    OPEN_STORAGE_SETTINGS,
    OPEN_WIFI_SETTINGS,
    OPEN_DATA_SETTINGS,
    OPEN_APP_SETTINGS,
    SUGGEST_CHARGING,
    SUGGEST_CLEANUP,
    SUGGEST_COOLDOWN,
    DEFER_HEAVY_WORK,
    RETRY_WHEN_CONNECTED
}

data class BatterySnapshot(
    val levelPercent: Int,
    val charging: Boolean,
    val source: ChargingSource = ChargingSource.UNKNOWN,
    val temperatureCelsius: Double? = null,
    val voltageMillivolts: Int? = null,
    val health: BatteryHealthState = BatteryHealthState.UNKNOWN,
    val powerSaveMode: Boolean = false,
    val estimatedMinutesRemaining: Int? = null,
    val estimatedMinutesToFull: Int? = null
) {
    init { require(levelPercent in 0..100) }
}

data class StorageSnapshot(
    val totalBytes: Long,
    val freeBytes: Long,
    val cacheBytes: Long? = null
) {
    init {
        require(totalBytes >= 0)
        require(freeBytes in 0..totalBytes)
    }
    val usedBytes: Long get() = totalBytes - freeBytes
    val freePercent: Int get() = if (totalBytes == 0L) 0 else ((freeBytes * 100.0) / totalBytes).roundToInt().coerceIn(0, 100)
}

data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val lowMemory: Boolean
) {
    init {
        require(totalBytes >= 0)
        require(availableBytes in 0..totalBytes)
    }
    val availablePercent: Int get() = if (totalBytes == 0L) 0 else ((availableBytes * 100.0) / totalBytes).roundToInt().coerceIn(0, 100)
}

data class NetworkSnapshot(
    val connected: Boolean,
    val validated: Boolean,
    val transport: NetworkTransport,
    val metered: Boolean,
    val roaming: Boolean = false,
    val downstreamKbps: Int? = null,
    val upstreamKbps: Int? = null
)

data class DeviceCapabilities(
    val camera: Boolean,
    val cameraFlash: Boolean,
    val bluetooth: Boolean,
    val telephony: Boolean,
    val microphone: Boolean,
    val accelerometer: Boolean,
    val gyroscope: Boolean,
    val stepCounter: Boolean,
    val biometric: Boolean,
    val notificationListenerDeclared: Boolean,
    val exactAlarmDeclared: Boolean,
    val writeSystemSettingsGranted: Boolean
)

data class DeviceSnapshot(
    val id: String = UUID.randomUUID().toString(),
    val capturedAt: Long = System.currentTimeMillis(),
    val battery: BatterySnapshot,
    val storage: StorageSnapshot,
    val memory: MemorySnapshot,
    val thermal: ThermalState,
    val network: NetworkSnapshot,
    val capabilities: DeviceCapabilities
)

data class DeviceInsight(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val title: String,
    val message: String,
    val severity: DeviceSeverity,
    val action: DeviceAction? = null,
    val dedupeKey: String = code,
    val createdAt: Long = System.currentTimeMillis()
)

data class DeviceHealthScore(
    val overall: Int,
    val battery: Int,
    val storage: Int,
    val memory: Int,
    val thermal: Int,
    val connectivity: Int
)

data class DeviceDiagnostics(
    val snapshots: Int,
    val generatedInsights: Long,
    val suppressedDuplicates: Long,
    val lastCapturedAt: Long?,
    val activeCriticalInsights: Int
)

data class DeviceIntelligencePolicy(
    val lowBatteryPercent: Int = 20,
    val criticalBatteryPercent: Int = 10,
    val lowStorageFreePercent: Int = 10,
    val criticalStorageFreePercent: Int = 5,
    val lowMemoryAvailablePercent: Int = 10,
    val duplicateWindowMs: Long = 30 * 60 * 1000L,
    val snapshotRetentionMs: Long = 7L * 24 * 60 * 60 * 1000,
    val maxSnapshots: Int = 200
) {
    init {
        require(criticalBatteryPercent in 1 until lowBatteryPercent)
        require(criticalStorageFreePercent in 1 until lowStorageFreePercent)
        require(lowMemoryAvailablePercent in 1..50)
        require(duplicateWindowMs >= 1_000)
        require(snapshotRetentionMs >= 60_000)
        require(maxSnapshots in 10..2_000)
    }
}

class ChargingEstimator {
    fun estimateMinutesToFull(history: List<DeviceSnapshot>, current: DeviceSnapshot): Int? {
        if (!current.battery.charging || current.battery.levelPercent >= 100) return null
        val recent = history.asSequence()
            .filter { it.battery.charging && it.capturedAt < current.capturedAt }
            .sortedByDescending(DeviceSnapshot::capturedAt)
            .take(12)
            .toList()
            .sortedBy(DeviceSnapshot::capturedAt)
        val first = recent.firstOrNull() ?: return null
        val gained = current.battery.levelPercent - first.battery.levelPercent
        val minutes = (current.capturedAt - first.capturedAt) / 60_000.0
        if (gained <= 0 || minutes < 2) return null
        val ratePerMinute = gained / minutes
        return ((100 - current.battery.levelPercent) / ratePerMinute).roundToInt().coerceIn(1, 24 * 60)
    }

    fun estimateMinutesRemaining(history: List<DeviceSnapshot>, current: DeviceSnapshot): Int? {
        if (current.battery.charging || current.battery.levelPercent <= 0) return null
        val recent = history.asSequence()
            .filter { !it.battery.charging && it.capturedAt < current.capturedAt }
            .sortedByDescending(DeviceSnapshot::capturedAt)
            .take(12)
            .toList()
            .sortedBy(DeviceSnapshot::capturedAt)
        val first = recent.firstOrNull() ?: return null
        val drained = first.battery.levelPercent - current.battery.levelPercent
        val minutes = (current.capturedAt - first.capturedAt) / 60_000.0
        if (drained <= 0 || minutes < 2) return null
        val ratePerMinute = drained / minutes
        return (current.battery.levelPercent / ratePerMinute).roundToInt().coerceIn(1, 7 * 24 * 60)
    }
}

class DeviceHealthScorer {
    fun score(snapshot: DeviceSnapshot): DeviceHealthScore {
        val battery = when {
            snapshot.battery.health == BatteryHealthState.OVERHEATED -> 10
            snapshot.battery.levelPercent <= 10 && !snapshot.battery.charging -> 25
            snapshot.battery.levelPercent <= 20 && !snapshot.battery.charging -> 55
            else -> 90
        }
        val storage = when {
            snapshot.storage.freePercent <= 5 -> 15
            snapshot.storage.freePercent <= 10 -> 45
            snapshot.storage.freePercent <= 20 -> 70
            else -> 95
        }
        val memory = when {
            snapshot.memory.lowMemory -> 25
            snapshot.memory.availablePercent <= 10 -> 40
            snapshot.memory.availablePercent <= 20 -> 70
            else -> 90
        }
        val thermal = when (snapshot.thermal) {
            ThermalState.SHUTDOWN, ThermalState.EMERGENCY -> 0
            ThermalState.CRITICAL -> 15
            ThermalState.SEVERE -> 35
            ThermalState.MODERATE -> 65
            ThermalState.LIGHT -> 80
            ThermalState.NONE -> 95
            ThermalState.UNKNOWN -> 75
        }
        val connectivity = when {
            !snapshot.network.connected -> 30
            !snapshot.network.validated -> 50
            snapshot.network.metered -> 75
            else -> 95
        }
        return DeviceHealthScore(
            overall = listOf(battery, storage, memory, thermal, connectivity).average().roundToInt(),
            battery = battery,
            storage = storage,
            memory = memory,
            thermal = thermal,
            connectivity = connectivity
        )
    }
}

class DeviceSuggestionPlanner(private val policy: DeviceIntelligencePolicy = DeviceIntelligencePolicy()) {
    fun plan(snapshot: DeviceSnapshot): List<DeviceInsight> {
        val insights = mutableListOf<DeviceInsight>()
        val battery = snapshot.battery
        when {
            battery.levelPercent <= policy.criticalBatteryPercent && !battery.charging -> insights += DeviceInsight(
                code = "battery_critical", title = "Battery bahut kam hai",
                message = "Battery ${battery.levelPercent}% hai. Charger connect karna best rahega.",
                severity = DeviceSeverity.CRITICAL, action = DeviceAction.SUGGEST_CHARGING
            )
            battery.levelPercent <= policy.lowBatteryPercent && !battery.charging -> insights += DeviceInsight(
                code = "battery_low", title = "Battery low hai",
                message = "Battery ${battery.levelPercent}% hai. Battery Saver ya charging consider karein.",
                severity = DeviceSeverity.HIGH, action = DeviceAction.OPEN_BATTERY_SETTINGS
            )
        }
        if (snapshot.storage.freePercent <= policy.criticalStorageFreePercent) insights += DeviceInsight(
            code = "storage_critical", title = "Storage almost full hai",
            message = "Sirf ${snapshot.storage.freePercent}% storage free hai. Cleanup urgently recommended hai.",
            severity = DeviceSeverity.CRITICAL, action = DeviceAction.OPEN_STORAGE_SETTINGS
        ) else if (snapshot.storage.freePercent <= policy.lowStorageFreePercent) insights += DeviceInsight(
            code = "storage_low", title = "Storage kam ho rahi hai",
            message = "${snapshot.storage.freePercent}% storage free hai. Downloads aur large files review karein.",
            severity = DeviceSeverity.HIGH, action = DeviceAction.SUGGEST_CLEANUP
        )
        if (snapshot.memory.lowMemory || snapshot.memory.availablePercent <= policy.lowMemoryAvailablePercent) insights += DeviceInsight(
            code = "memory_pressure", title = "Memory pressure detect hui",
            message = "Heavy background work ko thodi der defer karna safer hai.",
            severity = DeviceSeverity.MEDIUM, action = DeviceAction.DEFER_HEAVY_WORK
        )
        if (snapshot.thermal in setOf(ThermalState.SEVERE, ThermalState.CRITICAL, ThermalState.EMERGENCY, ThermalState.SHUTDOWN)) insights += DeviceInsight(
            code = "thermal_high", title = "Phone zyada garam hai",
            message = "Heavy tasks rok dein aur device ko cool hone dein.",
            severity = if (snapshot.thermal in setOf(ThermalState.EMERGENCY, ThermalState.SHUTDOWN)) DeviceSeverity.CRITICAL else DeviceSeverity.HIGH,
            action = DeviceAction.SUGGEST_COOLDOWN
        )
        if (!snapshot.network.connected) insights += DeviceInsight(
            code = "network_offline", title = "Internet unavailable hai",
            message = "Network-required work ko connection aane par retry kiya ja sakta hai.",
            severity = DeviceSeverity.MEDIUM, action = DeviceAction.RETRY_WHEN_CONNECTED
        ) else if (!snapshot.network.validated) insights += DeviceInsight(
            code = "network_unvalidated", title = "Internet connection verify nahi hua",
            message = "Wi-Fi connected hai, lekin internet access confirm nahi hai.",
            severity = DeviceSeverity.LOW, action = DeviceAction.OPEN_WIFI_SETTINGS
        )
        if (snapshot.network.metered) insights += DeviceInsight(
            code = "network_metered", title = "Metered network active hai",
            message = "Large sync ya download mobile data use kar sakta hai.",
            severity = DeviceSeverity.INFO
        )
        return insights
    }
}

class MayraDeviceIntelligence(
    private val policy: DeviceIntelligencePolicy = DeviceIntelligencePolicy(),
    private val planner: DeviceSuggestionPlanner = DeviceSuggestionPlanner(policy),
    private val estimator: ChargingEstimator = ChargingEstimator(),
    private val scorer: DeviceHealthScorer = DeviceHealthScorer(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val snapshots = ArrayDeque<DeviceSnapshot>()
    private val recentInsightKeys = linkedMapOf<String, Long>()
    private var generatedInsights = 0L
    private var suppressedDuplicates = 0L

    @Synchronized
    fun ingest(raw: DeviceSnapshot): DeviceAnalysis {
        prune()
        val history = snapshots.toList()
        val enhancedBattery = raw.battery.copy(
            estimatedMinutesToFull = raw.battery.estimatedMinutesToFull ?: estimator.estimateMinutesToFull(history, raw),
            estimatedMinutesRemaining = raw.battery.estimatedMinutesRemaining ?: estimator.estimateMinutesRemaining(history, raw)
        )
        val snapshot = raw.copy(battery = enhancedBattery)
        snapshots.addLast(snapshot)
        while (snapshots.size > policy.maxSnapshots) snapshots.removeFirst()

        val fresh = planner.plan(snapshot).filter { insight ->
            val previous = recentInsightKeys[insight.dedupeKey]
            if (previous != null && snapshot.capturedAt - previous < policy.duplicateWindowMs) {
                suppressedDuplicates++
                false
            } else {
                recentInsightKeys[insight.dedupeKey] = snapshot.capturedAt
                true
            }
        }
        generatedInsights += fresh.size
        return DeviceAnalysis(snapshot, scorer.score(snapshot), fresh)
    }

    @Synchronized
    fun recent(limit: Int = 20): List<DeviceSnapshot> = snapshots.toList().takeLast(limit.coerceIn(1, policy.maxSnapshots)).reversed()

    @Synchronized
    fun diagnostics(): DeviceDiagnostics = DeviceDiagnostics(
        snapshots = snapshots.size,
        generatedInsights = generatedInsights,
        suppressedDuplicates = suppressedDuplicates,
        lastCapturedAt = snapshots.lastOrNull()?.capturedAt,
        activeCriticalInsights = planner.plan(snapshots.lastOrNull() ?: return DeviceDiagnostics(0, generatedInsights, suppressedDuplicates, null, 0))
            .count { it.severity == DeviceSeverity.CRITICAL }
    )

    @Synchronized
    fun clear() {
        snapshots.clear()
        recentInsightKeys.clear()
    }

    @Synchronized
    private fun prune() {
        val cutoff = now() - policy.snapshotRetentionMs
        while (snapshots.firstOrNull()?.capturedAt?.let { it < cutoff } == true) snapshots.removeFirst()
        recentInsightKeys.entries.removeAll { it.value < now() - policy.duplicateWindowMs }
    }
}

data class DeviceAnalysis(
    val snapshot: DeviceSnapshot,
    val health: DeviceHealthScore,
    val insights: List<DeviceInsight>
)
