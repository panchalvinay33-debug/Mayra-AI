package ai.mayra.app.core.intelligence

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ResilientToolExecutorTest {
    @Test
    fun `retries transient failure and records each attempt`() = runBlocking {
        var executions = 0
        val tool = object : MayraTool {
            override val manifest = ToolManifest(
                id = "test.retry",
                displayName = "Retry tool",
                description = "Fails once and succeeds."
            )

            override suspend fun execute(invocation: ToolInvocation): ToolResult {
                executions += 1
                return if (executions == 1) {
                    ToolResult(manifest.id, ToolExecutionStatus.FAILED, errorCode = "temporary_failure")
                } else {
                    ToolResult(manifest.id, ToolExecutionStatus.SUCCESS, output = "done")
                }
            }
        }
        val waits = mutableListOf<Long>()
        val executor = ResilientToolExecutor(
            retryPolicy = ToolRetryPolicy(initialDelayMillis = 25),
            now = { Instant.parse("2026-01-01T00:00:00Z") },
            delay = { waits += it }
        )

        val result = executor.execute(tool, invocation("test.retry"))

        assertEquals(ToolExecutionStatus.SUCCESS, result.status)
        assertEquals("2", result.metadata["attempts"])
        assertEquals(listOf(25L), waits)
        assertEquals(2, executor.telemetrySnapshot("test.retry").size)
    }

    @Test
    fun `does not retry denied result`() = runBlocking {
        var executions = 0
        val tool = object : MayraTool {
            override val manifest = ToolManifest("test.denied", "Denied", "Always denied.")
            override suspend fun execute(invocation: ToolInvocation): ToolResult {
                executions += 1
                return ToolResult(manifest.id, ToolExecutionStatus.DENIED, errorCode = "permission_denied")
            }
        }
        val executor = ResilientToolExecutor()

        val result = executor.execute(tool, invocation("test.denied"))

        assertEquals(ToolExecutionStatus.DENIED, result.status)
        assertEquals(1, executions)
        assertEquals("1", result.metadata["attempts"])
    }

    private fun invocation(toolId: String) = ToolInvocation(
        toolId = toolId,
        context = ToolExecutionContext(sessionId = "session")
    )
}
