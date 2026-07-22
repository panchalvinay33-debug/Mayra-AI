package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.StepAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalExecutionProgressTest {
    @Test
    fun `execution emits deterministic lifecycle events`() = runTest {
        val events = mutableListOf<GoalExecutionEvent>()
        val engine = GoalExecutionEngine(
            action = StepAction { step -> "done:${step.command}" },
            idFactory = { "goal-events" },
            eventListener = GoalExecutionEventListener(events::add)
        )

        engine.submit("open settings then enable bluetooth")
        val result = engine.runNext()

        assertEquals(GoalState.COMPLETED, result?.state)
        assertEquals(
            listOf(
                GoalExecutionEvent.Queued::class,
                GoalExecutionEvent.Started::class,
                GoalExecutionEvent.Planned::class,
                GoalExecutionEvent.Finished::class
            ),
            events.map { it::class }
        )
        assertEquals(2, (events[2] as GoalExecutionEvent.Planned).totalSteps)
        val finished = events.last() as GoalExecutionEvent.Finished
        assertEquals(2, finished.completedSteps)
        assertEquals(2, finished.totalSteps)
        assertEquals(GoalState.COMPLETED, finished.state)
    }

    @Test
    fun `queued goals can be exported restored and executed in order`() = runTest {
        var sourceId = 0
        val source = GoalExecutionEngine(
            action = StepAction { "unused" },
            clock = { 100L + sourceId },
            idFactory = { "persisted-${++sourceId}" }
        )
        source.submit("first action")
        source.submit("second action")

        val persisted = source.exportQueue()
        val restoredEvents = mutableListOf<GoalExecutionEvent>()
        val commands = mutableListOf<String>()
        val restored = GoalExecutionEngine(
            action = StepAction { step ->
                commands += step.command
                "done"
            },
            eventListener = GoalExecutionEventListener(restoredEvents::add)
        )

        assertEquals(2, restored.restoreQueue(persisted))
        assertEquals(0, restored.restoreQueue(persisted))
        val results = restored.runUntilIdle()

        assertEquals(listOf("first action", "second action"), commands)
        assertTrue(results.all { it.state == GoalState.COMPLETED })
        assertEquals(2, restoredEvents.count { it is GoalExecutionEvent.Restored })
        assertTrue(restored.exportQueue().isEmpty())
    }

    @Test
    fun `cancelling queued goal emits cancellation event`() {
        val events = mutableListOf<GoalExecutionEvent>()
        val engine = GoalExecutionEngine(
            action = StepAction { "done" },
            idFactory = { "cancel-me" },
            eventListener = GoalExecutionEventListener(events::add)
        )

        val session = engine.submit("open camera")
        assertTrue(engine.cancel(session.id))

        assertTrue(events.last() is GoalExecutionEvent.Cancelled)
        assertEquals("cancel-me", events.last().sessionId)
    }
}
