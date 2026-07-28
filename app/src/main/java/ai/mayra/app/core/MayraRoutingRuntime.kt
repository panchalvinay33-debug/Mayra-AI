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
 * Confirmation, clarification and blocked plans never reach handlers. Handler failures are caught
 * and returned as typed failures so providers cannot crash or silently bypass the routing policy.
 */
class MayraRoutingRuntime(
    private val capabilities: MayraRuntimeCapabilities,
    private val handlers: MayraRuntimeHandlers
) {
    fun dispatch(message: String): MayraRoutingRuntimeResult {
        val plan = MayraRoutingPolicy.routeAndPlan(message, capabilities)
        return dispatch(message, plan)
    }

    fun dispatch(message: String, plan: MayraRoutingPlan): MayraRoutingRuntimeResult = when (plan.disposition) {
        MayraRouteDisposition.CONFIRM -> MayraRoutingRuntimeResult.ConfirmationRequired(
            plan = plan,
            prompt = "Please confirm before Mayra performs this action."
        )

        MayraRouteDisposition.CLARIFY -> MayraRoutingRuntimeResult.ClarificationRequired(
            plan = plan,
            prompt = plan.reason
        )

        MayraRouteDisposition.BLOCK -> MayraRoutingRuntimeResult.Blocked(
            plan = plan,
            reason = plan.reason
        )

        MayraRouteDisposition.FALLBACK,
        MayraRouteDisposition.EXECUTE -> execute(message, plan)
    }

    private fun execute(message: String, plan: MayraRoutingPlan): MayraRoutingRuntimeResult {
        val handler = handlers.forOutcome(plan.decision.outcome)
            ?: return MayraRoutingRuntimeResult.Failed(
                plan = plan,
                reason = "No runtime handler is registered for ${plan.decision.outcome.name}."
            )

        return runCatching { handler.handle(message, plan.decision).trim() }
            .fold(
                onSuccess = { output ->
                    if (output.isBlank()) {
                        MayraRoutingRuntimeResult.Failed(plan, "The runtime handler returned an empty result.")
                    } else {
                        MayraRoutingRuntimeResult.Executed(plan, output)
                    }
                },
                onFailure = { error ->
                    MayraRoutingRuntimeResult.Failed(
                        plan = plan,
                        reason = error.message ?: "The runtime handler failed."
                    )
                }
            )
    }
}
