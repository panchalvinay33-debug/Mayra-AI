package ai.mayra.app.automation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class MayraAutomationPolicyTest {
    private val policy = AutomationSafetyPolicy()

    @Test
    fun communicationActionsRequireConfirmation() {
        val request = AutomationRequest(
            type = AutomationType.COMPOSE_WHATSAPP,
            parameters = mapOf("number" to "919999999999", "message" to "Hello"),
            confirmed = false
        )
        val result = policy.validate(request)
        assertEquals(AutomationStatus.USER_ACTION_REQUIRED, result?.status)
        assertTrue(policy.requiresConfirmation(AutomationType.COMPOSE_SMS))
        assertTrue(policy.requiresConfirmation(AutomationType.DIAL_NUMBER))
    }

    @Test
    fun lowRiskActionsDoNotRequireConfirmation() {
        assertFalse(policy.requiresConfirmation(AutomationType.OPEN_WIFI_SETTINGS))
        assertFalse(policy.requiresConfirmation(AutomationType.WEB_SEARCH))
        assertNull(policy.validate(AutomationRequest(type = AutomationType.OPEN_WIFI_SETTINGS)))
    }

    @Test
    fun expiredRequestsAreRejected() {
        val request = AutomationRequest(
            type = AutomationType.OPEN_FILES,
            createdAt = System.currentTimeMillis() - AutomationSafetyPolicy.REQUEST_TTL_MS - 1
        )
        assertEquals(AutomationStatus.FAILED, policy.validate(request)?.status)
    }

    @Test
    fun excessiveParameterSizeIsRejected() {
        val request = AutomationRequest(
            type = AutomationType.SHARE_TEXT,
            parameters = mapOf("text" to "x".repeat(AutomationSafetyPolicy.MAX_VALUE_LENGTH + 1))
        )
        assertEquals(AutomationStatus.FAILED, policy.validate(request)?.status)
    }

    @Test
    fun parsesExplicitMorningTime() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val parsed = LocalTimeInterpreter.parse("tomorrow 8:30 am", calendar.timeInMillis)
        assertNotNull(parsed)
        assertEquals(8, parsed?.hour)
        assertEquals(30, parsed?.minute)
        val result = Calendar.getInstance().apply { timeInMillis = parsed!!.epochMillis }
        assertEquals(24, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun parsesHinglishDayParts() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(8, LocalTimeInterpreter.parse("kal subah", now)?.hour)
        assertEquals(19, LocalTimeInterpreter.parse("kal shaam", now)?.hour)
        assertEquals(21, LocalTimeInterpreter.parse("raat", now)?.hour)
    }

    @Test
    fun sameDayPastTimeMovesToNextDay() {
        val start = Calendar.getInstance().apply {
            set(2026, Calendar.JULY, 23, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val parsed = LocalTimeInterpreter.parse("7 pm", start.timeInMillis)!!
        val result = Calendar.getInstance().apply { timeInMillis = parsed.epochMillis }
        assertEquals(24, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, result.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun invalidTimeReturnsNull() {
        assertNull(LocalTimeInterpreter.parse("sometime later", 1_000L))
        assertNull(LocalTimeInterpreter.parse("28:99", 1_000L))
    }
}
