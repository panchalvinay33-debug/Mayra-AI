package ai.mayra.app.core.actions

sealed interface DeviceActionExecutionResult {
    data class Completed(
        val request: DeviceActionRequest,
        val output: String? = null
    ) : DeviceActionExecutionResult

    data class AwaitingPermission(
        val request: DeviceActionRequest,
        val missing: Set<DevicePermission>,
        val permanentlyDenied: Set<DevicePermission>
    ) : DeviceActionExecutionResult

    data class AwaitingConfirmation(
        val request: DeviceActionRequest,
        val ticket: ConfirmationTicket,
        val prompt: String
    ) : DeviceActionExecutionResult

    data class Rejected(
        val request: DeviceActionRequest,
        val reason: String
    ) : DeviceActionExecutionResult

    data class Failed(
        val request: DeviceActionRequest,
        val message: String
    ) : DeviceActionExecutionResult
}

fun interface DeviceActionRunner {
    suspend fun run(request: DeviceActionRequest): String?
}

class DeviceActionCoordinator(
    private val safetyGate: DeviceActionSafetyGate,
    private val runner: DeviceActionRunner
) {
    suspend fun submit(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot
    ): DeviceActionExecutionResult = when (val decision = safetyGate.evaluate(request, permissions)) {
        is ActionGateDecision.Ready -> execute(decision.request)
        is ActionGateDecision.NeedsPermission -> DeviceActionExecutionResult.AwaitingPermission(
            request = decision.request,
            missing = decision.missing,
            permanentlyDenied = decision.permanentlyDenied
        )
        is ActionGateDecision.NeedsConfirmation -> DeviceActionExecutionResult.AwaitingConfirmation(
            request = decision.request,
            ticket = decision.ticket,
            prompt = decision.prompt
        )
        is ActionGateDecision.Rejected -> DeviceActionExecutionResult.Rejected(
            decision.request,
            decision.reason
        )
    }

    suspend fun confirm(token: String): DeviceActionExecutionResult = when (val decision = safetyGate.confirm(token)) {
        is ActionGateDecision.Ready -> execute(decision.request)
        is ActionGateDecision.NeedsPermission -> DeviceActionExecutionResult.AwaitingPermission(
            decision.request,
            decision.missing,
            decision.permanentlyDenied
        )
        is ActionGateDecision.NeedsConfirmation -> DeviceActionExecutionResult.AwaitingConfirmation(
            decision.request,
            decision.ticket,
            decision.prompt
        )
        is ActionGateDecision.Rejected -> DeviceActionExecutionResult.Rejected(
            decision.request,
            decision.reason
        )
    }

    fun reject(token: String, reason: String = "User cancelled the action."): DeviceActionExecutionResult {
        return when (val decision = safetyGate.reject(token, reason)) {
            is ActionGateDecision.Rejected -> DeviceActionExecutionResult.Rejected(
                decision.request,
                decision.reason
            )
            else -> error("Reject must always produce a rejected decision.")
        }
    }

    fun snapshot(): ActionSafetySnapshot = safetyGate.snapshot()

    private suspend fun execute(request: DeviceActionRequest): DeviceActionExecutionResult = try {
        val output = runner.run(request)
        safetyGate.recordExecuted(request, output)
        DeviceActionExecutionResult.Completed(request, output)
    } catch (error: SecurityException) {
        val message = "Required permission was denied."
        safetyGate.recordFailed(request, message)
        DeviceActionExecutionResult.Failed(request, message)
    } catch (error: IllegalArgumentException) {
        val message = "The action request was invalid."
        safetyGate.recordFailed(request, message)
        DeviceActionExecutionResult.Failed(request, message)
    } catch (error: Throwable) {
        val message = "The device action could not be completed."
        safetyGate.recordFailed(request, message)
        DeviceActionExecutionResult.Failed(request, message)
    }
}
