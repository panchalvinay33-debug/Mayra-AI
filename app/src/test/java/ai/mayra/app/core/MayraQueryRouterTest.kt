package ai.mayra.app.core

import org.junit.Assert.assertEquals
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
    fun delegatesNormalConversationWithoutDocumentSignals() {
        val decision = MayraQueryRouter.route("How are you today?")

        assertEquals(MayraQueryRoute.DELEGATE, decision.route)
        assertEquals(100, decision.confidence)
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
