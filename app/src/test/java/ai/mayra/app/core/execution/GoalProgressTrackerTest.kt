package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.PlannedStep
import ai.mayra.app.core.planning.StepAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressTrackerTest {
    @Test
    fun `tracker exposes deterministic progress through completed execution`() = runTest {
        val tracker = GoalProgressTracker()
        val engine = GoalExecutionEngine(
            action = StepAction { "done:${it.command}" },
            idFactory = { "goal-progress" },
            eventListener = tracker
        )

        val queued = engine.submit("open settings then enable bluetooth")
        assertEquals(GoalState.QUEUED, tracker.get(queued.id)?.state)
        assertEquals(1, tracker.get(queued.id)?.queuePosition)

        val finished = engine.runNext()
        val snapshot = tracker.get(queued.id)

        assertEquals(GoalState.COMPLETED, finished?.state)
        assertEquals(GoalState.COMPLETED, snapshot?.state)
        assertEquals(2, snapshot?.completedSteps)
        assertEquals(2, snapshot?.totalSteps)
        assertEquals(100, snapshot?.percentComplete)
        assertNull(snapshot?.currentStepId)
        assertNull(snapshot?.queuePosition)
        assertNull(snapshot?.failureMessage)
    }

    @Test
    fun `started progress includes current step metadata`() {
        val tracker = GoalProgressTracker()
        val step = PlannedStep(
            id = "step-1",
            title = "Open camera",
            command = "open camera"
        )

        tracker.onEvent(GoalExecutionEvent.Queued("goal-1", "open camera", 1))
        tracker.onEvent(GoalExecutionEvent.Started("goal-1", "open camera"))
        tracker.onEvent(GoalExecutionEvent.Planned("goal-1", 4))
        tracker.onEvent(
            GoalExecutionEvent.Progress(
                "goal-1",
                StepProgressEvent.Started(step, completedSteps = 1, totalSteps = 4)
            )
        )

        val snapshot = tracker.get("goal-1")
        assertEquals(GoalState.RUNNING, snapshot?.state)
        assertEquals(25, snapshot?.percentComplete)
        assertEquals("step-1", snapshot?.currentStepId)
        assertEquals("Open camera", snapshot?.currentStepTitle)
        assertEquals("open camera", snapshot?.currentCommand)
    }

    @Test
    fun `cancelled progress remains cancelled after finished event`() {
        val tracker = GoalProgressTracker()
        val step = PlannedStep(title = "Wait", command = "wait")

        tracker.onEvent(GoalExecutionEvent.Queued("goal-cancel", "wait", 1))
        tracker.onEvent(GoalExecutionEvent.Started("goal-cancel", "wait"))
        tracker.onEvent(GoalExecutionEvent.Planned("goal-cancel", 2))
        tracker.onEvent(
            GoalExecutionEvent.Progress(
                "goal-cancel",
                StepProgressEvent.Cancelled(step, completedSteps = 1, totalSteps = 2)
            )
        )
        tracker.onEvent(GoalExecutionEvent.Cancelled("goal-cancel"))
        tracker.onEvent(
            GoalExecutionEvent.Finished(
                sessionId = "goal-cancel",
                state = GoalState.CANCELLED,
                completedSteps = 1,
                totalSteps = 2,
                failureMessage = null
            )
        )

        val snapshot = tracker.get("goal-cancel")
        assertEquals(GoalState.CANCELLED, snapshot?.state)
        assertEquals(50, snapshot?.percentComplete)
        assertNull(snapshot?.currentStepId)
        assertNull(snapshot?.failureMessage)
    }

    @Test
    fun `terminal snapshots can be cleared without removing active goals`() {
        val tracker = GoalProgressTracker()
        tracker.onEvent(GoalExecutionEvent.Queued("active", "active goal", 1))
        tracker.onEvent(GoalExecutionEvent.Queued("done", "done goal", 2))
        tracker.onEvent(GoalExecutionEvent.Started("done", "done goal"))
        tracker.onEvent(GoalExecutionEvent.Planned("done", 1))
        tracker.onEvent(
            GoalExecutionEvent.Finished(
                sessionId = "done",
                state = GoalState.COMPLETED,
                completedSteps = 1,
                totalSteps = 1,
                failureMessage = null
            )
        )

        assertEquals(1, tracker.clearTerminal())
        assertNull(tracker.get("done"))
        assertTrue(tracker.get("active") != null)
        assertEquals(1, tracker.snapshot().size)
    }
}
