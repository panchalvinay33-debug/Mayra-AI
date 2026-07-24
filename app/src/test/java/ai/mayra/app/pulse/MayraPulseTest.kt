package ai.mayra.app.pulse

import ai.mayra.app.device.BatterySnapshot
import ai.mayra.app.device.ChargingSource
import ai.mayra.app.device.DeviceAnalysis
import ai.mayra.app.device.DeviceCapabilities
import ai.mayra.app.device.DeviceHealthScore
import ai.mayra.app.device.DeviceInsight
import ai.mayra.app.device.DeviceSeverity
import ai.mayra.app.device.DeviceSnapshot
import ai.mayra.app.device.MemorySnapshot
import ai.mayra.app.device.NetworkSnapshot
import ai.mayra.app.device.NetworkTransport
import ai.mayra.app.device.StorageSnapshot
import ai.mayra.app.device.ThermalState
import ai.mayra.app.presence.MayraPresenceState
import ai.mayra.app.presence.toPresenceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MayraPulseTest {
    @Test fun missingAnalysisShowsWakeUpState() {
        val pulse = buildMayraPulseState(null)
        assertEquals(MayraPresence.ATTENTIVE, pulse.presence)
        assertEquals("Mayra is waking up", pulse.headline)
        assertNull(pulse.healthScore)
    }

    @Test fun healthyPhoneFeelsCalmAndReady() {
        val pulse = buildMayraPulseState(analysis())
        assertEquals(MayraPresence.CALM, pulse.presence)
        assertEquals("Phone feels healthy", pulse.headline)
        assertEquals("82%", pulse.batteryText)
        assertEquals("Wi‑Fi online", pulse.networkText)
        assertEquals(MayraPresenceState.IDLE, pulse.toPresenceState())
    }

    @Test fun offlinePhoneKeepsLocalPresence() {
        val pulse = buildMayraPulseState(analysis(connected = false, validated = false))
        assertEquals(MayraPresence.OFFLINE, pulse.presence)
        assertEquals("I’m here offline", pulse.headline)
        assertEquals(MayraPresenceState.OFFLINE, pulse.toPresenceState())
    }

    @Test fun criticalInsightMakesMayraConcerned() {
        val critical = DeviceInsight(
            code = "battery_critical",
            title = "Battery bahut kam hai",
            message = "Charge now",
            severity = DeviceSeverity.CRITICAL
        )
        val pulse = buildMayraPulseState(analysis(insights = listOf(critical)))
        assertEquals(MayraPresence.CONCERNED, pulse.presence)
        assertEquals("Phone needs care", pulse.headline)
        assertEquals(MayraPresenceState.NEEDS_ATTENTION, pulse.toPresenceState())
    }

    @Test fun healthMeaningUsesHonestBands() {
        assertEquals("Healthy and ready", healthMeaning(95))
        assertEquals("Mostly healthy; keep an eye on small issues", healthMeaning(70))
        assertEquals("Some conditions may slow the phone", healthMeaning(50))
        assertEquals("Phone needs attention before heavy work", healthMeaning(20))
    }

    private fun analysis(
        connected: Boolean = true,
        validated: Boolean = true,
        insights: List<DeviceInsight> = emptyList()
    ): DeviceAnalysis {
        val snapshot = DeviceSnapshot(
            battery = BatterySnapshot(82, false, ChargingSource.NONE),
            storage = StorageSnapshot(1000, 350),
            memory = MemorySnapshot(1000, 400, false),
            thermal = ThermalState.NONE,
            network = NetworkSnapshot(connected, validated, if (connected) NetworkTransport.WIFI else NetworkTransport.NONE, false),
            capabilities = DeviceCapabilities(
                camera = true, cameraFlash = true, bluetooth = true, telephony = true,
                microphone = true, accelerometer = true, gyroscope = true, stepCounter = true,
                biometric = true, notificationListenerDeclared = true, exactAlarmDeclared = true,
                writeSystemSettingsGranted = false
            )
        )
        return DeviceAnalysis(snapshot, DeviceHealthScore(90, 90, 90, 90, 95, 95), insights)
    }
}
