package ai.mayra.app.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraNotificationIntelligenceTest {
    @Test
    fun `otp content is classified and hidden`() {
        val sensitivity = NotificationContentGuard.classify(
            title = "Verification code",
            text = "Your OTP is 482911",
            secretVisibility = false
        )
        val sanitized = NotificationContentGuard.sanitize(
            title = "Verification code",
            text = "Your OTP is 482911",
            sensitivity = sensitivity,
            mode = NotificationPrivacyMode.FULL
        )

        assertEquals(NotificationSensitivity.OTP, sensitivity)
        assertEquals("OTP or verification code hidden.", sanitized.second)
        assertFalse(sanitized.second.contains("482911"))
    }

    @Test
    fun `sensitive financial numbers are redacted`() {
        val sensitivity = NotificationContentGuard.classify(
            title = "Bank alert",
            text = "Account 12345678 transaction completed",
            secretVisibility = false
        )
        val sanitized = NotificationContentGuard.sanitize(
            title = "Bank alert",
            text = "Account 12345678 transaction completed",
            sensitivity = sensitivity,
            mode = NotificationPrivacyMode.FULL
        )

        assertEquals(NotificationSensitivity.SENSITIVE, sensitivity)
        assertTrue(sanitized.second.contains("••••"))
        assertFalse(sanitized.second.contains("12345678"))
    }

    @Test
    fun `redact mode hides even normal content`() {
        val sanitized = NotificationContentGuard.sanitize(
            title = "Family",
            text = "Dinner at eight",
            sensitivity = NotificationSensitivity.NORMAL,
            mode = NotificationPrivacyMode.REDACT_CONTENT
        )

        assertEquals("Content hidden by notification privacy settings.", sanitized.second)
    }

    @Test
    fun `sensitive reply preview hides draft content`() {
        val preview = NotificationContentGuard.safeReplyPreview(
            "My account number is 12345678",
            NotificationSensitivity.SENSITIVE
        )

        assertEquals("Sensitive reply content hidden.", preview)
        assertFalse(preview.contains("12345678"))
    }

    @Test
    fun `conversation notifications are grouped in brief`() {
        val store = MayraNotificationStore()
        store.upsert(record("one", "WhatsApp", "Family", "First", 100L, reply = true))
        store.upsert(record("two", "WhatsApp", "Family", "Second", 200L, reply = true))
        store.upsert(record("three", "Mail", null, "Invoice", 150L))

        val brief = store.summary()

        assertEquals(3, brief.total)
        assertEquals(2, brief.replyableCount)
        assertEquals(2, brief.lines.size)
        assertTrue(brief.lines.first().contains("Family: 2 notifications"))
    }

    @Test
    fun `upsert replaces same notification id`() {
        val store = MayraNotificationStore()
        store.upsert(record("same", "Chat", null, "Old", 100L))
        store.upsert(record("same", "Chat", null, "New", 200L))

        assertEquals(1, store.snapshot().size)
        assertEquals("New", store.snapshot().single().text)
    }

    @Test
    fun `store remains bounded`() {
        val store = MayraNotificationStore(maxEntries = 20)
        repeat(25) { index -> store.upsert(record("id-$index", "App", null, "Text $index", index.toLong())) }

        assertEquals(20, store.snapshot().size)
        assertEquals("id-24", store.snapshot().first().id)
    }

    @Test
    fun `spoken brief discloses protected count without protected content`() {
        val brief = NotificationBrief(
            total = 2,
            sensitiveCount = 1,
            replyableCount = 0,
            lines = listOf("Bank: OTP or verification code hidden.")
        )

        val spoken = brief.spokenText()

        assertTrue(spoken.contains("1 sensitive notifications were protected"))
        assertFalse(spoken.contains("482911"))
    }

    @Test
    fun `blank reply fails before handle lookup`() {
        val result = MayraNotificationReplyRuntime.prepare(
            notificationId = "missing",
            replyText = "   ",
            policy = NotificationAppPolicy("chat.app")
        )

        assertTrue(result is NotificationReplyResult.Failed)
        assertEquals("Reply cannot be empty.", (result as NotificationReplyResult.Failed).message)
    }

    @Test
    fun `disabled app reply is blocked`() {
        val result = MayraNotificationReplyRuntime.prepare(
            notificationId = "missing",
            replyText = "Hello",
            policy = NotificationAppPolicy("chat.app", allowReply = false)
        )

        assertTrue(result is NotificationReplyResult.Blocked)
        assertEquals("Replies are disabled for this app.", (result as NotificationReplyResult.Blocked).message)
    }

    @Test
    fun `ignored app reply is blocked`() {
        val result = MayraNotificationReplyRuntime.prepare(
            notificationId = "missing",
            replyText = "Hello",
            policy = NotificationAppPolicy("chat.app", mode = NotificationPrivacyMode.IGNORE)
        )

        assertTrue(result is NotificationReplyResult.Blocked)
        assertEquals("This app is ignored by notification privacy settings.", (result as NotificationReplyResult.Blocked).message)
    }

    @Test
    fun `otp notification reply is always blocked`() {
        val result = MayraNotificationReplyRuntime.prepare(
            notificationId = "missing",
            replyText = "Use this code",
            policy = NotificationAppPolicy("bank.app"),
            sensitivity = NotificationSensitivity.OTP
        )

        assertTrue(result is NotificationReplyResult.Blocked)
        assertEquals("Replies to OTP notifications are blocked for safety.", (result as NotificationReplyResult.Blocked).message)
    }

    private fun record(
        id: String,
        app: String,
        conversation: String?,
        text: String,
        time: Long,
        reply: Boolean = false
    ) = MayraNotificationRecord(
        id = id,
        sourcePackage = app.lowercase(),
        appLabel = app,
        title = app,
        text = text,
        postedAt = time,
        conversationKey = conversation,
        replyAvailable = reply
    )
}