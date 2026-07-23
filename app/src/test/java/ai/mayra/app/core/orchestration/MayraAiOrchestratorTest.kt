package ai.mayra.app.core.orchestration

import ai.mayra.app.core.memory.MemoryKind
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraAiOrchestratorTest {
    @Test
    fun `blank input is rejected without changing context`() = runTest {
        val orchestrator = MayraAiOrchestrator(clock = { 100L })

        val result = orchestrator.processText("   ")

        assertTrue(result is OrchestrationResult.Rejected)
        assertTrue(orchestrator.contextSnapshot().turns.isEmpty())
    }

    @Test
    fun `text turn is normalized and stored as user assistant exchange`() = runTest {
        var now = 100L
        val orchestrator = MayraAiOrchestrator(clock = { now++ })

        val result = orchestrator.processText("  hello   ") as OrchestrationResult.Completed
        val context = orchestrator.contextSnapshot()

        assertEquals("hello", result.input)
        assertEquals(2, context.turns.size)
        assertEquals("hello", context.turns.first().text)
        assertEquals(result.response, context.turns.last().text)
    }

    @Test
    fun `relevant durable memories are returned with assistant result`() = runTest {
        val orchestrator = MayraAiOrchestrator(clock = { 100L })
        orchestrator.remember(
            namespace = "preferences",
            key = "reply_language",
            value = "Hindi replies",
            kind = MemoryKind.PREFERENCE
        )

        val result = orchestrator.processText("reply in Hindi") as OrchestrationResult.Completed

        assertEquals("preferences:reply_language", result.recalledMemories.single().record.id)
    }

    @Test
    fun `multi step goal executes sequentially and records outcome memory`() = runTest {
        var now = 100L
        val orchestrator = MayraAiOrchestrator(clock = { now++ })

        val result = orchestrator.processGoal("hello, then what is the date")
            as GoalOrchestrationResult.Completed

        assertTrue(result.report.isSuccessful)
        assertEquals(2, result.report.completedSteps)
        val goalMemory = orchestrator.memorySnapshot().records.single {
            it.namespace == "goal_history"
        }
        assertTrue(goalMemory.value.contains("status=completed"))
        assertTrue(goalMemory.value.contains("completed=2/2"))
    }

    @Test
    fun `remember exposes normalized long term memory`() {
        val orchestrator = MayraAiOrchestrator(clock = { 123L })

        val stored = orchestrator.remember(" User Profile ", " Home City ", " Pitol ")

        assertEquals("user_profile:home_city", stored.id)
        assertEquals(123L, stored.createdAt)
    }
}
