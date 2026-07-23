package ai.mayra.app.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class MayraAgentRuntimeTest {
    @Test(expected = IllegalArgumentException::class)
    fun `validator rejects dependency cycle`() {
        val tool = successTool()
        val a = toolStep("a", 0, dependencies = setOf("b"))
        val b = toolStep("b", 1, dependencies = setOf("a"))
        AgentPlanValidator().validate(plan(listOf(a, b)), mapOf(tool.descriptor.id to tool))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `high risk step must require confirmation`() {
        val tool = successTool(risk = AgentRisk.HIGH)
        AgentPlanValidator().validate(plan(listOf(toolStep("a", 0))), mapOf(tool.descriptor.id to tool))
    }

    @Test
    fun `independent steps execute in one parallel tick`() = runBlocking {
        val tool = successTool()
        val runtime = MayraAgentRuntime(listOf(tool))
        val submitted = runtime.submit(plan(listOf(toolStep("a", 0), toolStep("b", 1))))

        val tick = runtime.tick(submitted.id)

        assertEquals(setOf("a", "b"), tick.executedStepIds.toSet())
        assertEquals(AgentRunState.COMPLETED, tick.run.state)
        assertEquals(2L, runtime.diagnostics().executedSteps)
    }

    @Test
    fun `dependency waits for prior output`() = runBlocking {
        val calls = mutableListOf<String>()
        val tool = FunctionalAgentTool(descriptor()) { call, _ ->
            calls += call.arguments.getValue("name")
            AgentToolResult.Success("done", mapOf("value" to call.arguments.getValue("name")))
        }
        val first = toolStep("first", 0, args = mapOf("name" to "first"), bindings = mapOf("result.first" to "value"))
        val second = toolStep("second", 1, dependencies = setOf(first.id), args = mapOf("name" to "second"))
        val runtime = MayraAgentRuntime(listOf(tool))
        val run = runtime.submit(plan(listOf(first, second)))

        runtime.tick(run.id)
        val completed = runtime.tick(run.id).run

        assertEquals(listOf("first", "second"), calls)
        assertEquals("first", completed.workspace.variables["result.first"])
        assertEquals(AgentRunState.COMPLETED, completed.state)
    }

    @Test
    fun `sensitive step blocks until confirmation`() = runBlocking {
        val tool = successTool(risk = AgentRisk.HIGH)
        val step = toolStep("send", 0, requiresConfirmation = true)
        val runtime = MayraAgentRuntime(listOf(tool))
        val run = runtime.submit(plan(listOf(step)))

        val blocked = runtime.tick(run.id)
        assertEquals(step.id, blocked.waitingForConfirmationStepId)
        assertEquals(AgentRunState.BLOCKED, blocked.run.state)

        assertTrue(runtime.confirm(run.id, step.id))
        assertEquals(AgentRunState.COMPLETED, runtime.tick(run.id).run.state)
    }

    @Test
    fun `retryable failure uses bounded retry budget`() = runBlocking {
        val attempts = AtomicInteger(0)
        val tool = FunctionalAgentTool(descriptor()) { _, _ ->
            if (attempts.incrementAndGet() < 2) AgentToolResult.RetryableFailure("offline")
            else AgentToolResult.Success("recovered")
        }
        val step = toolStep("retry", 0, failurePolicy = AgentFailurePolicy.RETRY, maxAttempts = 3)
        val runtime = MayraAgentRuntime(listOf(tool))
        val run = runtime.submit(plan(listOf(step)))

        assertEquals(AgentRunState.READY, runtime.tick(run.id).run.state)
        assertEquals(AgentRunState.COMPLETED, runtime.tick(run.id).run.state)
        assertEquals(1L, runtime.diagnostics().retries)
    }

    @Test
    fun `tool timeout becomes failure`() = runBlocking {
        val tool = FunctionalAgentTool(descriptor(timeout = 100L)) { _, _ ->
            delay(500L)
            AgentToolResult.Success("late")
        }
        val runtime = MayraAgentRuntime(listOf(tool))
        val run = runtime.submit(plan(listOf(toolStep("slow", 0))))

        val result = runtime.tick(run.id).run

        assertEquals(AgentRunState.FAILED, result.state)
        assertTrue(result.lastError?.contains("timed out", ignoreCase = true) == true)
    }

    @Test
    fun `user input step blocks and resumes`() = runBlocking {
        val step = AgentStep(
            id = "input",
            order = 0,
            title = "Kaunsa city?",
            kind = AgentStepKind.USER_INPUT,
            outputBindings = mapOf("city" to "value")
        )
        val runtime = MayraAgentRuntime(emptyList())
        val run = runtime.submit(plan(listOf(step)))

        val blocked = runtime.tick(run.id)
        assertEquals("input", blocked.waitingForInputStepId)
        assertEquals("Kaunsa city?", blocked.run.workspace.pendingQuestion)

        val resumed = runtime.provideInput(run.id, "input", "Indore")
        assertNotNull(resumed)
        assertEquals("Indore", resumed?.workspace?.variables?.get("city"))
        assertEquals(AgentRunState.COMPLETED, resumed?.state)
    }

    @Test
    fun `cancel stops pending work`() = runBlocking {
        val runtime = MayraAgentRuntime(listOf(successTool()))
        val run = runtime.submit(plan(listOf(toolStep("a", 0), toolStep("b", 1))))

        val cancelled = runtime.cancel(run.id, "user changed mind")

        assertEquals(AgentRunState.CANCELLED, cancelled?.state)
        assertTrue(cancelled?.steps?.all { it.state == AgentStepState.CANCELLED } == true)
        assertTrue(runtime.tick(run.id).idle)
    }

    @Test
    fun `checkpoint contains bounded recovery state`() {
        val runtime = MayraAgentRuntime(listOf(successTool()))
        val run = runtime.submit(plan(listOf(toolStep("a", 0))), mapOf("city" to "Bhopal"))

        val checkpoint = runtime.checkpoint(run.id)

        assertNotNull(checkpoint)
        assertEquals(run.plan.id, checkpoint?.planId)
        assertEquals("Bhopal", checkpoint?.variables?.get("city"))
        assertEquals(AgentStepState.WAITING, checkpoint?.stepStates?.get("a"))
    }

    @Test
    fun `rollback compensates completed reversible steps`() = runBlocking {
        val compensated = AtomicInteger(0)
        val tool = FunctionalAgentTool(
            descriptor = descriptor(supportsCompensation = true),
            executor = { call, _ ->
                if (call.arguments["fail"] == "true") AgentToolResult.Failure("boom")
                else AgentToolResult.Success("created", mapOf("id" to "123"))
            },
            compensator = { _, _, _ ->
                compensated.incrementAndGet()
                AgentToolResult.Success("rolled back")
            }
        )
        val first = toolStep("first", 0)
        val second = toolStep(
            "second",
            1,
            dependencies = setOf(first.id),
            args = mapOf("fail" to "true"),
            failurePolicy = AgentFailurePolicy.ROLLBACK
        )
        val runtime = MayraAgentRuntime(listOf(tool))
        val run = runtime.submit(plan(listOf(first, second)))

        runtime.tick(run.id)
        val failed = runtime.tick(run.id).run

        assertEquals(AgentRunState.FAILED, failed.state)
        assertEquals(1, compensated.get())
        assertEquals(AgentStepState.COMPENSATED, failed.steps.first { it.id == first.id }.state)
    }

    @Test
    fun `planner creates weather reminder calendar and message chain`() {
        val registry = MayraAgentToolRegistry(
            listOf(
                successTool("search", setOf("weather", "unified_search")),
                successTool("personal", setOf("create_reminder", "create_note")),
                successTool("calendar", setOf("create_event")),
                successTool("communication", setOf("compose_message", "compose_whatsapp", "call"), AgentRisk.HIGH)
            )
        )
        val result = MayraAgentPlanner(registry, now = { 1_000L }).plan(
            AgentObjective("Kal subah meeting calendar me banao, reminder lagao, weather check karo aur Shiv ko WhatsApp message karo", requestedAt = 1_000L)
        )

        assertNotNull(result.plan)
        val calls = result.plan!!.steps.mapNotNull(AgentStep::call)
        assertTrue(calls.any { it.operation == "weather" })
        assertTrue(calls.any { it.operation == "create_reminder" })
        assertTrue(calls.any { it.operation == "create_event" })
        assertTrue(calls.any { it.operation == "compose_whatsapp" })
        assertTrue(result.plan.steps.first { it.call?.operation == "compose_whatsapp" }.requiresConfirmation)
    }

    @Test
    fun `planner asks for missing time`() {
        val registry = MayraAgentToolRegistry(listOf(successTool("personal", setOf("create_reminder"))))
        val result = MayraAgentPlanner(registry).plan(AgentObjective("Dawa lene ka reminder laga do"))

        assertEquals(null, result.plan)
        assertTrue(result.clarification?.contains("samay", ignoreCase = true) == true)
    }

    private fun descriptor(
        id: String = "test",
        operations: Set<String> = setOf("run"),
        risk: AgentRisk = AgentRisk.LOW,
        supportsCompensation: Boolean = false,
        timeout: Long = 1_000L
    ) = AgentToolDescriptor(
        id = id,
        displayName = id,
        operations = operations,
        risk = risk,
        supportsCompensation = supportsCompensation,
        timeoutMillis = timeout
    )

    private fun successTool(
        id: String = "test",
        operations: Set<String> = setOf("run"),
        risk: AgentRisk = AgentRisk.LOW
    ) = FunctionalAgentTool(descriptor(id, operations, risk)) { _, _ -> AgentToolResult.Success("done") }

    private fun toolStep(
        id: String,
        order: Int,
        dependencies: Set<String> = emptySet(),
        args: Map<String, String> = emptyMap(),
        bindings: Map<String, String> = emptyMap(),
        requiresConfirmation: Boolean = false,
        failurePolicy: AgentFailurePolicy = AgentFailurePolicy.STOP,
        maxAttempts: Int = 3
    ) = AgentStep(
        id = id,
        order = order,
        title = id,
        kind = AgentStepKind.TOOL,
        call = AgentToolCall("test", "run", args),
        dependencies = dependencies,
        outputBindings = bindings,
        requiresConfirmation = requiresConfirmation,
        failurePolicy = failurePolicy,
        maxAttempts = maxAttempts
    )

    private fun plan(steps: List<AgentStep>) = AgentPlan(
        title = "test plan",
        objective = "test objective",
        createdAt = 1_000L,
        expiresAt = Long.MAX_VALUE,
        steps = steps
    )
}
