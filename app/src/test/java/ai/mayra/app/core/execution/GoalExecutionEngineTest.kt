package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.StepAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalExecutionEngineTest {
    @Test
    fun `submitted goal is normalized queued and then completed`() = runTest {
        var now = 100L
        val executed = mutableListOf<String>()
        val engine = GoalExecutionEngine(
            action = StepAction { step ->
                executed += step.command
                "done:${step.command}"
            },
            clock = { now++ },
            idFactory = { "goal-1" }
        )

        val queued = engine.submit("  open settings,   then enable bluetooth ")
        val completed = engine.runNext()

        assertEquals("open settings, then enable bluetooth", queued.goal)
        assertEquals(GoalState.QUEUED, queued.state)
        assertEquals(GoalState.COMPLETED, completed?.state)
        assertEquals(listOf("open settings", "enable bluetooth"), executed)
        assertTrue(completed?.report?.isSuccessful == true)
        assertNotNull(completed?.startedAt)
        assertNotNull(completed?.finishedAt)
        assertNull(engine.runNext())
    }

    @Test
    fun `failed step marks goal failed and blocks dependent steps`() = runTest {
        val engine = GoalExecutionEngine(
            action = StepAction { step ->
                if (step.command == "first") error("device unavailable")
                "done"
            },
            idFactory = { "goal-failure" }
        )

        engine.submit("first then second")
        val result = engine.runNext()

        assertEquals(GoalState.FAILED, result?.state)
        assertEquals("device unavailable", result?.failureMessage)
        assertEquals(1, result?.report?.failedSteps)
        assertEquals(1, result?.report?.blockedSteps)
        assertFalse(result?.report?.isSuccessful ?: true)
    }

    @Test
    fun `queued goal can be cancelled without being executed`() = runTest {
        var executions = 0
        val engine = GoalExecutionEngine(
            action = StepAction {
                executions += 1
                "done"
            },
            idFactory = { "goal-cancel" }
        )
        val session = engine.submit("open camera")

        assertTrue(engine.cancel(session.id))
        assertFalse(engine.cancel(session.id))
        assertEquals(GoalState.CANCELLED, engine.get(session.id)?.state)
        assertNull(engine.runNext())
        assertEquals(0, executions)
    }

    @Test
    fun `run until idle preserves serial queue order`() = runTest {
        var nextId = 0
        val commands = mutableListOf<String>()
        val engine = GoalExecutionEngine(
            action = StepAction { step ->
                commands += step.command
                "done"
            },
            idFactory = { "goal-${++nextId}" }
        )

        engine.submit("one")
        engine.submit("two")
        engine.submit("three")
        val results = engine.runUntilIdle()

        assertEquals(listOf("one", "two", "three"), commands)
        assertEquals(3, results.size)
        assertTrue(results.all { it.state == GoalState.COMPLETED })
        assertEquals(3, engine.snapshot().completedCount)
        assertEquals(0, engine.snapshot().queuedCount)
    }

    @Test
    fun `history limit removes oldest terminal sessions`() = runTest {
        var nextId = 0
        val engine = GoalExecutionEngine(
            action = StepAction { "done" },
            idFactory = { "goal-${++nextId}" },
            historyLimit = 2
        )

        val first = engine.submit("one")
        engine.runNext()
        engine.submit("two")
        engine.runNext()
        engine.submit("three")
        engine.runNext()

        assertNull(engine.get(first.id))
        assertEquals(listOf("goal-2", "goal-3"), engine.snapshot().sessions.map { it.id })
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank goal is rejected`() {
        GoalExecutionEngine(action = StepAction { "done" }).submit("   ")
    }
}
