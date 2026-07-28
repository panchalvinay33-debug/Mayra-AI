package ai.mayra.app.core

sealed interface MayraRoutingRuntimeResult {
    val plan: MayraRoutingPlan

    data class Executed(
        override val plan: MayraRoutingPlan,
        val output: String
    ) : MayraRoutingRuntimeResult

    data class ConfirmationRequired(
        override val plan: MayraRoutingPlan,
        val prompt: String
    ) : MayraRoutingRuntimeResult

    data class ClarificationRequired(
        override val plan: MayraRoutingPlan,
        val prompt: String
    ) : MayraRoutingRuntimeResult

    data class Blocked(
        override val plan: MayraRoutingPlan,
        val reason: String
    ) : MayraRoutingRuntimeResult

    data class DuplicateBlocked(
        override val plan: MayraRoutingPlan,
        val idempotencyKey: String,
        val reason: String
    ) : MayraRoutingRuntimeResult

    data class Failed(
        override val plan: MayraRoutingPlan,
        val reason: String
    ) : MayraRoutingRuntimeResult
}

fun interface MayraRouteHandler {
    fun handle(message: String, decision: MayraRoutingDecision): String
}

data class MayraRuntimeHandlers(
    val answer: MayraRouteHandler? = null,
    val retrieve: MayraRouteHandler? = null,
    val act: MayraRouteHandler? = null
) {
    fun forOutcome(outcome: MayraRoutingOutcome): MayraRouteHandler? = when (outcome) {
        MayraRoutingOutcome.ANSWER -> answer
        MayraRoutingOutcome.RETRIEVE -> retrieve
        MayraRoutingOutcome.ACT -> act
        MayraRoutingOutcome.CLARIFY,
        MayraRoutingOutcome.UNSUPPORTED -> null
    }
}

/**
 * Audited runtime boundary between classification/planning and concrete assistant adapters.
 *
 * Confirmation, clarification and blocked plans never reach handlers. Action execution is protected
 * by an idempotency reservation. Failed actions release their reservation so an explicit retry can
 * proceed, while successful actions remain reserved to prevent accidental duplicate execution.
 */
class MayraRoutingRuntime(
    private val capabilities: MayraRuntimeCapabilities,
    private val handlers: MayraRuntimeHandlers,
    private val idempotencyStore: MayraIdempotencyStore = MayraInMemoryIdempotencyStore(),
    private val activityRecorder: MayraActivityRecorder? = null
) {
    fun dispatch(message: String): MayraRoutingRuntimeResult {
        val plan = MayraRoutingPolicy.routeAndPlan(message, capabilities)
        return dispatch(message, plan)
    }

    fun dispatch(message: String, plan: MayraRoutingPlan): MayraRoutingRuntimeResult = when (plan.disposition) {
        MayraRouteDisposition.CONFIRM -> MayraRoutingRuntimeResult.ConfirmationRequired(
            plan = plan,
            prompt = "Please confirm before Mayra performs this action."
        ).also { activityRecorder?.record(plan, MayraActivityStatus.CONFIRMATION_REQUIRED, it.prompt) }

        MayraRouteDisposition.CLARIFY -> MayraRoutingRuntimeResult.ClarificationRequired(
            plan = plan,
            prompt = plan.reason
        ).also { activityRecorder?.record(plan, MayraActivityStatus.CLARIFICATION_REQUIRED, it.prompt) }

        MayraRouteDisposition.BLOCK -> MayraRoutingRuntimeResult.Blocked(
            plan = plan,
            reason = plan.reason
        ).also { activityRecorder?.record(plan, MayraActivityStatus.BLOCKED, it.reason) }

        MayraRouteDisposition.FALLBACK,
        MayraRouteDisposition.EXECUTE -> execute(message, plan)
    }

    private fun execute(message: String, plan: MayraRoutingPlan): MayraRoutingRuntimeResult {
        val idempotencyKey = if (plan.decision.outcome == MayraRoutingOutcome.ACT) {
            MayraActionIdempotency.key(message, plan.decision)
        } else null

        if (idempotencyKey != null && !idempotencyStore.reserve(idempotencyKey)) {
            return MayraRoutingRuntimeResult.DuplicateBlocked(
                plan = plan,
                idempotencyKey = idempotencyKey,
                reason = "This action was already executed or is currently in progress."
            ).also {
                activityRecorder?.record(
                    plan,
                    MayraActivityStatus.DUPLICATE_BLOCKED,
                    it.reason,
                    idempotencyKey
                )
            }
        }

        val handler = handlers.forOutcome(plan.decision.outcome)
        if (handler == null) {
            idempotencyKey?.let(idempotencyStore::release)
            return MayraRoutingRuntimeResult.Failed(
                plan = plan,
                reason = "No runtime handler is registered for ${plan.decision.outcome.name}."
            ).also {
                activityRecorder?.record(plan, MayraActivityStatus.FAILED, it.reason, idempotencyKey)
            }
        }

        return runCatching { handler.handle(message, plan.decision).trim() }
            .fold(
                onSuccess = { output ->
                    if (output.isBlank()) {
                        idempotencyKey?.let(idempotencyStore::release)
                        MayraRoutingRuntimeResult.Failed(plan, "The runtime handler returned an empty result.")
                            .also {
                                activityRecorder?.record(plan, MayraActivityStatus.FAILED, it.reason, idempotencyKey)
                            }
                    } else {
                        MayraRoutingRuntimeResult.Executed(plan, output).also {
                            activityRecorder?.record(plan, MayraActivityStatus.EXECUTED, output, idempotencyKey)
                        }
                    }
                },
                onFailure = { error ->
                    idempotencyKey?.let(idempotencyStore::release)
                    MayraRoutingRuntimeResult.Failed(
                        plan = plan,
                        reason = error.message ?: "The runtime handler failed."
                    ).also {
                        activityRecorder?.record(plan, MayraActivityStatus.FAILED, it.reason, idempotencyKey)
                    }
                }
            )
    }
}