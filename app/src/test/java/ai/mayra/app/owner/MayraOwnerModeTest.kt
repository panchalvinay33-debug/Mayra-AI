package ai.mayra.app.owner

import ai.mayra.app.TestMayraApplication
import ai.mayra.app.action.MayraActionRisk
import ai.mayra.app.core.actions.DeviceActionRequest
import ai.mayra.app.core.actions.DeviceActionType
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class MayraOwnerModeTest {
    @Test
    fun `owner preferences persist`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MayraOwnerModeStore(context)
        val expected = MayraOwnerPreferences(
            enabled = true,
            directLowRiskActions = true,
            directMediumRiskActions = true,
            trustedDirectHandoffs = true,
            proactivePresence = false,
            keepBackgroundRuntime = true
        )

        store.save(expected)

        assertEquals(expected, store.read())
    }

    @Test
    fun `trusted owner may auto confirm ordinary high risk handoff`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MayraOwnerModeStore(context).save(
            MayraOwnerPreferences(enabled = true, trustedDirectHandoffs = true)
        )
        val policy = StoredMayraOwnerActionPolicy(context)

        assertTrue(policy.mayAutoConfirm(request(DeviceActionType.CALL_CONTACT), MayraActionRisk.HIGH))
        assertTrue(policy.mayAutoConfirm(request(DeviceActionType.SEND_MESSAGE), MayraActionRisk.HIGH))
    }

    @Test
    fun `trusted owner never bypasses sensitive destructive financial or critical action`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MayraOwnerModeStore(context).save(
            MayraOwnerPreferences(enabled = true, trustedDirectHandoffs = true)
        )
        val policy = StoredMayraOwnerActionPolicy(context)

        assertFalse(policy.mayAutoConfirm(request(DeviceActionType.SEND_MESSAGE, mapOf("sensitive" to "true")), MayraActionRisk.HIGH))
        assertFalse(policy.mayAutoConfirm(request(DeviceActionType.SEND_MESSAGE, mapOf("destructive" to "true")), MayraActionRisk.HIGH))
        assertFalse(policy.mayAutoConfirm(request(DeviceActionType.SEND_MESSAGE, mapOf("financial" to "true")), MayraActionRisk.CRITICAL))
        assertFalse(policy.mayAutoConfirm(request(DeviceActionType.SEND_MESSAGE), MayraActionRisk.CRITICAL))
    }

    @Test
    fun `owner mode off preserves confirmations`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        MayraOwnerModeStore(context).save(
            MayraOwnerPreferences(enabled = false, trustedDirectHandoffs = true)
        )

        assertFalse(
            StoredMayraOwnerActionPolicy(context).mayAutoConfirm(
                request(DeviceActionType.CALL_CONTACT),
                MayraActionRisk.HIGH
            )
        )
    }

    @Test
    fun `readiness score is deterministic`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val inspector = MayraOwnerCapabilityInspector(context)
        val statuses = listOf(
            OwnerCapabilityStatus(OwnerCapability.MICROPHONE, OwnerAccessState.READY, "Mic", "Ready"),
            OwnerCapabilityStatus(OwnerCapability.CONTACTS, OwnerAccessState.ACTION_REQUIRED, "Contacts", "Missing"),
            OwnerCapabilityStatus(OwnerCapability.CAMERA, OwnerAccessState.READY, "Camera", "Ready"),
            OwnerCapabilityStatus(OwnerCapability.SMS, OwnerAccessState.DEVICE_UNSUPPORTED, "SMS", "No telephony")
        )

        assertEquals(50, inspector.readinessScore(statuses))
    }

    private fun request(
        type: DeviceActionType,
        metadata: Map<String, String> = emptyMap()
    ) = DeviceActionRequest(
        type = type,
        target = "target",
        createdAt = 1L,
        metadata = metadata
    )
}
