package ai.mayra.app.core.orchestration

import ai.mayra.app.core.runtime.TaskPriority
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraRuntimeQueueTest {
    @Test
    fun `submitted text remains pending until runtime executes it`() = runTest {
        val queue = MayraRuntimeQueue(clock = { 100L })

        val ticket = queue.submitText("  hello   Mayra  ")

        assertEquals(QueuedOrchestrationResult.Pending, queue.result(ticket.taskId))
        assertEquals(1, queue.runtimeSnapshot().queuedTasks)

        assertTrue(queue.runNext())
        val result = queue.result(ticket.taskId) as QueuedOrchestrationResult.Text
        assertTrue(result.result is OrchestrationResult.Completed)
        assertEquals(0, queue.runtimeSnapshot().queuedTasks)
        assertEquals(1L, queue.runtimeSnapshot().completedTasks)
    }

    @Test
    fun `higher priority work executes before earlier normal work`() = runTest {
        var now = 1L
        val queue = MayraRuntimeQueue(clock = { now++ })

        val normal = queue.submitText("hello", TaskPriority.NORMAL)
        val critical = queue.submitText("what time is it", TaskPriority.CRITICAL)

        assertTrue(queue.runNext())
        assertEquals(QueuedOrchestrationResult.Pending, queue.result(normal.taskId))
        assertTrue(queue.result(critical.taskId) is QueuedOrchestrationResult.Text)

        assertTrue(queue.runNext())
        assertTrue(queue.result(normal.taskId) is QueuedOrchestrationResult.Text)
    }

    @Test
    fun `queued tasks keep independent result correlation`() = runTest {
        var now = 10L
        val queue = MayraRuntimeQueue(clock = { now++ })

        val first = queue.submitText("hello")
        val second = queue.submitText("what date is today")

        assertNotEquals(first.taskId, second.taskId)
        assertEquals(2, queue.drain())

        val firstResult = queue.result(first.taskId) as QueuedOrchestrationResult.Text
        val secondResult = queue.result(second.taskId) as QueuedOrchestrationResult.Text
        val firstResponse = (firstResult.result as OrchestrationResult.Completed).response
        val secondResponse = (secondResult.result as OrchestrationResult.Completed).response
        assertNotEquals(firstResponse, secondResponse)
    }

    @Test
    fun `cancel removes queued work and records cancellation`() = runTest {
        val queue = MayraRuntimeQueue(clock = { 50L })
        val ticket = queue.submitGoal("hello then what time is it")

        assertTrue(queue.cancel(ticket.taskId))
        assertEquals(QueuedOrchestrationResult.Cancelled, queue.result(ticket.taskId))
        assertEquals(0, queue.runtimeSnapshot().queuedTasks)
        assertFalse(queue.runNext())
    }

    @Test
    fun `goal work executes through planner and stores typed result`() = runTest {
        val queue = MayraRuntimeQueue(clock = { 80L })
        val ticket = queue.submitGoal("hello then what time is it", TaskPriority.HIGH)

        assertEquals(1, queue.drain())
        val result = queue.result(ticket.taskId) as QueuedOrchestrationResult.Goal
        assertTrue(result.result is GoalOrchestrationResult.Completed)
    }

    @Test
    fun `finished results can be cleared without removing pending work`() = runTest {
        var now = 1L
        val queue = MayraRuntimeQueue(clock = { now++ })
        val finished = queue.submitText("hello", TaskPriority.CRITICAL)
        val pending = queue.submitText("help", TaskPriority.NORMAL)

        assertTrue(queue.runNext())
        assertEquals(1, queue.clearFinishedResults())
        assertEquals(null, queue.result(finished.taskId))
        assertEquals(QueuedOrchestrationResult.Pending, queue.result(pending.taskId))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank text submission is rejected before queue mutation`() = runTest {
        MayraRuntimeQueue().submitText("   ")
    }
}
