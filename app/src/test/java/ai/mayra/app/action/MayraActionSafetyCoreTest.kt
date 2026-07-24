package ai.mayra.app.action

import ai.mayra.app.core.actions.DeviceActionCoordinator
import ai.mayra.app.core.actions.DeviceActionRequest
import ai.mayra.app.core.actions.DeviceActionRunner
import ai.mayra.app.core.actions.DeviceActionSafetyGate
import ai.mayra.app.core.actions.DeviceActionType
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.core.actions.PermissionSnapshot
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MayraActionSafetyCoreTest {
    @Test
    fun `unavailable capability blocks before runner and offers fallback`() = runTest {
        var executed = false
        val request = request(DeviceActionType.SEND_MESSAGE, "Rahul", "Hello")
        val engine = engine(
            runner = { executed = true; "unexpected" },
            registry = MayraCapabilityRegistry(
                listOf(
                    MayraCapability(
                        DeviceActionType.SEND_MESSAGE,
                        MayraCapabilityState.USER_SETUP_REQUIRED,
                        "Messaging app is not configured.",
                        "Open a messaging app and select a recipient manually."
                    )
                )
            )
        )

        val result = engine.submit(request, PermissionSnapshot(granted = request.requiredPermissions))

        val blocked = assertIs<MayraActionResult.Blocked>(result)
        assertFalse(executed)
        assertEquals(MayraCapabilityState.USER_SETUP_REQUIRED, blocked.capability.state)
        assertTrue(blocked.fallback.instruction.contains("messaging app"))
    }

    @Test
    fun `missing permission returns permission state without execution`() = runTest {
        var executed = false
        val request = request(DeviceActionType.CALL_CONTACT, "Mummy")
        val engine = engine(runner = { executed = true; null })

        val result = engine.submit(request, PermissionSnapshot())

        val waiting = assertIs<MayraActionResult.AwaitingPermission>(result)
        assertFalse(executed)
        assertEquals(
            setOf(DevicePermission.READ_CONTACTS, DevicePermission.CALL_PHONE),
            waiting.missing
        )
    }

    @Test
    fun `high risk action waits for one time confirmation and then executes`() = runTest {
        var executions = 0
        val request = request(DeviceActionType.CALL_CONTACT, "Mummy")
        val engine = engine(runner = { executions++; "dialer opened" })

        val waiting = engine.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as MayraActionResult.AwaitingConfirmation

        assertEquals(MayraActionRisk.HIGH, waiting.risk.level)
        assertTrue(waiting.risk.confirmationRequired)
        assertEquals(0, executions)

        val completed = assertIs<MayraActionResult.Completed>(engine.confirm(waiting.ticket.token))
        assertEquals(1, executions)
        assertEquals(MayraVerificationStatus.USER_VISIBLE_HANDOFF, completed.verification.status)
    }

    @Test
    fun `message verification never claims that compose handoff was sent`() = runTest {
        val request = request(DeviceActionType.SEND_MESSAGE, "Rahul", "I am late")
        val engine = engine(runner = { "Opened message for Rahul." })
        val waiting = engine.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as MayraActionResult.AwaitingConfirmation

        val completed = engine.confirm(waiting.ticket.token) as MayraActionResult.Completed

        assertEquals(MayraVerificationStatus.USER_VISIBLE_HANDOFF, completed.verification.status)
        assertTrue(completed.verification.message.contains("not claimed as sent"))
    }

    @Test
    fun `critical metadata produces double confirmation recommendation`() {
        val decision = MayraRiskClassifier().classify(
            request(DeviceActionType.SEND_MESSAGE, "Bank", "Approve").copy(
                metadata = mapOf("financial" to "true")
            )
        )

        assertEquals(MayraActionRisk.CRITICAL, decision.level)
        assertTrue(decision.confirmationRequired)
        assertTrue(decision.strongAuthenticationRecommended)
        assertTrue(decision.doubleConfirmationRequired)
    }

    @Test
    fun `runner failure is masked and produces action specific fallback`() = runTest {
        val request = request(DeviceActionType.CREATE_REMINDER, "Medicine")
        val engine = engine(runner = { error("private implementation detail") })

        val failed = engine.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as MayraActionResult.Failed

        assertEquals("The device action could not be completed.", failed.message)
        assertFalse(failed.message.contains("private"))
        assertTrue(failed.fallback.instruction.contains("Calendar or Clock"))
    }

    @Test
    fun `kill switch blocks every new action until resumed`() = runTest {
        var executions = 0
        val request = request(DeviceActionType.OPEN_APP, "Maps")
        val engine = engine(runner = { executions++; "opened" })
        val permissions = PermissionSnapshot(granted = request.requiredPermissions)

        engine.stopAll()
        assertIs<MayraActionResult.Blocked>(engine.submit(request, permissions))
        assertEquals(0, executions)
        assertTrue(engine.isStopped())

        engine.resume()
        assertIs<MayraActionResult.Completed>(engine.submit(request, permissions))
        assertEquals(1, executions)
    }

    @Test
    fun `audit log is bounded and records request through verification`() = runTest {
        val audit = MayraActionAuditLog(maxEntries = 20, clock = { 100L })
        val engine = engine(runner = { "opened" }, audit = audit)
        val permissions = PermissionSnapshot(granted = setOf(DevicePermission.QUERY_APPS))

        repeat(12) { index ->
            engine.submit(request(DeviceActionType.OPEN_APP, "App $index"), permissions)
        }

        val entries = engine.auditSnapshot()
        assertEquals(20, entries.size)
        assertTrue(entries.any { it.status == MayraActionEventStatus.EXECUTED })
        assertTrue(entries.any { it.status == MayraActionEventStatus.VERIFIED })
        assertEquals(20, engine.clearAudit())
        assertTrue(engine.auditSnapshot().isEmpty())
    }

    private fun engine(
        runner: suspend (DeviceActionRequest) -> String?,
        registry: MayraCapabilityRegistry = MayraCapabilityRegistry(),
        audit: MayraActionAuditLog = MayraActionAuditLog()
    ): MayraActionEngine {
        val coordinator = DeviceActionCoordinator(
            safetyGate = DeviceActionSafetyGate(
                clock = { 100L },
                tokenFactory = { "token-${System.nanoTime()}" }
            ),
            runner = DeviceActionRunner(runner)
        )
        return MayraActionEngine(
            coordinator = coordinator,
            capabilityRegistry = registry,
            auditLog = audit
        )
    }

    private fun request(
        type: DeviceActionType,
        target: String,
        payload: String? = null
    ) = DeviceActionRequest(
        id = "${type.name}-$target",
        type = type,
        target = target,
        payload = payload,
        createdAt = 100L
    )
}
