package ai.mayra.app.chat

import ai.mayra.app.core.MayraRouteHandler
import ai.mayra.app.core.MayraRoutingRuntime
import ai.mayra.app.core.MayraRoutingRuntimeResult
import ai.mayra.app.core.MayraRuntimeCapabilities
import ai.mayra.app.core.MayraRuntimeHandlers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraChatRuntimeBridgeTest {
    private fun bridge(actionOutput: String = "done"): MayraChatRuntimeBridge {
        val runtime = MayraRoutingRuntime(
            capabilities = MayraRuntimeCapabilities(
                coreAssistant = true,
                documentLibrary = true,
                deviceActions = true
            ),
            handlers = MayraRuntimeHandlers(
                answer = MayraRouteHandler { _, _ -> "typed answer" },
                retrieve = MayraRouteHandler { _, _ -> "grounded document result" },
                act = MayraRouteHandler { _, _ -> actionOutput }
            )
        )
        return MayraChatRuntimeBridge(runtime)
    }

    @Test
    fun conversationalAnswerDelegatesToStableAssistant() {
        assertTrue(bridge().dispatch("Hello Mayra") is MayraChatBridgeResult.DelegateToAssistant)
    }

    @Test
    fun documentRequestUsesTypedRuntime() {
        val result = bridge().dispatch("Search my documents for Rahul")
        assertEquals("grounded document result", (result as MayraChatBridgeResult.Reply).text)
    }

    @Test
    fun destructiveActionCreatesExactPendingConfirmationWithExpiry() {
        val result = bridge().dispatch("Delete file report.pdf") as MayraChatBridgeResult.NeedsConfirmation
        assertEquals("Delete file report.pdf", result.pending.message)
        assertTrue(result.pending.token.isNotBlank())
        assertTrue(result.pending.prompt.contains("confirm", ignoreCase = true))
        assertTrue(result.pending.expiresAtEpochMillis > System.currentTimeMillis())
        assertTrue(!result.pending.isExpired())
    }

    @Test
    fun pendingConfirmationExecutesOnceAndReplayIsBlocked() {
        val bridge = bridge("action executed")
        val pending = (bridge.dispatch("Delete file report.pdf") as MayraChatBridgeResult.NeedsConfirmation).pending

        assertEquals("action executed", bridge.confirm(pending).text)
        assertTrue(bridge.confirm(pending).text.contains("rejected", ignoreCase = true))
    }

    @Test
    fun expiredPendingConfirmationIsRejectedBeforeRuntimeDispatch() {
        val bridge = bridge("should not execute")
        val expired = PendingChatConfirmation(
            message = "Delete file report.pdf",
            token = "expired-token",
            prompt = "Confirm action",
            expiresAtEpochMillis = 1L
        )

        val reply = bridge.confirm(expired).text

        assertTrue(reply.contains("expired", ignoreCase = true))
        assertTrue(reply.contains("request the action again", ignoreCase = true))
    }

    @Test
    fun runtimeResultTextIsDeterministic() {
        val result = MayraRoutingRuntimeResult.Failed(
            plan = ai.mayra.app.core.MayraRoutingPolicy.routeAndPlan(
                "Open file manager",
                MayraRuntimeCapabilities(deviceActions = true)
            ),
            reason = "failure detail"
        )
        assertEquals("failure detail", result.userText())
    }
}
