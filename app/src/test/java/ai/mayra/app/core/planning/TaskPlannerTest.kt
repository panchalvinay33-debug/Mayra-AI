package ai.mayra.app.core.planning

import ai.mayra.app.core.runtime.TaskPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPlannerTest {
    @Test
    fun `planner creates one normalized step for a simple goal`() {
        val plan = TaskPlanner().plan("  open   settings  ")

        assertEquals("open settings", plan.goal)
        assertEquals(1, plan.steps.size)
        assertEquals("open settings", plan.steps.single().command)
        assertTrue(plan.steps.single().dependsOn.isEmpty())
    }

    @Test
    fun `planner creates sequential dependencies for multi step goal`() {
        val plan = TaskPlanner().plan("open settings, then enable bluetooth; then return home")

        assertEquals(3, plan.steps.size)
        assertTrue(plan.steps[0].dependsOn.isEmpty())
        assertEquals(setOf(plan.steps[0].id), plan.steps[1].dependsOn)
        assertEquals(setOf(plan.steps[1].id), plan.steps[2].dependsOn)
    }

    @Test
    fun `ready steps respect dependencies and priority`() {
        val first = PlannedStep(id = "first", title = "First", command = "first")
        val normal = PlannedStep(
            id = "normal",
            title = "Normal",
            command = "normal",
            dependsOn = setOf("first")
        )
        val critical = PlannedStep(
            id = "critical",
            title = "Critical",
            command = "critical",
            priority = TaskPriority.CRITICAL,
            dependsOn = setOf("first")
        )
        val plan = ExecutionPlan("test", listOf(first, normal, critical))

        assertEquals(listOf(first), plan.readySteps(emptySet()))
        assertEquals(listOf(critical, normal), plan.readySteps(setOf("first")))
    }

    @Test
    fun `executor retries a transient failure and completes dependents`() = runTest {
        val first = PlannedStep(
            id = "first",
            title = "First",
            command = "first",
            maxAttempts = 2
        )
        val second = PlannedStep(
            id = "second",
            title = "Second",
            command = "second",
            dependsOn = setOf("first")
        )
        var firstAttempts = 0
        val executor = PlanExecutor { step ->
            if (step.id == "first" && firstAttempts++ == 0) error("temporary")
            "done:${step.command}"
        }

        val report = executor.execute(ExecutionPlan("retry test", listOf(first, second)))

        assertTrue(report.isSuccessful)
        assertEquals(2, report.completedSteps)
        assertEquals(2, (report.results[0] as StepExecutionResult.Completed).attempts)
        assertEquals("done:second", (report.results[1] as StepExecutionResult.Completed).output)
    }

    @Test
    fun `executor blocks dependent steps after permanent failure`() = runTest {
        val first = PlannedStep(
            id = "first",
            title = "First",
            command = "first",
            maxAttempts = 2
        )
        val second = PlannedStep(
            id = "second",
            title = "Second",
            command = "second",
            dependsOn = setOf("first")
        )
        val executor = PlanExecutor { error("permanent") }

        val report = executor.execute(ExecutionPlan("failure test", listOf(first, second)))

        assertFalse(report.isSuccessful)
        assertEquals(1, report.failedSteps)
        assertEquals(1, report.blockedSteps)
        assertEquals(2, (report.results[0] as StepExecutionResult.Failed).attempts)
        assertEquals(setOf("first"), (report.results[1] as StepExecutionResult.Blocked).unmetDependencies)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `plan rejects dependency cycles`() {
        ExecutionPlan(
            goal = "cycle",
            steps = listOf(
                PlannedStep(id = "a", title = "A", command = "a", dependsOn = setOf("b")),
                PlannedStep(id = "b", title = "B", command = "b", dependsOn = setOf("a"))
            )
        )
    }
}
