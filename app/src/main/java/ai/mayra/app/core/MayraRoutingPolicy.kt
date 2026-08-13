package ai.mayra.app.core

enum class MayraRouteDisposition {
    EXECUTE,
    CONFIRM,
    CLARIFY,
    FALLBACK,
    BLOCK
}

data class MayraRuntimeCapabilities(
    val coreAssistant: Boolean = true,
    val documentLibrary: Boolean = true,
    val deviceActions: Boolean = false,
    val documentOcr: Boolean = false,
    val legacyDocParser: Boolean = false
) {
    fun supports(capability: MayraRequiredCapability): Boolean = when (capability) {
        MayraRequiredCapability.CORE_ASSISTANT -> coreAssistant
        MayraRequiredCapability.DOCUMENT_LIBRARY -> documentLibrary
        MayraRequiredCapability.DEVICE_ACTIONS -> deviceActions
        MayraRequiredCapability.DOCUMENT_OCR -> documentOcr
        MayraRequiredCapability.LEGACY_DOC_PARSER -> legacyDocParser
    }
}

data class MayraRoutingPlan(
    val decision: MayraRoutingDecision,
    val disposition: MayraRouteDisposition,
    val reason: String
) {
    init {
        require(reason.isNotBlank()) { "Routing plans require an explicit reason." }
        require(disposition != MayraRouteDisposition.CONFIRM || decision.outcome == MayraRoutingOutcome.ACT) {
            "Only action routes may request confirmation."
        }
    }

    val mayExecute: Boolean
        get() = disposition == MayraRouteDisposition.EXECUTE
}

/**
 * Applies runtime availability and confirmation policy after deterministic intent classification.
 *
 * This layer never executes providers or actions. It only produces an auditable plan so unavailable
 * capabilities cannot be called accidentally and state-changing requests cannot bypass confirmation.
 */
object MayraRoutingPolicy {
    fun plan(
        decision: MayraRoutingDecision,
        capabilities: MayraRuntimeCapabilities = MayraRuntimeCapabilities()
    ): MayraRoutingPlan {
        if (decision.outcome == MayraRoutingOutcome.CLARIFY) {
            return MayraRoutingPlan(
                decision = decision,
                disposition = MayraRouteDisposition.CLARIFY,
                reason = decision.reason
            )
        }

        if (decision.outcome == MayraRoutingOutcome.UNSUPPORTED) {
            return MayraRoutingPlan(
                decision = decision,
                disposition = MayraRouteDisposition.BLOCK,
                reason = decision.reason
            )
        }

        if (!capabilities.supports(decision.requiredCapability)) {
            val fallbackAllowed = decision.outcome == MayraRoutingOutcome.ANSWER && capabilities.coreAssistant
            return MayraRoutingPlan(
                decision = decision,
                disposition = if (fallbackAllowed) MayraRouteDisposition.FALLBACK else MayraRouteDisposition.BLOCK,
                reason = "Required capability ${decision.requiredCapability.name} is unavailable at runtime."
            )
        }

        if (decision.outcome == MayraRoutingOutcome.ACT && decision.requiresConfirmation) {
            return MayraRoutingPlan(
                decision = decision,
                disposition = MayraRouteDisposition.CONFIRM,
                reason = "This state-changing action requires explicit user confirmation before execution."
            )
        }

        return MayraRoutingPlan(
            decision = decision,
            disposition = MayraRouteDisposition.EXECUTE,
            reason = "The required capability is available and all routing safety gates passed."
        )
    }

    fun routeAndPlan(
        message: String,
        capabilities: MayraRuntimeCapabilities = MayraRuntimeCapabilities()
    ): MayraRoutingPlan = plan(MayraQueryRouter.route(message), capabilities)
}
