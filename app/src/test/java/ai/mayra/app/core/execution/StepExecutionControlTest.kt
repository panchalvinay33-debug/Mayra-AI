package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.PlannedStep
import ai.mayra.app.core.planning.StepAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StepExecutionControlTest {
    @Test
    fun `successful action emits started and completed progress`() = runTest {
        val events = mutableListOf<StepProgressEvent>()
        val step = PlannedStep(title = "Open camera", command = "open camera")
        val action = ControllableStepAction(
            delegate = StepAction { "opened" },
            cancellationToken = ExecutionCancellationToken(),
            totalSteps = 2,
            listener = StepProgressListener(events::add)
        )

        val output = action.execute(step)

        assertEquals("opened", output)
        assertEquals(1, action.completedStepCount())
        assertTrue(events[0] is StepProgressEvent.Started)
        val completed = events[1] as StepProgressEvent.Completed
        assertEquals(1, completed.completedSteps)
        assertEquals(2, completed.totalSteps)
        assertEquals("opened", completed.output)
    }

    @Test
    fun `cancellation before execution prevents delegate call`() = runTest {
        var delegateCalled = false
        val events = mutableListOf<StepProgressEvent>()
        val token = ExecutionCancellationToken()
        token.cancel()
        val action = ControllableStepAction(
            delegate = StepAction {
                delegateCalled = true
                "done"
            },
            cancellationToken = token,
            totalSteps = 1,
            listener = StepProgressListener(events::add)
        )

        val error = runCatching {
            action.execute(PlannedStep(title = "One", command = "one"))
        }.exceptionOrNull()

        assertTrue(error is StepExecutionCancelledException)
        assertFalse(delegateCalled)
        assertEquals(1, events.size)
        assertTrue(events.single() is StepProgressEvent.Cancelled)
    }

    @Test
    fun `cancellation requested by delegate is observed before completion`() = runTest {
        val events = mutableListOf<StepProgressEvent>()
        val token = ExecutionCancellationToken()
        val action = ControllableStepAction(
            delegate = StepAction {
                token.cancel()
                "late-result"
            },
            cancellationToken = token,
            totalSteps = 1,
            listener = StepProgressListener(events::add)
        )

        val error = runCatching {
            action.execute(PlannedStep(title = "Wait", command = "wait"))
        }.exceptionOrNull()

        assertTrue(error is StepExecutionCancelledException)
        assertEquals(0, action.completedStepCount())
        assertTrue(events.first() is StepProgressEvent.Started)
        assertTrue(events.last() is StepProgressEvent.Cancelled)
    }

    @Test
    fun `delegate failure emits failed event without advancing progress`() = runTest {
        val events = mutableListOf<StepProgressEvent>()
        val action = ControllableStepAction(
            delegate = StepAction { error("bluetooth unavailable") },
            cancellationToken = ExecutionCancellationToken(),
            totalSteps = 3,
            listener = StepProgressListener(events::add)
        )

        val error = runCatching {
            action.execute(PlannedStep(title = "Bluetooth", command = "enable bluetooth"))
        }.exceptionOrNull()

        assertEquals("bluetooth unavailable", error?.message)
        assertEquals(0, action.completedStepCount())
        assertTrue(events.first() is StepProgressEvent.Started)
        val failed = events.last() as StepProgressEvent.Failed
        assertEquals(0, failed.completedSteps)
        assertEquals(3, failed.totalSteps)
    }

    @Test
    fun `cancellation token changes state only once`() {
        val token = ExecutionCancellationToken()

        assertFalse(token.isCancellationRequested)
        assertTrue(token.cancel())
        assertFalse(token.cancel())
        assertTrue(token.isCancellationRequested)
    }
}
