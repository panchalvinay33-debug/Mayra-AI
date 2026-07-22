package ai.mayra.app.core.runtime

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeKernelTest {

    @Test
    fun higherPriorityTasksRunFirst() = runTest {
        val order = mutableListOf<String>()
        val kernel = RuntimeKernel()

        kernel.submit(RuntimeTask(name = "low", priority = TaskPriority.LOW) { order += "low" })
        kernel.submit(RuntimeTask(name = "critical", priority = TaskPriority.CRITICAL) { order += "critical" })
        kernel.submit(RuntimeTask(name = "normal", priority = TaskPriority.NORMAL) { order += "normal" })

        assertEquals(3, kernel.drain())
        assertEquals(listOf("critical", "normal", "low"), order)
    }

    @Test
    fun failedTaskDoesNotStopQueueDrain() = runTest {
        val order = mutableListOf<String>()
        val kernel = RuntimeKernel()

        kernel.submit(RuntimeTask(name = "broken") { error("boom") })
        kernel.submit(RuntimeTask(name = "next") { order += "next" })

        assertEquals(2, kernel.drain())
        assertEquals(listOf("next"), order)

        val snapshot = kernel.snapshot()
        assertEquals(1, snapshot.failedTasks)
        assertEquals(1, snapshot.completedTasks)
        assertEquals(0, snapshot.queuedTasks)
        assertFalse(snapshot.isRunning)
    }

    @Test
    fun queuedTaskCanBeCancelled() = runTest {
        var executed = false
        val kernel = RuntimeKernel()
        val id = kernel.submit(RuntimeTask(name = "cancel me") { executed = true })

        assertTrue(kernel.cancel(id))
        assertFalse(kernel.cancel(id))
        assertEquals(0, kernel.drain())
        assertFalse(executed)
    }

    @Test
    fun lifecycleEventsArePublished() = runTest {
        val events = mutableListOf<RuntimeEvent>()
        val kernel = RuntimeKernel(RuntimeEventListener(events::add))

        kernel.submit(RuntimeTask(id = "task-1", name = "sync") { })
        kernel.drain()

        assertEquals(
            listOf(
                RuntimeEvent.TaskQueued("task-1", "sync"),
                RuntimeEvent.TaskStarted("task-1", "sync"),
                RuntimeEvent.TaskCompleted("task-1", "sync")
            ),
            events
        )
    }
}
