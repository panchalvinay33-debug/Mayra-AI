package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraTypedQueryRouterTest {
    @Test
    fun blankInputRequiresClarification() {
        val decision = MayraTypedQueryRouter.route("   ")

        assertEquals(MayraRoutingOutcome.CLARIFY, decision.outcome)
        assertEquals(MayraRequiredCapability.CORE_ASSISTANT, decision.requiredCapability)
        assertEquals(100, decision.confidence)
    }

    @Test
    fun normalConversationUsesAnswerPath() {
        val decision = MayraTypedQueryRouter.route("How are you today?")

        assertEquals(MayraRoutingOutcome.ANSWER, decision.outcome)
        assertEquals(MayraRequiredCapability.CORE_ASSISTANT, decision.requiredCapability)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun explicitDocumentSearchUsesRetrievePath() {
        val decision = MayraTypedQueryRouter.route("Search my documents for payment terms")

        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertEquals(MayraRequiredCapability.DOCUMENT_LIBRARY, decision.requiredCapability)
        assertTrue(decision.confidence >= 90)
        assertTrue(decision.matchedSignals.contains("documents"))
    }

    @Test
    fun hindiDocumentQuestionUsesRetrievePath() {
        val decision = MayraTypedQueryRouter.route("मेरी फ़ाइल में भुगतान की तारीख क्या है?")

        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertEquals(MayraRequiredCapability.DOCUMENT_LIBRARY, decision.requiredCapability)
        assertTrue(decision.matchedSignals.contains("फ़ाइल"))
    }

    @Test
    fun ambiguousDocumentMentionRequiresClarification() {
        val decision = MayraTypedQueryRouter.route("My project PDF")

        assertEquals(MayraRoutingOutcome.CLARIFY, decision.outcome)
        assertEquals(MayraRequiredCapability.DOCUMENT_LIBRARY, decision.requiredCapability)
    }

    @Test
    fun safeDeviceActionDoesNotForceConfirmation() {
        val decision = MayraTypedQueryRouter.route("Open file manager")

        assertEquals(MayraRoutingOutcome.ACT, decision.outcome)
        assertEquals(MayraRequiredCapability.DEVICE_ACTIONS, decision.requiredCapability)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun destructiveActionRequiresConfirmation() {
        val decision = MayraTypedQueryRouter.route("Delete this file")

        assertEquals(MayraRoutingOutcome.ACT, decision.outcome)
        assertTrue(decision.requiresConfirmation)
        assertTrue(decision.reason.contains("confirmed"))
    }

    @Test
    fun hindiDestructiveActionRequiresConfirmation() {
        val decision = MayraTypedQueryRouter.route("इस फाइल को हटाओ")

        // The verb is not at the beginning, so this remains a clarification instead of unsafe execution.
        assertEquals(MayraRoutingOutcome.CLARIFY, decision.outcome)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun scannedPdfRequestIsExplicitlyUnsupported() {
        val decision = MayraTypedQueryRouter.route("OCR this scanned PDF")

        assertEquals(MayraRoutingOutcome.UNSUPPORTED, decision.outcome)
        assertEquals(MayraRequiredCapability.OCR, decision.requiredCapability)
        assertTrue(decision.confidence >= 95)
    }

    @Test
    fun legacyDocRequestIsExplicitlyUnsupported() {
        val decision = MayraTypedQueryRouter.route("Read this legacy DOC file")

        assertEquals(MayraRoutingOutcome.UNSUPPORTED, decision.outcome)
        assertEquals(MayraRequiredCapability.LEGACY_DOC, decision.requiredCapability)
    }

    @Test
    fun documentSummaryWinsOverLeadingOpenAction() {
        val decision = MayraTypedQueryRouter.route("Open my project PDF and summarize it")

        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun markerSubstringsDoNotHijackNormalConversation() {
        val profile = MayraTypedQueryRouter.route("What is shown on my profile?")
        val librarian = MayraTypedQueryRouter.route("How does a librarian organize books?")

        assertEquals(MayraRoutingOutcome.ANSWER, profile.outcome)
        assertEquals(MayraRoutingOutcome.ANSWER, librarian.outcome)
    }

    @Test
    fun everyOutcomeHasAuditableReasonAndValidConfidence() {
        val messages = listOf(
            "",
            "Hello",
            "Search my notes for Rahul",
            "Open settings",
            "Delete this file",
            "OCR this scanned PDF",
            "My PDF"
        )

        messages.map(MayraTypedQueryRouter::route).forEach { decision ->
            assertTrue(decision.reason.isNotBlank())
            assertTrue(decision.confidence in 0..100)
        }
    }

    @Test
    fun legacyRouterCompatibilityRemainsStable() {
        assertEquals(
            MayraQueryRoute.DOCUMENTS,
            MayraQueryRouter.route("Search my documents for payment terms").route
        )
        assertEquals(
            MayraQueryRoute.DELEGATE,
            MayraQueryRouter.route("Open file manager").route
        )
    }
}
