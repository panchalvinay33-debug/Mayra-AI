package ai.mayra.app.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MayraDeviceTestCenterTest {
    @Test
    fun `empty session reports not started`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.NOT_TESTED }

        val summary = buildDeviceTestSummary(states, accessReady = 3, accessTotal = 10)

        assertEquals(0, summary.tested)
        assertEquals(DeviceTestId.entries.size, summary.notTested)
        assertEquals(30, summary.accessPercent)
        assertEquals("Device test has not started", summary.headline())
    }

    @Test
    fun `failed checks take headline priority`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.PASSED }.toMutableMap()
        states[DeviceTestId.VOICE_INPUT_OUTPUT] = DeviceTestState.FAILED
        states[DeviceTestId.NOTIFICATION_REPLY] = DeviceTestState.BLOCKED

        val summary = buildDeviceTestSummary(states, accessReady = 8, accessTotal = 10)

        assertEquals(1, summary.failed)
        assertEquals(1, summary.blocked)
        assertEquals("1 checks failed · fix before daily use", summary.headline())
    }

    @Test
    fun `all passed reports complete personal alpha`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.PASSED }

        val summary = buildDeviceTestSummary(states, accessReady = 10, accessTotal = 10)

        assertEquals(100, summary.passPercent)
        assertEquals(100, summary.accessPercent)
        assertEquals("Personal alpha check complete", summary.headline())
    }

    @Test
    fun `access count is clamped to total`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.NOT_TESTED }

        val summary = buildDeviceTestSummary(states, accessReady = 99, accessTotal = 5)

        assertEquals(5, summary.accessReady)
        assertEquals(100, summary.accessPercent)
        assertTrue(summary.total > 10)
    }
}