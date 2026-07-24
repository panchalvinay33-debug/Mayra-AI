package ai.mayra.app.core

import ai.mayra.app.background.MayraNotificationIntelligenceRuntime
import ai.mayra.app.background.MayraNotificationRecord
import ai.mayra.app.background.NotificationSensitivity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCommandRoutingTest {
    @After
    fun clearStore() {
        MayraNotificationIntelligenceRuntime.store.clear()
    }

    @Test
    fun `English notification brief command stays structured and local`() {
        val intent = AssistantIntentEngine().parse("Mayra, read notifications")

        assertEquals(
            AssistantIntent.DeviceInfo(DeviceInfoType.NOTIFICATIONS),
            intent
        )
    }

    @Test
    fun `Hinglish notification brief command stays structured and local`() {
        val intent = AssistantIntentEngine().parse("Mayra notifications padhkar batao")

        assertEquals(
            AssistantIntent.DeviceInfo(DeviceInfoType.NOTIFICATIONS),
            intent
        )
    }

    @Test
    fun `local brief groups notifications without exposing OTP`() = runTest {
        MayraNotificationIntelligenceRuntime.store.upsert(
            MayraNotificationRecord(
                id = "otp",
                sourcePackage = "bank.app",
                appLabel = "Bank",
                title = "Verification",
                text = "OTP or verification code hidden.",
                postedAt = 200L,
                sensitivity = NotificationSensitivity.OTP
            )
        )
        MayraNotificationIntelligenceRuntime.store.upsert(
            MayraNotificationRecord(
                id = "chat",
                sourcePackage = "chat.app",
                appLabel = "Chat",
                title = "Family",
                text = "Dinner at eight",
                postedAt = 100L,
                conversationKey = "Family",
                replyAvailable = true
            )
        )

        val response = LocalCommandEngine().respond("notification summary")

        assertTrue(response.contains("2 notifications"))
        assertTrue(response.contains("protected"))
        assertTrue(response.contains("OTP or verification code hidden"))
        assertFalse(response.contains("482911"))
    }

    @Test
    fun `empty store gives calm private response`() = runTest {
        val response = LocalCommandEngine().respond("unread notifications")

        assertEquals("There are no captured notifications.", response)
    }
}
