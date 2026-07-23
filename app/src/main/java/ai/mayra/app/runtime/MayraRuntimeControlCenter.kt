package ai.mayra.app.runtime

import ai.mayra.app.background.PendingAction
import ai.mayra.app.background.PendingActionState
import ai.mayra.app.background.PendingActionStore
import ai.mayra.app.background.TrustAuditStore
import ai.mayra.app.brain.MayraPlan
import ai.mayra.app.brain.MayraPlanRuntime
import ai.mayra.app.brain.MayraPlanStore
import ai.mayra.app.brain.MayraRuntimeOrchestrator
import ai.mayra.app.brain.PlanRuntimeDiagnostics
import ai.mayra.app.brain.RuntimeDiagnostics
import android.content.Context

/** Read-only runtime summary intended for diagnostics and a future Compose control-center screen. */
data class RuntimeControlSnapshot(
    val runtime: RuntimeDiagnostics,
    val plans: PlanRuntimeDiagnostics,
    val pendingActions: List<PendingAction>,
    val activePlans: List<MayraPlan>,
    val recentAuditCount: Int,
    val capturedAt: Long
)

sealed interface RuntimeControlResult {
    data class Success(val message: String) : RuntimeControlResult
    data class NotFound(val message: String) : RuntimeControlResult
    data class InvalidState(val message: String) : RuntimeControlResult
    data class Failure(val message: String) : RuntimeControlResult
}

/**
 * One safe boundary for runtime inspection and user-authorized controls.
 * It never bypasses confirmation or skill permission checks.
 */
class MayraRuntimeControlCenter(
    context: Context,
    private val orchestrator: MayraRuntimeOrchestrator,
    private val planRuntime: MayraPlanRuntime,
    private val planStore: MayraPlanStore,
    private val pendingActions: PendingActionStore = PendingActionStore(context.applicationContext),
    private val audit: TrustAuditStore = TrustAuditStore(context.applicationContext),
    private val now: () -> Long = System::currentTimeMillis
) {
    fun snapshot(auditLimit: Int = 100): RuntimeControlSnapshot = RuntimeControlSnapshot(
        runtime = orchestrator.diagnostics(),
        plans = planRuntime.diagnostics(now()),
        pendingActions = pendingActions.waiting(now()),
        activePlans = planStore.active(now()),
        recentAuditCount = audit.snapshot(auditLimit.coerceIn(1, 500)).size,
        capturedAt = now()
    )

    fun approvePendingAction(actionId: String): RuntimeControlResult {
        val action = pendingActions.snapshot().firstOrNull { it.id == actionId }
            ?: return RuntimeControlResult.NotFound("Pending action not found.")
        if (action.state != PendingActionState.WAITING) {
            return RuntimeControlResult.InvalidState("Action is no longer waiting for approval.")
        }
        return if (pendingActions.approve(actionId, now()) != null) {
            RuntimeControlResult.Success("Action approved. It is ready for execution.")
        } else {
            RuntimeControlResult.Failure("Action could not be approved.")
        }
    }

    fun rejectPendingAction(actionId: String): RuntimeControlResult {
        val action = pendingActions.snapshot().firstOrNull { it.id == actionId }
            ?: return RuntimeControlResult.NotFound("Pending action not found.")
        if (action.state !in setOf(PendingActionState.WAITING, PendingActionState.APPROVED)) {
            return RuntimeControlResult.InvalidState("Action can no longer be rejected.")
        }
        return if (pendingActions.reject(actionId, now()) != null) {
            RuntimeControlResult.Success("Action rejected.")
        } else {
            RuntimeControlResult.Failure("Action could not be rejected.")
        }
    }

    fun confirmPlanStep(planId: String, stepId: String): RuntimeControlResult =
        if (planRuntime.confirmStep(planId, stepId)) {
            RuntimeControlResult.Success("Plan step confirmed.")
        } else {
            RuntimeControlResult.InvalidState("Plan step could not be confirmed.")
        }

    fun cancelPlan(planId: String): RuntimeControlResult =
        if (planRuntime.cancel(planId) != null) {
            RuntimeControlResult.Success("Plan cancelled.")
        } else {
            RuntimeControlResult.NotFound("Plan not found.")
        }

    suspend fun executeNextPlanStep(planId: String): RuntimeControlResult = runCatching {
        val result = planRuntime.executeNext(planId, now())
        when {
            result.waitingForConfirmation -> RuntimeControlResult.InvalidState("The next plan step needs confirmation.")
            result.idle -> RuntimeControlResult.InvalidState("Plan has no executable step right now.")
            result.executedStepId != null -> RuntimeControlResult.Success("Executed the next plan step.")
            else -> RuntimeControlResult.InvalidState("No step was executed.")
        }
    }.getOrElse {
        RuntimeControlResult.Failure(it.message ?: "Plan execution failed.")
    }

    fun clearCompletedHistory(): Int {
        val removable = planStore.snapshot().filter {
            it.state.name in setOf("COMPLETED", "FAILED", "CANCELLED")
        }
        removable.forEach { planStore.remove(it.id) }
        return removable.size
    }
}
