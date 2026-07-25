package ai.mayra.app.testing

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MayraDeviceTestCenterTest {
    @Test
    fun `empty session reports not started`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.NOT_TESTED }

        val summary = buildDeviceTestSummary(states, accessReady = 3, accessTotal = 10)

        assertEquals(20, summary.total)
        assertEquals(0, summary.tested)
        assertEquals(DeviceTestId.entries.size, summary.notTested)
        assertEquals(30, summary.accessPercent)
        assertEquals("Device test has not started", summary.headline())
        assertFalse(summary.alphaAccepted())
    }

    @Test
    fun `mandatory failure takes headline priority`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.PASSED }.toMutableMap()
        states[DeviceTestId.VOICE_INPUT_OUTPUT] = DeviceTestState.FAILED
        states[DeviceTestId.NOTIFICATION_REPLY] = DeviceTestState.BLOCKED

        val summary = buildDeviceTestSummary(states, accessReady = 8, accessTotal = 10)

        assertEquals(1, summary.failed)
        assertEquals(1, summary.blocked)
        assertEquals(1, summary.mandatoryFailed)
        assertEquals("1 mandatory checks failed · fix before daily use", summary.headline())
        assertFalse(summary.alphaAccepted())
    }

    @Test
    fun `optional failure does not become mandatory failure`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.PASSED }.toMutableMap()
        states[DeviceTestId.ONLINE_AI_PROVIDER] = DeviceTestState.FAILED

        val summary = buildDeviceTestSummary(states, accessReady = 10, accessTotal = 10)

        assertEquals(1, summary.failed)
        assertEquals(0, summary.mandatoryFailed)
        assertEquals("1 checks failed · review before daily use", summary.headline())
        assertFalse(summary.alphaAccepted())
    }

    @Test
    fun `all passed reports complete personal alpha`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.PASSED }

        val summary = buildDeviceTestSummary(states, accessReady = 10, accessTotal = 10)

        assertEquals(100, summary.passPercent)
        assertEquals(100, summary.mandatoryPercent)
        assertEquals(100, summary.accessPercent)
        assertEquals("Personal alpha check complete", summary.headline())
        assertTrue(summary.alphaAccepted())
    }

    @Test
    fun `eighty percent mandatory with no failures is alpha eligible`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.NOT_TESTED }.toMutableMap()
        val mandatory = DEVICE_TESTS.filter { it.mandatory }
        mandatory.take((mandatory.size * 80 + 99) / 100).forEach { states[it.id] = DeviceTestState.PASSED }

        val summary = buildDeviceTestSummary(states, accessReady = 8, accessTotal = 10)

        assertTrue(summary.mandatoryPercent >= 80)
        assertEquals(0, summary.failed)
        assertTrue(summary.alphaAccepted())
    }

    @Test
    fun `access count is clamped to total`() {
        val states = DeviceTestId.entries.associateWith { DeviceTestState.NOT_TESTED }

        val summary = buildDeviceTestSummary(states, accessReady = 99, accessTotal = 5)

        assertEquals(5, summary.accessReady)
        assertEquals(100, summary.accessPercent)
        assertEquals(20, summary.total)
    }
}
