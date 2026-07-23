package ai.mayra.app.core.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceActionSafetyTest {
    @Test
    fun `open app is ready when query permission is granted`() {
        val gate = DeviceActionSafetyGate(clock = { 100L })
        val request = request(DeviceActionType.OPEN_APP, "Maps")

        val decision = gate.evaluate(
            request,
            PermissionSnapshot(granted = setOf(DevicePermission.QUERY_APPS))
        )

        assertTrue(decision is ActionGateDecision.Ready)
        assertEquals(ActionRiskLevel.LOW, request.riskLevel)
        assertFalse(request.requiresConfirmation)
    }

    @Test
    fun `missing permissions are returned before confirmation`() {
        val gate = DeviceActionSafetyGate(clock = { 100L })
        val request = request(DeviceActionType.CALL_CONTACT, "Amit")

        val decision = gate.evaluate(request, PermissionSnapshot()) as ActionGateDecision.NeedsPermission

        assertEquals(
            setOf(DevicePermission.READ_CONTACTS, DevicePermission.CALL_PHONE),
            decision.missing
        )
        assertTrue(gate.snapshot().pendingConfirmations == 0)
    }

    @Test
    fun `permanently denied permissions are identified separately`() {
        val gate = DeviceActionSafetyGate(clock = { 100L })
        val request = request(DeviceActionType.SEND_MESSAGE, "Mayra", "Hello")
        val permissions = PermissionSnapshot(
            granted = setOf(DevicePermission.READ_CONTACTS),
            permanentlyDenied = setOf(DevicePermission.SEND_MESSAGES)
        )

        val decision = gate.evaluate(request, permissions) as ActionGateDecision.NeedsPermission

        assertEquals(setOf(DevicePermission.SEND_MESSAGES), decision.missing)
        assertEquals(setOf(DevicePermission.SEND_MESSAGES), decision.permanentlyDenied)
    }

    @Test
    fun `high risk call receives one time confirmation ticket`() {
        var now = 100L
        val gate = DeviceActionSafetyGate(
            confirmationTtlMillis = 1_000L,
            clock = { now },
            tokenFactory = { "token-1" }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "Amit")
        val permissions = PermissionSnapshot(granted = request.requiredPermissions)

        val pending = gate.evaluate(request, permissions) as ActionGateDecision.NeedsConfirmation
        val ready = gate.confirm(pending.ticket.token)
        val reused = gate.confirm(pending.ticket.token)

        assertEquals("token-1", pending.ticket.token)
        assertEquals("Confirm call to Amit.", pending.prompt)
        assertTrue(ready is ActionGateDecision.Ready)
        assertTrue(reused is ActionGateDecision.Rejected)
        assertEquals(0, gate.snapshot().pendingConfirmations)
    }

    @Test
    fun `expired ticket is rejected and audited`() {
        var now = 100L
        val gate = DeviceActionSafetyGate(
            confirmationTtlMillis = 50L,
            clock = { now },
            tokenFactory = { "expires" }
        )
        val request = request(DeviceActionType.SEND_MESSAGE, "Mayra", "Hello")
        val pending = gate.evaluate(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as ActionGateDecision.NeedsConfirmation

        now = 150L
        val result = gate.confirm(pending.ticket.token)

        assertTrue(result is ActionGateDecision.Rejected)
        assertTrue(
            gate.snapshot().auditEntries.any {
                it.requestId == request.id && it.status == ActionAuditStatus.EXPIRED
            }
        )
    }

    @Test
    fun `user rejection removes pending ticket and records reason`() {
        val gate = DeviceActionSafetyGate(
            clock = { 100L },
            tokenFactory = { "cancel" }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "Amit")
        val pending = gate.evaluate(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as ActionGateDecision.NeedsConfirmation

        val rejected = gate.reject(pending.ticket.token, "Not now")

        assertEquals("Not now", rejected.reason)
        assertEquals(0, gate.snapshot().pendingConfirmations)
        assertEquals(
            "Not now",
            gate.snapshot().auditEntries.last { it.status == ActionAuditStatus.REJECTED }.detail
        )
    }

    @Test
    fun `reminder is medium risk and requires notification permission only`() {
        val request = request(DeviceActionType.CREATE_REMINDER, "Buy milk")

        assertEquals(ActionRiskLevel.MEDIUM, request.riskLevel)
        assertFalse(request.requiresConfirmation)
        assertEquals(setOf(DevicePermission.POST_NOTIFICATIONS), request.requiredPermissions)
    }

    @Test
    fun `audit can be cleared without affecting pending confirmations`() {
        val gate = DeviceActionSafetyGate(
            clock = { 100L },
            tokenFactory = { "pending" }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "Amit")
        gate.evaluate(request, PermissionSnapshot(granted = request.requiredPermissions))

        val cleared = gate.clearAudit()

        assertTrue(cleared > 0)
        assertEquals(1, gate.snapshot().pendingConfirmations)
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String? = null
    ) = DeviceActionRequest(
        id = "request-${type.name}",
        type = type,
        target = target,
        payload = payload,
        createdAt = 100L
    )
}
