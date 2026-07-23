package ai.mayra.app.core.intelligence

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToolInvocationPipelineTest {
    @Test
    fun `successful invocation validates and records event`() = runTest {
        val registry = ToolRegistry()
        registry.register(EchoTool())
        val pipeline = ToolInvocationPipeline(registry)

        val result = pipeline.invoke(
            ToolInvocation(
                toolId = "utility.echo",
                arguments = mapOf("text" to "hello"),
                context = ToolExecutionContext("s1")
            )
        )

        assertEquals(ToolExecutionStatus.SUCCESS, result.status)
        assertEquals("hello", result.output)
        assertEquals("1", result.metadata["attempts"])
        assertEquals(1, pipeline.eventSnapshot().size)
        assertEquals(1, pipeline.telemetrySnapshot("utility.echo").size)
    }

    @Test
    fun `missing required argument fails before tool execution`() = runTest {
        val tool = EchoTool()
        val registry = ToolRegistry().apply { register(tool) }
        val result = ToolInvocationPipeline(registry).invoke(
            ToolInvocation("utility.echo", context = ToolExecutionContext("s1"))
        )

        assertEquals(ToolExecutionStatus.FAILED, result.status)
        assertEquals("missing_required_argument", result.errorCode)
        assertEquals(0, tool.calls)
    }

    @Test
    fun `high risk tool requires explicit confirmation`() = runTest {
        val registry = ToolRegistry().apply { register(RiskyTool()) }
        val pipeline = ToolInvocationPipeline(registry)
        val invocation = ToolInvocation("device.erase", context = ToolExecutionContext("s1"))

        assertEquals(ToolExecutionStatus.DENIED, pipeline.invoke(invocation).status)
        assertEquals(ToolExecutionStatus.SUCCESS, pipeline.invoke(invocation, confirmed = true).status)
    }

    @Test
    fun `tool exception becomes structured failure`() = runTest {
        val registry = ToolRegistry().apply { register(FailingTool()) }
        val result = ToolInvocationPipeline(registry).invoke(
            ToolInvocation("utility.fail", context = ToolExecutionContext("s1"))
        )

        assertEquals(ToolExecutionStatus.FAILED, result.status)
        assertEquals("tool_execution_failed", result.errorCode)
        assertEquals("3", result.metadata["attempts"])
        assertTrue(result.metadata.containsKey("exception"))
    }

    @Test
    fun `transient failure is retried through resilient executor`() = runTest {
        val tool = FlakyTool()
        val registry = ToolRegistry().apply { register(tool) }
        val executor = ResilientToolExecutor(
            retryPolicy = ToolRetryPolicy(maxAttempts = 2, initialDelayMillis = 0, maxDelayMillis = 0)
        )
        val pipeline = ToolInvocationPipeline(registry, executor = executor)

        val result = pipeline.invoke(
            ToolInvocation("utility.flaky", context = ToolExecutionContext("s1"))
        )

        assertEquals(ToolExecutionStatus.SUCCESS, result.status)
        assertEquals("recovered", result.output)
        assertEquals("2", result.metadata["attempts"])
        assertEquals(2, tool.calls)
        assertEquals(2, pipeline.telemetrySnapshot("utility.flaky").size)
        assertEquals(1, pipeline.eventSnapshot("utility.flaky").size)
    }

    private class EchoTool : MayraTool {
        var calls = 0
        override val manifest = ToolManifest(
            "utility.echo", "Echo", "Returns provided text",
            parameters = listOf(ToolParameter("text", "Text to return"))
        )
        override suspend fun execute(invocation: ToolInvocation): ToolResult {
            calls += 1
            return ToolResult(manifest.id, ToolExecutionStatus.SUCCESS, invocation.arguments["text"])
        }
    }

    private class RiskyTool : MayraTool {
        override val manifest = ToolManifest(
            "device.erase", "Erase", "Dangerous test action", riskLevel = ToolRiskLevel.HIGH
        )
        override suspend fun execute(invocation: ToolInvocation) =
            ToolResult(manifest.id, ToolExecutionStatus.SUCCESS, "confirmed")
    }

    private class FailingTool : MayraTool {
        override val manifest = ToolManifest("utility.fail", "Fail", "Always fails")
        override suspend fun execute(invocation: ToolInvocation): ToolResult = error("boom")
    }

    private class FlakyTool : MayraTool {
        var calls = 0
        override val manifest = ToolManifest("utility.flaky", "Flaky", "Fails once then succeeds")

        override suspend fun execute(invocation: ToolInvocation): ToolResult {
            calls += 1
            return if (calls == 1) {
                ToolResult(manifest.id, ToolExecutionStatus.FAILED, errorCode = "temporary_failure")
            } else {
                ToolResult(manifest.id, ToolExecutionStatus.SUCCESS, output = "recovered")
            }
        }
    }
}
