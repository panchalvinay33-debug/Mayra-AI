package ai.mayra.app.core.actions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DeviceActionSafetyGateReliabilityTest {
    @Test
    fun `identical high risk action cannot create two pending confirmations`() {
        var now = 1_000L
        var token = 0
        val gate = DeviceActionSafetyGate(clock = { now }, tokenFactory = { "token-${++token}" })
        val request = request(DeviceActionType.SEND_MESSAGE, "Mummy", "Reached safely", now)
        val permissions = PermissionSnapshot(granted = request.requiredPermissions)

        val first = assertIs<ActionGateDecision.NeedsConfirmation>(gate.evaluate(request, permissions))
        val duplicate = assertIs<ActionGateDecision.Rejected>(
            gate.evaluate(request.copy(id = "second"), permissions)
        )

        assertEquals("token-1", first.ticket.token)
        assertTrue(duplicate.reason.contains("already waiting"))
        assertEquals(1, gate.snapshot().pendingConfirmations)
    }

    @Test
    fun `confirmation token is one time`() {
        val gate = DeviceActionSafetyGate(clock = { 1_000L }, tokenFactory = { "one-time" })
        val request = request(DeviceActionType.CALL_CONTACT, "Mummy", null, 1_000L)
        val waiting = assertIs<ActionGateDecision.NeedsConfirmation>(
            gate.evaluate(request, PermissionSnapshot(granted = request.requiredPermissions))
        )

        assertIs<ActionGateDecision.Ready>(gate.confirm(waiting.ticket.token))
        val repeated = assertIs<ActionGateDecision.Rejected>(gate.confirm(waiting.ticket.token))

        assertEquals("Confirmation is invalid or expired.", repeated.reason)
    }

    @Test
    fun `recently executed duplicate is blocked`() {
        var now = 1_000L
        val gate = DeviceActionSafetyGate(
            duplicateWindowMillis = 30_000L,
            clock = { now },
            tokenFactory = { "token" }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "Mummy", null, now)
        val permissions = PermissionSnapshot(granted = request.requiredPermissions)
        val waiting = assertIs<ActionGateDecision.NeedsConfirmation>(gate.evaluate(request, permissions))
        val ready = assertIs<ActionGateDecision.Ready>(gate.confirm(waiting.ticket.token))
        gate.recordExecuted(ready.request, "dialer opened")

        now += 5_000L
        val duplicate = assertIs<ActionGateDecision.Rejected>(
            gate.evaluate(request.copy(id = "repeat", createdAt = now), permissions)
        )

        assertTrue(duplicate.reason.contains("already handed"))
        assertTrue(gate.snapshot().auditEntries.any { it.status == ActionAuditStatus.DUPLICATE_BLOCKED })
    }

    @Test
    fun `same action can be requested after duplicate window`() {
        var now = 1_000L
        var token = 0
        val gate = DeviceActionSafetyGate(
            duplicateWindowMillis = 30_000L,
            clock = { now },
            tokenFactory = { "token-${++token}" }
        )
        val request = request(DeviceActionType.CALL_CONTACT, "Mummy", null, now)
        val permissions = PermissionSnapshot(granted = request.requiredPermissions)
        val first = assertIs<ActionGateDecision.NeedsConfirmation>(gate.evaluate(request, permissions))
        val ready = assertIs<ActionGateDecision.Ready>(gate.confirm(first.ticket.token))
        gate.recordExecuted(ready.request)

        now += 31_000L
        assertIs<ActionGateDecision.NeedsConfirmation>(
            gate.evaluate(request.copy(id = "later", createdAt = now), permissions)
        )
    }

    @Test
    fun `stale action is rejected before permission or confirmation`() {
        val now = 20 * 60 * 1_000L
        val gate = DeviceActionSafetyGate(clock = { now })
        val request = request(DeviceActionType.SEND_MESSAGE, "Rahul", "Hello", 1L)

        val rejected = assertIs<ActionGateDecision.Rejected>(
            gate.evaluate(request, PermissionSnapshot())
        )

        assertTrue(rejected.reason.contains("stale"))
        assertEquals(0, gate.snapshot().pendingConfirmations)
    }

    @Test
    fun `future clock skew is rejected`() {
        val gate = DeviceActionSafetyGate(clock = { 1_000L })
        val request = request(DeviceActionType.CALL_CONTACT, "Mummy", null, 120_000L)

        val rejected = assertIs<ActionGateDecision.Rejected>(
            gate.evaluate(request, PermissionSnapshot(granted = request.requiredPermissions))
        )

        assertTrue(rejected.reason.contains("time is invalid"))
    }

    @Test
    fun `fingerprint normalizes whitespace and volatile metadata`() {
        val first = request(DeviceActionType.SEND_MESSAGE, " Mummy ", "Reached   safely", 1_000L)
            .copy(metadata = mapOf("traceId" to "one", "channel" to "sms"))
        val second = request(DeviceActionType.SEND_MESSAGE, "mummy", " reached safely ", 2_000L)
            .copy(metadata = mapOf("traceId" to "two", "channel" to "SMS"))

        assertEquals(first.safetyFingerprint(), second.safetyFingerprint())
    }

    @Test
    fun `different message bodies have different fingerprints`() {
        val first = request(DeviceActionType.SEND_MESSAGE, "Mummy", "Reached safely", 1_000L)
        val second = request(DeviceActionType.SEND_MESSAGE, "Mummy", "Running late", 1_000L)

        assertFalse(first.safetyFingerprint() == second.safetyFingerprint())
    }

    @Test
    fun `audit is bounded and redacts keys`() {
        var now = 1_000L
        val gate = DeviceActionSafetyGate(maxAuditEntries = 20, clock = { now })
        repeat(30) { index ->
            val request = request(DeviceActionType.OPEN_APP, "App $index", null, now)
            val ready = assertIs<ActionGateDecision.Ready>(
                gate.evaluate(request, PermissionSnapshot(granted = request.requiredPermissions))
            )
            gate.recordFailed(ready.request, "Bearer sk-secret123456789 failed\nprivately")
            now++
        }

        val audit = gate.snapshot().auditEntries
        assertEquals(20, audit.size)
        assertTrue(audit.none { it.detail.orEmpty().contains("sk-secret") })
        assertTrue(audit.none { it.detail.orEmpty().contains('\n') })
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String?,
        createdAt: Long
    ) = DeviceActionRequest(
        id = "${type.name}-$target-$createdAt",
        type = type,
        target = target,
        payload = payload,
        createdAt = createdAt
    )
}
