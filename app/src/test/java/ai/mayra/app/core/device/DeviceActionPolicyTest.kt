package ai.mayra.app.core.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeviceActionPolicyTest {
    @Test
    fun `notification requires permission before confirmation`() {
        val policy = DeviceActionPolicy()
        val action = DeviceAction.ShowNotification("Hello", "World")

        val decision = policy.evaluate(action, DeviceActionContext("s1"))

        assertFalse(decision.allowed)
        assertEquals(DeviceActionStatus.PERMISSION_DENIED, decision.status)
    }

    @Test
    fun `confirmed notification with permission is allowed`() {
        val policy = DeviceActionPolicy()
        val context = DeviceActionContext(
            sessionId = "s1",
            grantedPermissions = setOf("android.permission.POST_NOTIFICATIONS"),
            confirmed = true
        )

        val decision = policy.evaluate(DeviceAction.ShowNotification("Hello", "World"), context)

        assertTrue(decision.allowed)
        assertEquals(DeviceActionStatus.SUCCESS, decision.status)
    }

    @Test
    fun `safe low impact action does not require confirmation`() {
        val decision = DeviceActionPolicy().evaluate(
            DeviceAction.CopyToClipboard("hello"),
            DeviceActionContext("s1")
        )

        assertTrue(decision.allowed)
    }
}
