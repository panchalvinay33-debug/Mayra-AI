package ai.mayra.app.pulse

import ai.mayra.app.device.DeviceAnalysis
import ai.mayra.app.device.DeviceInsight
import ai.mayra.app.device.DeviceSeverity
import ai.mayra.app.device.NetworkTransport
import ai.mayra.app.device.ThermalState

enum class MayraPresence { CALM, ATTENTIVE, BUSY, CONCERNED, OFFLINE }

data class MayraPulseState(
    val presence: MayraPresence,
    val headline: String,
    val message: String,
    val healthScore: Int?,
    val batteryText: String,
    val networkText: String,
    val storageText: String,
    val memoryText: String,
    val capabilityCount: Int,
    val suggestions: List<DeviceInsight>,
    val capturedAt: Long?
)

fun buildMayraPulseState(analysis: DeviceAnalysis?): MayraPulseState {
    if (analysis == null) return MayraPulseState(
        presence = MayraPresence.ATTENTIVE,
        headline = "Mayra is waking up",
        message = "I’m reading this phone’s current state so I can help safely.",
        healthScore = null,
        batteryText = "Checking battery",
        networkText = "Checking connection",
        storageText = "Checking storage",
        memoryText = "Checking memory",
        capabilityCount = 0,
        suggestions = emptyList(),
        capturedAt = null
    )

    val snapshot = analysis.snapshot
    val critical = analysis.insights.any { it.severity == DeviceSeverity.CRITICAL }
    val high = analysis.insights.any { it.severity == DeviceSeverity.HIGH }
    val offline = !snapshot.network.connected || !snapshot.network.validated
    val presence = when {
        critical -> MayraPresence.CONCERNED
        snapshot.thermal in setOf(ThermalState.SEVERE, ThermalState.CRITICAL, ThermalState.EMERGENCY, ThermalState.SHUTDOWN) -> MayraPresence.CONCERNED
        high -> MayraPresence.ATTENTIVE
        offline -> MayraPresence.OFFLINE
        snapshot.memory.lowMemory -> MayraPresence.BUSY
        else -> MayraPresence.CALM
    }
    val (headline, message) = when (presence) {
        MayraPresence.CALM -> "Phone feels healthy" to "Everything important looks steady. I’m ready when you need me."
        MayraPresence.ATTENTIVE -> "I noticed something" to "The phone is usable, but one area deserves your attention."
        MayraPresence.BUSY -> "Phone is working hard" to "I’ll avoid unnecessary heavy work until memory pressure settles."
        MayraPresence.CONCERNED -> "Phone needs care" to "I found a condition that may affect safety or reliability."
        MayraPresence.OFFLINE -> "I’m here offline" to "Local commands still work. Online intelligence will resume with a validated connection."
    }
    val caps = snapshot.capabilities.let {
        listOf(it.camera, it.cameraFlash, it.bluetooth, it.telephony, it.microphone, it.accelerometer,
            it.gyroscope, it.stepCounter, it.biometric, it.notificationListenerDeclared,
            it.exactAlarmDeclared, it.writeSystemSettingsGranted).count(Boolean::not).let { missing -> 12 - missing }
    }
    return MayraPulseState(
        presence = presence,
        headline = headline,
        message = message,
        healthScore = analysis.health.overall,
        batteryText = buildString {
            append("${snapshot.battery.levelPercent}%")
            if (snapshot.battery.charging) append(" · charging")
            if (snapshot.battery.powerSaveMode) append(" · saver on")
        },
        networkText = when {
            !snapshot.network.connected -> "Offline"
            !snapshot.network.validated -> "Connected, internet unverified"
            else -> snapshot.network.transport.label + if (snapshot.network.metered) " · metered" else ""
        },
        storageText = "${snapshot.storage.freePercent}% free",
        memoryText = "${snapshot.memory.availablePercent}% available" + if (snapshot.memory.lowMemory) " · pressure" else "",
        capabilityCount = caps,
        suggestions = analysis.insights.sortedByDescending { it.severity.ordinal }.take(3),
        capturedAt = snapshot.capturedAt
    )
}

private val NetworkTransport.label: String
    get() = when (this) {
        NetworkTransport.WIFI -> "Wi‑Fi online"
        NetworkTransport.CELLULAR -> "Mobile data online"
        NetworkTransport.ETHERNET -> "Ethernet online"
        NetworkTransport.VPN -> "VPN online"
        NetworkTransport.BLUETOOTH -> "Bluetooth network"
        NetworkTransport.NONE -> "Offline"
        NetworkTransport.OTHER -> "Network online"
    }
