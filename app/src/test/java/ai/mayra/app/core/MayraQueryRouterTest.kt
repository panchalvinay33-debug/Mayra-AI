package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraQueryRouterTest {
    @Test
    fun routesExplicitDocumentSearchToLocalDocuments() {
        val decision = MayraQueryRouter.route("Search my documents for payment terms")

        assertEquals(MayraQueryRoute.DOCUMENTS, decision.route)
        assertTrue(decision.confidence >= 90)
        assertTrue(decision.matchedSignals.contains("documents"))
    }

    @Test
    fun routesDocumentQuestionInHindi() {
        val decision = MayraQueryRouter.route("मेरी फाइल में भुगतान की तारीख क्या है?")

        assertEquals(MayraQueryRoute.DOCUMENTS, decision.route)
        assertTrue(decision.confidence >= 80)
    }

    @Test
    fun routesShortDocumentAliases() {
        val docs = MayraQueryRouter.route("Find payment terms in my docs")
        val notes = MayraQueryRouter.route("Search my notes for Rahul")

        assertEquals(MayraQueryRoute.DOCUMENTS, docs.route)
        assertTrue(docs.matchedSignals.contains("docs"))
        assertEquals(MayraQueryRoute.DOCUMENTS, notes.route)
        assertTrue(notes.matchedSignals.contains("notes"))
    }

    @Test
    fun normalizesUnicodeAndFlexibleWhitespaceInMarkers() {
        val decision = MayraQueryRouter.route("मेरी फ़ाइल में भुगतान खोजो")
        val spaced = MayraQueryRouter.route("Look   for payment terms in my PDF")

        assertEquals(MayraQueryRoute.DOCUMENTS, decision.route)
        assertEquals(MayraQueryRoute.DOCUMENTS, spaced.route)
        assertTrue(spaced.matchedSignals.contains("look for"))
    }

    @Test
    fun delegatesNormalConversationWithoutDocumentSignals() {
        val decision = MayraQueryRouter.route("How are you today?")

        assertEquals(MayraQueryRoute.DELEGATE, decision.route)
        assertEquals(100, decision.confidence)
    }

    @Test
    fun doesNotTreatDocumentMarkerSubstringsAsDocuments() {
        val profile = MayraQueryRouter.route("What is shown on my profile?")
        val libraryWord = MayraQueryRouter.route("How does a librarian organize books?")

        assertEquals(MayraQueryRoute.DELEGATE, profile.route)
        assertEquals(MayraQueryRoute.DELEGATE, libraryWord.route)
        assertFalse(profile.matchedSignals.contains("file"))
        assertFalse(libraryWord.matchedSignals.contains("library"))
    }

    @Test
    fun delegatesDeviceFileActionsInsteadOfHijackingThem() {
        val decision = MayraQueryRouter.route("Open file manager")

        assertEquals(MayraQueryRoute.DELEGATE, decision.route)
        assertTrue(decision.matchedSignals.contains("file"))
    }

    @Test
    fun routesDocumentSummaryEvenWhenSentenceStartsWithOpen() {
        val decision = MayraQueryRouter.route("Open my project PDF and summarize it")

        assertEquals(MayraQueryRoute.DOCUMENTS, decision.route)
        assertTrue(decision.confidence >= 90)
    }
}
