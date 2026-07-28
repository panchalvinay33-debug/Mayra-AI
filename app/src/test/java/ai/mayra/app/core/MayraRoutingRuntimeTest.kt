package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRoutingRuntimeTest {
    private val handlers = MayraRuntimeHandlers(
        answer = MayraRouteHandler { message, _ -> "answer:$message" },
        retrieve = MayraRouteHandler { message, _ -> "retrieve:$message" },
        act = MayraRouteHandler { message, _ -> "act:$message" }
    )

    @Test fun answerRouteExecutesAnswerHandler() {
        val result = MayraRoutingRuntime(MayraRuntimeCapabilities(coreAssistant = true), handlers).dispatch("How are you?")
        assertTrue(result is MayraRoutingRuntimeResult.Executed)
        assertEquals("answer:How are you?", (result as MayraRoutingRuntimeResult.Executed).output)
    }

    @Test fun documentRouteExecutesRetrieveHandler() {
        val result = MayraRoutingRuntime(MayraRuntimeCapabilities(documentLibrary = true), handlers).dispatch("Search my documents for Rahul")
        assertTrue(result is MayraRoutingRuntimeResult.Executed)
    }

    @Test fun unavailableDocumentCapabilityBlocksBeforeHandler() {
        var called = false
        val runtime = MayraRoutingRuntime(MayraRuntimeCapabilities(documentLibrary = false), handlers.copy(retrieve = MayraRouteHandler { _, _ -> called = true; "bad" }))
        assertTrue(runtime.dispatch("Search my documents for Rahul") is MayraRoutingRuntimeResult.Blocked)
        assertTrue(!called)
    }

    @Test fun destructiveActionReturnsConfirmationWithoutExecution() {
        var called = false
        val runtime = MayraRoutingRuntime(MayraRuntimeCapabilities(deviceActions = true), handlers.copy(act = MayraRouteHandler { _, _ -> called = true; "bad" }))
        val result = runtime.dispatch("Delete file report.pdf")
        assertTrue(result is MayraRoutingRuntimeResult.ConfirmationRequired)
        assertNotNull((result as MayraRoutingRuntimeResult.ConfirmationRequired).token)
        assertTrue(!called)
    }

    @Test fun validConfirmationExecutesExactlyOnce() {
        var calls = 0
        val runtime = MayraRoutingRuntime(MayraRuntimeCapabilities(deviceActions = true), handlers.copy(act = MayraRouteHandler { _, _ -> calls++; "deleted" }))
        val pending = runtime.dispatch("Delete file report.pdf") as MayraRoutingRuntimeResult.ConfirmationRequired
        val token = pending.token!!.value
        assertTrue(runtime.confirmAndDispatch("Delete file report.pdf", token) is MayraRoutingRuntimeResult.Executed)
        assertEquals(1, calls)
        assertTrue(runtime.confirmAndDispatch("Delete file report.pdf", token) is MayraRoutingRuntimeResult.Blocked)
        assertEquals(1, calls)
    }

    @Test fun confirmationCannotApproveDifferentAction() {
        var calls = 0
        val runtime = MayraRoutingRuntime(MayraRuntimeCapabilities(deviceActions = true), handlers.copy(act = MayraRouteHandler { _, _ -> calls++; "bad" }))
        val pending = runtime.dispatch("Delete file report.pdf") as MayraRoutingRuntimeResult.ConfirmationRequired
        assertTrue(runtime.confirmAndDispatch("Delete file other.pdf", pending.token!!.value) is MayraRoutingRuntimeResult.Blocked)
        assertEquals(0, calls)
    }

    @Test fun safeActionExecutesWhenCapabilityExists() {
        val result = MayraRoutingRuntime(MayraRuntimeCapabilities(deviceActions = true), handlers).dispatch("Open file manager")
        assertTrue(result is MayraRoutingRuntimeResult.Executed)
    }

    @Test fun blankInputRequestsClarificationWithoutHandler() {
        assertTrue(MayraRoutingRuntime(MayraRuntimeCapabilities(), handlers).dispatch("  ") is MayraRoutingRuntimeResult.ClarificationRequired)
    }

    @Test fun unsupportedOcrIsBlocked() {
        assertTrue(MayraRoutingRuntime(MayraRuntimeCapabilities(documentOcr = false), handlers).dispatch("Read text from my scanned PDF using OCR") is MayraRoutingRuntimeResult.Blocked)
    }

    @Test fun missingHandlerReturnsTypedFailure() {
        val result = MayraRoutingRuntime(MayraRuntimeCapabilities(coreAssistant = true), MayraRuntimeHandlers()).dispatch("Hello")
        assertTrue(result is MayraRoutingRuntimeResult.Failed)
    }

    @Test fun handlerExceptionReturnsTypedFailure() {
        val result = MayraRoutingRuntime(MayraRuntimeCapabilities(coreAssistant = true), MayraRuntimeHandlers(answer = MayraRouteHandler { _, _ -> error("provider down") })).dispatch("Hello")
        assertEquals("provider down", (result as MayraRoutingRuntimeResult.Failed).reason)
    }

    @Test fun emptyHandlerOutputReturnsTypedFailure() {
        val result = MayraRoutingRuntime(MayraRuntimeCapabilities(coreAssistant = true), MayraRuntimeHandlers(answer = MayraRouteHandler { _, _ -> "   " })).dispatch("Hello")
        assertTrue(result is MayraRoutingRuntimeResult.Failed)
    }
}
