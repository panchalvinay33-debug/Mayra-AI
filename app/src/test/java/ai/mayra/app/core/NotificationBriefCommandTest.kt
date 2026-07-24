package ai.mayra.app.core

import ai.mayra.app.background.MayraNotificationIntelligenceRuntime
import ai.mayra.app.background.MayraNotificationRecord
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationBriefCommandTest {
    @Before
    fun clearStore() {
        MayraNotificationIntelligenceRuntime.store.clear()
    }

    @Test
    fun `hinglish notification command parses as device info`() {
        val intent = AssistantIntentEngine().parse("Mayra notifications padhkar batao")

        assertEquals(
            AssistantIntent.DeviceInfo(DeviceInfoType.NOTIFICATIONS),
            intent
        )
    }

    @Test
    fun `english notification brief command parses as device info`() {
        val intent = AssistantIntentEngine().parse("read notifications")

        assertEquals(
            AssistantIntent.DeviceInfo(DeviceInfoType.NOTIFICATIONS),
            intent
        )
    }

    @Test
    fun `empty notification store produces safe spoken response`() = runTest {
        val response = LocalCommandEngine().respond("notification summary")

        assertEquals("There are no captured notifications.", response)
    }

    @Test
    fun `notification brief response uses protected stored content`() = runTest {
        MayraNotificationIntelligenceRuntime.store.upsert(
            MayraNotificationRecord(
                id = "otp",
                sourcePackage = "bank.app",
                appLabel = "Bank",
                title = "Verification",
                text = "OTP or verification code hidden.",
                postedAt = 100L,
                sensitivity = ai.mayra.app.background.NotificationSensitivity.OTP
            )
        )

        val response = LocalCommandEngine().respond("notifications batao")

        assertTrue(response.contains("OTP or verification code hidden"))
        assertTrue(response.contains("sensitive notifications were protected"))
    }
}
