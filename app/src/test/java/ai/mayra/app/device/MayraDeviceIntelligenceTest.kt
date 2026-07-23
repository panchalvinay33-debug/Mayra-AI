package ai.mayra.app.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraDeviceIntelligenceTest {
    @Test
    fun `critical battery and storage produce urgent insights`() {
        val engine = MayraDeviceIntelligence(now = { 1_000L })
        val analysis = engine.ingest(snapshot(
            at = 1_000L,
            battery = battery(level = 7),
            storage = storage(freePercent = 3)
        ))

        assertTrue(analysis.insights.any { it.code == "battery_critical" && it.severity == DeviceSeverity.CRITICAL })
        assertTrue(analysis.insights.any { it.code == "storage_critical" && it.action == DeviceAction.OPEN_STORAGE_SETTINGS })
        assertTrue(analysis.health.overall < 70)
    }

    @Test
    fun `duplicate insight is suppressed inside policy window`() {
        var clock = 1_000L
        val engine = MayraDeviceIntelligence(
            policy = DeviceIntelligencePolicy(duplicateWindowMs = 10_000L),
            now = { clock }
        )
        engine.ingest(snapshot(at = clock, battery = battery(level = 15)))
        clock += 500
        val second = engine.ingest(snapshot(at = clock, battery = battery(level = 14)))

        assertTrue(second.insights.none { it.code == "battery_low" })
        assertEquals(1L, engine.diagnostics().suppressedDuplicates)
    }

    @Test
    fun `charging estimator derives bounded minutes to full`() {
        val estimator = ChargingEstimator()
        val first = snapshot(at = 0L, battery = battery(level = 40, charging = true))
        val current = snapshot(at = 20 * 60_000L, battery = battery(level = 60, charging = true))

        assertEquals(40, estimator.estimateMinutesToFull(listOf(first), current))
    }

    @Test
    fun `discharge estimator derives remaining minutes`() {
        val estimator = ChargingEstimator()
        val first = snapshot(at = 0L, battery = battery(level = 80))
        val current = snapshot(at = 30 * 60_000L, battery = battery(level = 70))

        assertEquals(210, estimator.estimateMinutesRemaining(listOf(first), current))
    }

    @Test
    fun `thermal and offline state block heavy network work`() {
        val provider = FakeProvider(snapshot(
            at = 1_000L,
            thermal = ThermalState.SEVERE,
            network = network(connected = false, validated = false)
        ))
        val runtime = MayraDeviceRuntime(provider, minCaptureIntervalMs = 0, now = { 1_000L })
        runtime.capture(force = true)

        val gate = runtime.gateHeavyWork(requireNetwork = true)

        assertFalse(gate.allowed)
        assertTrue(gate.retryWhenConnected)
        assertTrue(gate.retryAfterCooldown)
    }

    @Test
    fun `metered network blocks heavy work unless allowed`() {
        val provider = FakeProvider(snapshot(
            at = 1_000L,
            network = network(connected = true, validated = true, metered = true)
        ))
        val runtime = MayraDeviceRuntime(provider, minCaptureIntervalMs = 0, now = { 1_000L })
        runtime.capture(force = true)

        assertFalse(runtime.gateHeavyWork(requireNetwork = true).allowed)
        assertTrue(runtime.gateHeavyWork(requireNetwork = true, allowMetered = true).allowed)
    }

    @Test
    fun `capture throttling returns latest analysis without recapture`() {
        var clock = 1_000L
        val provider = FakeProvider(snapshot(at = clock))
        val runtime = MayraDeviceRuntime(provider, minCaptureIntervalMs = 10_000L, now = { clock })

        val first = runtime.capture().getOrThrow()
        clock += 100
        val second = runtime.capture().getOrThrow()

        assertEquals(first.snapshot.id, second.snapshot.id)
        assertEquals(1, provider.calls)
        assertEquals(1L, runtime.snapshot().diagnostics.throttledCaptures)
    }

    @Test
    fun `confirmed action can launch and unconfirmed action cannot`() {
        var launches = 0
        val runtime = MayraDeviceRuntime(
            provider = FakeProvider(snapshot(at = 1_000L)),
            actionLauncher = { launches++; true },
            minCaptureIntervalMs = 0,
            now = { 1_000L }
        )

        assertFalse(runtime.launchSuggestedAction(DeviceAction.OPEN_WIFI_SETTINGS, confirmed = false))
        assertTrue(runtime.launchSuggestedAction(DeviceAction.OPEN_WIFI_SETTINGS, confirmed = true))
        assertEquals(1, launches)
        assertEquals(1L, runtime.snapshot().diagnostics.actionLaunches)
    }

    @Test
    fun `healthy device receives strong score and no urgent insight`() {
        val analysis = MayraDeviceIntelligence(now = { 1_000L }).ingest(snapshot(at = 1_000L))

        assertTrue(analysis.health.overall >= 85)
        assertTrue(analysis.insights.none { it.severity in setOf(DeviceSeverity.HIGH, DeviceSeverity.CRITICAL) })
        assertNotNull(analysis.snapshot.capabilities)
    }

    private class FakeProvider(private var value: DeviceSnapshot) : DeviceStateProvider {
        var calls: Int = 0
        override fun capture(): DeviceSnapshot {
            calls++
            return value
        }
    }

    private fun snapshot(
        at: Long,
        battery: BatterySnapshot = battery(),
        storage: StorageSnapshot = storage(),
        memory: MemorySnapshot = MemorySnapshot(8_000, 4_000, false),
        thermal: ThermalState = ThermalState.NONE,
        network: NetworkSnapshot = network()
    ) = DeviceSnapshot(
        id = "snapshot-$at",
        capturedAt = at,
        battery = battery,
        storage = storage,
        memory = memory,
        thermal = thermal,
        network = network,
        capabilities = DeviceCapabilities(
            camera = true,
            cameraFlash = true,
            bluetooth = true,
            telephony = true,
            microphone = true,
            accelerometer = true,
            gyroscope = true,
            stepCounter = false,
            biometric = true,
            notificationListenerDeclared = true,
            exactAlarmDeclared = true,
            writeSystemSettingsGranted = false
        )
    )

    private fun battery(level: Int = 80, charging: Boolean = false) = BatterySnapshot(
        levelPercent = level,
        charging = charging,
        source = if (charging) ChargingSource.AC else ChargingSource.NONE,
        health = BatteryHealthState.GOOD
    )

    private fun storage(freePercent: Int = 50): StorageSnapshot {
        val total = 10_000L
        return StorageSnapshot(total, total * freePercent / 100)
    }

    private fun network(
        connected: Boolean = true,
        validated: Boolean = true,
        metered: Boolean = false
    ) = NetworkSnapshot(
        connected = connected,
        validated = validated,
        transport = if (connected) NetworkTransport.WIFI else NetworkTransport.NONE,
        metered = metered
    )
}
