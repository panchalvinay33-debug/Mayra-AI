package ai.mayra.app.core.orchestration

import ai.mayra.app.core.execution.GoalState
import ai.mayra.app.core.memory.LongTermMemoryEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraAiGoalOrchestrationTest {
    @Test
    fun `multi step goal executes through planner and stores outcome in memory`() = runTest {
        var now = 1_000L
        val memory = LongTermMemoryEngine()
        val orchestrator = MayraAiOrchestrator(
            memoryEngine = memory,
            clock = { now++ }
        )

        val result = orchestrator.processGoal("hello then help")

        assertTrue(result is GoalOrchestrationResult.Completed)
        val completed = result as GoalOrchestrationResult.Completed
        assertEquals(GoalState.COMPLETED, completed.session.state)
        assertEquals(2, completed.session.plan?.steps?.size)
        assertEquals(2, completed.session.report?.completedSteps)

        val snapshot = orchestrator.memorySnapshot()
        assertEquals(1, snapshot.totalCount)
        assertEquals("goal_history", snapshot.records.single().namespace)
        assertTrue(snapshot.records.single().value.contains("status=completed"))
        assertTrue(snapshot.records.single().value.contains("completed=2/2"))
    }

    @Test
    fun `blank goal is rejected without writing memory`() = runTest {
        val orchestrator = MayraAiOrchestrator()

        val result = orchestrator.processGoal("   ")

        assertTrue(result is GoalOrchestrationResult.Rejected)
        assertEquals(0, orchestrator.memorySnapshot().totalCount)
    }
}
