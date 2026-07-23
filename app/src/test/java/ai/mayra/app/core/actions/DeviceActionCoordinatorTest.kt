package ai.mayra.app.core.actions

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceActionCoordinatorTest {
    @Test
    fun `low risk action executes immediately after permission check`() = runTest {
        val executed = mutableListOf<String>()
        val coordinator = coordinator { request ->
            executed += request.target
            "opened:${request.target}"
        }
        val request = request(DeviceActionType.OPEN_APP, "Maps")

        val result = coordinator.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as DeviceActionExecutionResult.Completed

        assertEquals("opened:Maps", result.output)
        assertEquals(listOf("Maps"), executed)
        assertTrue(coordinator.snapshot().auditEntries.any { it.status == ActionAuditStatus.EXECUTED })
    }

    @Test
    fun `high risk action waits and executes only after confirmation`() = runTest {
        var executions = 0
        val coordinator = coordinator { executions += 1; "called" }
        val request = request(DeviceActionType.CALL_CONTACT, "Amit")

        val waiting = coordinator.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as DeviceActionExecutionResult.AwaitingConfirmation

        assertEquals(0, executions)
        val completed = coordinator.confirm(waiting.ticket.token)
        assertTrue(completed is DeviceActionExecutionResult.Completed)
        assertEquals(1, executions)
    }

    @Test
    fun `missing permission never invokes runner`() = runTest {
        var invoked = false
        val coordinator = coordinator { invoked = true; null }
        val request = request(DeviceActionType.SEND_MESSAGE, "Mayra", "Hello")

        val result = coordinator.submit(request, PermissionSnapshot())

        assertTrue(result is DeviceActionExecutionResult.AwaitingPermission)
        assertFalse(invoked)
    }

    @Test
    fun `rejected confirmation never invokes runner`() = runTest {
        var invoked = false
        val coordinator = coordinator { invoked = true; null }
        val request = request(DeviceActionType.CALL_CONTACT, "Amit")
        val waiting = coordinator.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as DeviceActionExecutionResult.AwaitingConfirmation

        val rejected = coordinator.reject(waiting.ticket.token)

        assertTrue(rejected is DeviceActionExecutionResult.Rejected)
        assertFalse(invoked)
    }

    @Test
    fun `security exception is converted to safe failure`() = runTest {
        val coordinator = coordinator { throw SecurityException("private platform details") }
        val request = request(DeviceActionType.OPEN_APP, "Maps")

        val result = coordinator.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as DeviceActionExecutionResult.Failed

        assertEquals("Required permission was denied.", result.message)
        assertFalse(result.message.contains("private"))
    }

    @Test
    fun `unexpected exception is masked and audited`() = runTest {
        val coordinator = coordinator { error("secret implementation detail") }
        val request = request(DeviceActionType.CREATE_REMINDER, "Buy milk")

        val result = coordinator.submit(
            request,
            PermissionSnapshot(granted = request.requiredPermissions)
        ) as DeviceActionExecutionResult.Failed

        assertEquals("The device action could not be completed.", result.message)
        val failure = coordinator.snapshot().auditEntries.last { it.status == ActionAuditStatus.FAILED }
        assertFalse(failure.detail.orEmpty().contains("secret"))
    }

    @Test
    fun `invalid confirmation token is rejected safely`() = runTest {
        val coordinator = coordinator { "unused" }

        val result = coordinator.confirm("unknown-token")

        assertTrue(result is DeviceActionExecutionResult.Rejected)
        assertEquals(
            "Confirmation is invalid or expired.",
            (result as DeviceActionExecutionResult.Rejected).reason
        )
    }

    private fun coordinator(
        runner: suspend (DeviceActionRequest) -> String?
    ): DeviceActionCoordinator = DeviceActionCoordinator(
        safetyGate = DeviceActionSafetyGate(
            clock = { 100L },
            tokenFactory = { "token" }
        ),
        runner = DeviceActionRunner(runner)
    )

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
