package ai.mayra.app.core.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidDeviceActionSpecTest {
    @Test
    fun `open app uses package metadata when available`() {
        val request = request(
            type = DeviceActionType.OPEN_APP,
            target = "YouTube",
            metadata = mapOf("packageName" to "com.google.android.youtube")
        )

        val spec = AndroidDeviceActionSpecFactory.create(request)

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_MAIN, spec.action)
        assertEquals("com.google.android.youtube", spec.packageName)
    }

    @Test
    fun `call spec uses tel uri`() {
        val spec = AndroidDeviceActionSpecFactory.create(
            request(DeviceActionType.CALL_CONTACT, "+91 98765 43210")
        )

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_CALL, spec.action)
        assertEquals("tel:+91 98765 43210", spec.data)
    }

    @Test
    fun `message spec includes body only when non blank`() {
        val spec = AndroidDeviceActionSpecFactory.create(
            request(DeviceActionType.SEND_MESSAGE, "9876543210", "  Hello Mayra  ")
        )

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_SENDTO, spec.action)
        assertEquals("smsto:9876543210", spec.data)
        assertEquals("Hello Mayra", spec.extras[AndroidDeviceActionSpecFactory.EXTRA_TEXT])
    }

    @Test
    fun `reminder spec contains title and optional detail`() {
        val spec = AndroidDeviceActionSpecFactory.create(
            request(DeviceActionType.CREATE_REMINDER, "Medicine", "At 8 PM")
        )

        assertEquals(AndroidDeviceActionSpecFactory.ACTION_INSERT, spec.action)
        assertEquals("Medicine", spec.extras[AndroidDeviceActionSpecFactory.EXTRA_TITLE])
        assertEquals("At 8 PM", spec.extras[AndroidDeviceActionSpecFactory.EXTRA_TEXT])
    }

    @Test
    fun `permission bridge maps runtime permissions`() {
        assertEquals(
            "android.permission.CALL_PHONE",
            AndroidDeviceActionSpecFactory.androidPermissionName(DevicePermission.CALL_PHONE)
        )
        assertEquals(
            "android.permission.SEND_SMS",
            AndroidDeviceActionSpecFactory.androidPermissionName(DevicePermission.SEND_MESSAGES)
        )
        assertNull(AndroidDeviceActionSpecFactory.androidPermissionName(DevicePermission.QUERY_APPS))
    }

    @Test
    fun `blank message body produces no extras`() {
        val spec = AndroidDeviceActionSpecFactory.create(
            request(DeviceActionType.SEND_MESSAGE, "9876543210", "   ")
        )

        assertTrue(spec.extras.isEmpty())
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) = DeviceActionRequest(
        id = "request-${type.name}",
        type = type,
        target = target,
        payload = payload,
        createdAt = 100L,
        metadata = metadata
    )
}
