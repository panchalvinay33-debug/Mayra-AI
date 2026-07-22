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
        assertEquals(1, pipeline.eventSnapshot().size)
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
        assertTrue(result.metadata.containsKey("exception"))
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
}
