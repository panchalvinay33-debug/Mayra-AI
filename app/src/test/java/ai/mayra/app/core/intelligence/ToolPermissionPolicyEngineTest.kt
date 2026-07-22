package ai.mayra.app.core.intelligence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolPermissionPolicyEngineTest {
    @Test
    fun `missing permission denies execution`() {
        val manifest = ToolManifest(
            id = "device.notification",
            displayName = "Notification",
            description = "Reads notifications",
            requiredPermissions = setOf("notification_access")
        )

        val result = ToolPermissionPolicyEngine().evaluate(
            manifest,
            ToolExecutionContext(sessionId = "s1")
        )

        assertEquals(ToolPermissionDecision.DENY, result.decision)
        assertTrue("notification_access" in result.missingPermissions)
    }

    @Test
    fun `high risk requires confirmation after permissions pass`() {
        val manifest = ToolManifest(
            id = "device.settings.write",
            displayName = "Write Settings",
            description = "Changes a device setting",
            requiredPermissions = setOf("write_settings"),
            riskLevel = ToolRiskLevel.HIGH
        )

        val result = ToolPermissionPolicyEngine().evaluate(
            manifest,
            ToolExecutionContext("s1", grantedPermissions = setOf("write_settings"))
        )

        assertEquals(ToolPermissionDecision.REQUIRE_CONFIRMATION, result.decision)
    }

    @Test
    fun `explicit policy denial wins`() {
        val engine = ToolPermissionPolicyEngine(
            ToolPermissionPolicy(deniedToolIds = setOf("device.share"))
        )
        val manifest = ToolManifest("device.share", "Share", "Shares content")

        assertEquals(
            ToolPermissionDecision.DENY,
            engine.evaluate(manifest, ToolExecutionContext("s1")).decision
        )
    }
}
