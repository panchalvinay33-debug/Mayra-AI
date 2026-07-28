package ai.mayra.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRoutingRuntimeTest {
    private val handlers = MayraRuntimeHandlers(
        answer = MayraRouteHandler { message, _ -> "answer:$message" },
        retrieve = MayraRouteHandler { message, _ -> "retrieve:$message" },
        act = MayraRouteHandler { message, _ -> "act:$message" }
    )

    @Test
    fun answerRouteExecutesAnswerHandler() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(coreAssistant = true),
            handlers = handlers
        )

        val result = runtime.dispatch("How are you?")

        assertTrue(result is MayraRoutingRuntimeResult.Executed)
        assertEquals("answer:How are you?", (result as MayraRoutingRuntimeResult.Executed).output)
    }

    @Test
    fun documentRouteExecutesRetrieveHandler() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(documentLibrary = true),
            handlers = handlers
        )

        val result = runtime.dispatch("Search my documents for Rahul")

        assertTrue(result is MayraRoutingRuntimeResult.Executed)
        assertTrue((result as MayraRoutingRuntimeResult.Executed).output.startsWith("retrieve:"))
    }

    @Test
    fun unavailableDocumentCapabilityBlocksBeforeHandler() {
        var called = false
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(documentLibrary = false),
            handlers = handlers.copy(retrieve = MayraRouteHandler { _, _ -> called = true; "bad" })
        )

        val result = runtime.dispatch("Search my documents for Rahul")

        assertTrue(result is MayraRoutingRuntimeResult.Blocked)
        assertTrue(!called)
    }

    @Test
    fun destructiveActionReturnsConfirmationWithoutExecution() {
        var called = false
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(deviceActions = true),
            handlers = handlers.copy(act = MayraRouteHandler { _, _ -> called = true; "bad" })
        )

        val result = runtime.dispatch("Delete file report.pdf")

        assertTrue(result is MayraRoutingRuntimeResult.ConfirmationRequired)
        assertTrue(!called)
    }

    @Test
    fun safeActionExecutesWhenCapabilityExists() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(deviceActions = true),
            handlers = handlers
        )

        val result = runtime.dispatch("Open file manager")

        assertTrue(result is MayraRoutingRuntimeResult.Executed)
        assertTrue((result as MayraRoutingRuntimeResult.Executed).output.startsWith("act:"))
    }

    @Test
    fun blankInputRequestsClarificationWithoutHandler() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(),
            handlers = handlers
        )

        val result = runtime.dispatch("  ")

        assertTrue(result is MayraRoutingRuntimeResult.ClarificationRequired)
    }

    @Test
    fun unsupportedOcrIsBlocked() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(documentOcr = false),
            handlers = handlers
        )

        val result = runtime.dispatch("Read text from my scanned PDF using OCR")

        assertTrue(result is MayraRoutingRuntimeResult.Blocked)
    }

    @Test
    fun missingHandlerReturnsTypedFailure() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(coreAssistant = true),
            handlers = MayraRuntimeHandlers()
        )

        val result = runtime.dispatch("Hello")

        assertTrue(result is MayraRoutingRuntimeResult.Failed)
        assertTrue((result as MayraRoutingRuntimeResult.Failed).reason.contains("No runtime handler"))
    }

    @Test
    fun handlerExceptionReturnsTypedFailure() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(coreAssistant = true),
            handlers = MayraRuntimeHandlers(answer = MayraRouteHandler { _, _ -> error("provider down") })
        )

        val result = runtime.dispatch("Hello")

        assertTrue(result is MayraRoutingRuntimeResult.Failed)
        assertEquals("provider down", (result as MayraRoutingRuntimeResult.Failed).reason)
    }

    @Test
    fun emptyHandlerOutputReturnsTypedFailure() {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(coreAssistant = true),
            handlers = MayraRuntimeHandlers(answer = MayraRouteHandler { _, _ -> "   " })
        )

        val result = runtime.dispatch("Hello")

        assertTrue(result is MayraRoutingRuntimeResult.Failed)
        assertTrue((result as MayraRoutingRuntimeResult.Failed).reason.contains("empty result"))
    }
}
