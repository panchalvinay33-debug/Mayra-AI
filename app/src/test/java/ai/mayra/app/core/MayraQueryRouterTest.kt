package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraQueryRouterTest {
    @Test
    fun routesExplicitDocumentSearchToRetrieval() {
        val decision = MayraQueryRouter.route("Search my documents for payment terms")

        assertEquals(MayraQueryRoute.DOCUMENTS, decision.route)
        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertEquals(MayraRequiredCapability.DOCUMENT_LIBRARY, decision.requiredCapability)
        assertTrue(decision.confidence >= 90)
        assertTrue(decision.matchedSignals.contains("documents"))
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun routesDocumentQuestionInHindi() {
        val decision = MayraQueryRouter.route("मेरी फाइल में भुगतान की तारीख क्या है?")

        assertEquals(MayraQueryRoute.DOCUMENTS, decision.route)
        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertTrue(decision.confidence >= 80)
    }

    @Test
    fun routesShortDocumentAliases() {
        val docs = MayraQueryRouter.route("Find payment terms in my docs")
        val notes = MayraQueryRouter.route("Search my notes for Rahul")

        assertEquals(MayraRoutingOutcome.RETRIEVE, docs.outcome)
        assertTrue(docs.matchedSignals.contains("docs"))
        assertEquals(MayraRoutingOutcome.RETRIEVE, notes.outcome)
        assertTrue(notes.matchedSignals.contains("notes"))
    }

    @Test
    fun normalizesUnicodeAndFlexibleWhitespaceInMarkers() {
        val decision = MayraQueryRouter.route("मेरी फ़ाइल में भुगतान खोजो")
        val spaced = MayraQueryRouter.route("Look   for payment terms in my PDF")

        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertEquals(MayraRoutingOutcome.RETRIEVE, spaced.outcome)
        assertTrue(spaced.matchedSignals.contains("look for"))
    }

    @Test
    fun normalConversationUsesAnswerOutcome() {
        val decision = MayraQueryRouter.route("How are you today?")

        assertEquals(MayraQueryRoute.DELEGATE, decision.route)
        assertEquals(MayraRoutingOutcome.ANSWER, decision.outcome)
        assertEquals(MayraRequiredCapability.CORE_ASSISTANT, decision.requiredCapability)
        assertEquals(100, decision.confidence)
    }

    @Test
    fun blankInputRequestsClarification() {
        val decision = MayraQueryRouter.route("   ")

        assertEquals(MayraRoutingOutcome.CLARIFY, decision.outcome)
        assertEquals(100, decision.confidence)
        assertTrue(decision.reason.isNotBlank())
    }

    @Test
    fun doesNotTreatDocumentMarkerSubstringsAsDocuments() {
        val profile = MayraQueryRouter.route("What is shown on my profile?")
        val libraryWord = MayraQueryRouter.route("How does a librarian organize books?")

        assertEquals(MayraRoutingOutcome.ANSWER, profile.outcome)
        assertEquals(MayraRoutingOutcome.ANSWER, libraryWord.outcome)
        assertFalse(profile.matchedSignals.contains("file"))
        assertFalse(libraryWord.matchedSignals.contains("library"))
    }

    @Test
    fun routesDeviceFileOpenAsNonDestructiveAction() {
        val decision = MayraQueryRouter.route("Open file manager")

        assertEquals(MayraQueryRoute.DELEGATE, decision.route)
        assertEquals(MayraRoutingOutcome.ACT, decision.outcome)
        assertEquals(MayraRequiredCapability.DEVICE_ACTIONS, decision.requiredCapability)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun destructiveActionRequiresConfirmation() {
        val decision = MayraQueryRouter.route("Delete file report.pdf")

        assertEquals(MayraRoutingOutcome.ACT, decision.outcome)
        assertTrue(decision.requiresConfirmation)
    }

    @Test
    fun routesDocumentSummaryEvenWhenSentenceStartsWithOpen() {
        val decision = MayraQueryRouter.route("Open my project PDF and summarize it")

        assertEquals(MayraRoutingOutcome.RETRIEVE, decision.outcome)
        assertTrue(decision.confidence >= 90)
        assertFalse(decision.requiresConfirmation)
    }

    @Test
    fun unsupportedScannedPdfExplainsMissingCapability() {
        val decision = MayraQueryRouter.route("Read text from my scanned PDF using OCR")

        assertEquals(MayraRoutingOutcome.UNSUPPORTED, decision.outcome)
        assertEquals(MayraRequiredCapability.DOCUMENT_OCR, decision.requiredCapability)
        assertTrue(decision.reason.contains("OCR"))
    }

    @Test
    fun unsupportedLegacyDocSuggestsConversionBoundary() {
        val decision = MayraQueryRouter.route("Open my legacy DOC file")

        assertEquals(MayraRoutingOutcome.UNSUPPORTED, decision.outcome)
        assertEquals(MayraRequiredCapability.LEGACY_DOC_PARSER, decision.requiredCapability)
        assertTrue(decision.reason.contains("DOCX"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidConfidence() {
        MayraRoutingDecision(
            route = MayraQueryRoute.DELEGATE,
            confidence = 101,
            matchedSignals = emptyList()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun confirmationCanOnlyBeUsedForActions() {
        MayraRoutingDecision(
            route = MayraQueryRoute.DELEGATE,
            confidence = 90,
            matchedSignals = emptyList(),
            outcome = MayraRoutingOutcome.ANSWER,
            requiresConfirmation = true
        )
    }
}