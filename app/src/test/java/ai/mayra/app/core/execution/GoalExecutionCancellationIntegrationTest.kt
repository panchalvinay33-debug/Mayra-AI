package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.StepAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalExecutionCancellationIntegrationTest {
    @Test
    fun `running goal cancellation stops dependent steps and finishes cancelled`() = runTest {
        val events = mutableListOf<GoalExecutionEvent>()
        val executed = mutableListOf<String>()
        lateinit var engine: GoalExecutionEngine
        var sessionId = ""

        engine = GoalExecutionEngine(
            action = StepAction { step ->
                executed += step.command
                if (step.command == "first") {
                    assertTrue(engine.cancel(sessionId))
                }
                "done:${step.command}"
            },
            idFactory = { "goal-running-cancel" },
            eventListener = GoalExecutionEventListener(events::add)
        )
        sessionId = engine.submit("first then second").id

        val result = engine.runNext()

        assertEquals(GoalState.CANCELLED, result?.state)
        assertNull(result?.failureMessage)
        assertEquals(listOf("first"), executed)
        assertEquals(1, result?.report?.failedSteps)
        assertEquals(1, result?.report?.blockedSteps)
        assertTrue(events.any { it is GoalExecutionEvent.Progress })
        assertTrue(events.any { it is GoalExecutionEvent.Cancelled })
        assertEquals(
            GoalState.CANCELLED,
            events.filterIsInstance<GoalExecutionEvent.Finished>().single().state
        )
        assertFalse(engine.cancel(sessionId))
    }

    @Test
    fun `completed goal emits ordered step progress`() = runTest {
        val progress = mutableListOf<StepProgressEvent>()
        val engine = GoalExecutionEngine(
            action = StepAction { step -> "done:${step.command}" },
            idFactory = { "goal-progress" },
            eventListener = GoalExecutionEventListener { event ->
                if (event is GoalExecutionEvent.Progress) progress += event.event
            }
        )

        engine.submit("one then two")
        val result = engine.runNext()

        assertEquals(GoalState.COMPLETED, result?.state)
        assertEquals(
            listOf(
                StepProgressEvent.Started::class,
                StepProgressEvent.Completed::class,
                StepProgressEvent.Started::class,
                StepProgressEvent.Completed::class
            ),
            progress.map { it::class }
        )
        assertEquals(
            listOf(0, 1, 1, 2),
            progress.map {
                when (it) {
                    is StepProgressEvent.Started -> it.completedSteps
                    is StepProgressEvent.Completed -> it.completedSteps
                    is StepProgressEvent.Failed -> it.completedSteps
                    is StepProgressEvent.Cancelled -> it.completedSteps
                }
            }
        )
        assertTrue(progress.all {
            when (it) {
                is StepProgressEvent.Started -> it.totalSteps == 2
                is StepProgressEvent.Completed -> it.totalSteps == 2
                is StepProgressEvent.Failed -> it.totalSteps == 2
                is StepProgressEvent.Cancelled -> it.totalSteps == 2
            }
        })
    }
}
