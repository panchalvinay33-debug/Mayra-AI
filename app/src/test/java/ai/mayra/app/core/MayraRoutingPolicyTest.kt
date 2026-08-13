package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRoutingPolicyTest {
    @Test
    fun currentDocumentRetrievalExecutesWhenLibraryIsAvailable() {
        val plan = MayraRoutingPolicy.routeAndPlan("Search my PDF for payment terms")

        assertEquals(MayraRoutingOutcome.RETRIEVE, plan.decision.outcome)
        assertEquals(MayraRouteDisposition.EXECUTE, plan.disposition)
        assertTrue(plan.mayExecute)
    }

    @Test
    fun documentRetrievalIsBlockedWhenLibraryIsUnavailable() {
        val plan = MayraRoutingPolicy.routeAndPlan(
            "मेरी फाइल में भुगतान खोजो",
            MayraRuntimeCapabilities(documentLibrary = false)
        )

        assertEquals(MayraRouteDisposition.BLOCK, plan.disposition)
        assertFalse(plan.mayExecute)
        assertTrue(plan.reason.contains("DOCUMENT_LIBRARY"))
    }

    @Test
    fun nonDestructiveActionExecutesOnlyWhenActionCapabilityExists() {
        val unavailable = MayraRoutingPolicy.routeAndPlan("Open file manager")
        val available = MayraRoutingPolicy.routeAndPlan(
            "Open file manager",
            MayraRuntimeCapabilities(deviceActions = true)
        )

        assertEquals(MayraRouteDisposition.BLOCK, unavailable.disposition)
        assertEquals(MayraRouteDisposition.EXECUTE, available.disposition)
    }

    @Test
    fun destructiveActionRequiresConfirmationEvenWhenCapabilityExists() {
        val plan = MayraRoutingPolicy.routeAndPlan(
            "Delete file report.pdf",
            MayraRuntimeCapabilities(deviceActions = true)
        )

        assertEquals(MayraRouteDisposition.CONFIRM, plan.disposition)
        assertFalse(plan.mayExecute)
        assertTrue(plan.reason.contains("confirmation"))
    }

    @Test
    fun unsupportedOcrNeverExecutesEvenIfRuntimeFlagIsAccidentallyEnabled() {
        val plan = MayraRoutingPolicy.routeAndPlan(
            "Read text from my scanned PDF using OCR",
            MayraRuntimeCapabilities(documentOcr = true)
        )

        assertEquals(MayraRoutingOutcome.UNSUPPORTED, plan.decision.outcome)
        assertEquals(MayraRouteDisposition.BLOCK, plan.disposition)
    }

    @Test
    fun blankInputRemainsClarification() {
        val plan = MayraRoutingPolicy.routeAndPlan("   ")

        assertEquals(MayraRouteDisposition.CLARIFY, plan.disposition)
    }

    @Test
    fun normalAnswerExecutesThroughCoreAssistant() {
        val plan = MayraRoutingPolicy.routeAndPlan("Explain photosynthesis")

        assertEquals(MayraRoutingOutcome.ANSWER, plan.decision.outcome)
        assertEquals(MayraRouteDisposition.EXECUTE, plan.disposition)
    }

    @Test
    fun normalAnswerIsBlockedWhenCoreAssistantIsUnavailable() {
        val plan = MayraRoutingPolicy.routeAndPlan(
            "Explain photosynthesis",
            MayraRuntimeCapabilities(coreAssistant = false)
        )

        assertEquals(MayraRouteDisposition.BLOCK, plan.disposition)
    }

    @Test(expected = IllegalArgumentException::class)
    fun confirmationDispositionRejectsNonActionDecision() {
        MayraRoutingPlan(
            decision = MayraQueryRouter.route("Explain gravity"),
            disposition = MayraRouteDisposition.CONFIRM,
            reason = "Invalid test plan"
        )
    }
}
