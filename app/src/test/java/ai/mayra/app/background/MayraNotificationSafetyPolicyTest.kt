package ai.mayra.app.background

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MayraNotificationSafetyPolicyTest {
    @Test
    fun `ignored source is never captured`() {
        val decision = MayraNotificationSafetyPolicy.decide(
            NotificationPrivacyMode.IGNORE, true, NotificationSensitivity.NORMAL,
            "Family", globalStopActive = false, proactiveSuggestionsEnabled = true
        )
        assertFalse(decision.capture)
        assertFalse(decision.registerReply)
        assertFalse(decision.allowProactiveTask)
        assertNull(decision.safeConversationKey)
    }

    @Test
    fun `global stop permits protected local capture but blocks actions`() {
        val decision = MayraNotificationSafetyPolicy.decide(
            NotificationPrivacyMode.FULL, true, NotificationSensitivity.NORMAL,
            "Family", globalStopActive = true, proactiveSuggestionsEnabled = true
        )
        assertTrue(decision.capture)
        assertFalse(decision.registerReply)
        assertFalse(decision.allowProactiveTask)
        assertEquals("global_stop", decision.reason)
    }

    @Test
    fun `otp never becomes replyable or proactive`() {
        val decision = MayraNotificationSafetyPolicy.decide(
            NotificationPrivacyMode.FULL, true, NotificationSensitivity.OTP,
            "Bank OTP 482911", globalStopActive = false, proactiveSuggestionsEnabled = true
        )
        assertFalse(decision.registerReply)
        assertFalse(decision.allowProactiveTask)
        assertEquals("Protected conversation", decision.safeConversationKey)
    }

    @Test
    fun `redacted policy hides conversation identity`() {
        val decision = MayraNotificationSafetyPolicy.decide(
            NotificationPrivacyMode.REDACT_CONTENT, true, NotificationSensitivity.NORMAL,
            "Private family chat", globalStopActive = false, proactiveSuggestionsEnabled = true
        )
        assertEquals("Private conversation", decision.safeConversationKey)
        assertFalse(decision.allowProactiveTask)
    }

    @Test
    fun `normal conversation key removes secret shaped content`() {
        val safe = MayraNotificationSafetyPolicy.sanitizeConversationKey(
            "Verification code 482911", NotificationSensitivity.NORMAL, NotificationPrivacyMode.FULL
        )
        assertFalse(safe.orEmpty().contains("482911"))
        assertTrue(safe.orEmpty().contains("••••"))
    }
}
