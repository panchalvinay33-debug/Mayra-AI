package ai.mayra.app.core.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalExecutionEventHubTest {
    @Test
    fun `hub delivers events to every active listener`() {
        val first = mutableListOf<GoalExecutionEvent>()
        val second = mutableListOf<GoalExecutionEvent>()
        val hub = GoalExecutionEventHub()
        hub.subscribe(GoalExecutionEventListener(first::add))
        hub.subscribe(GoalExecutionEventListener(second::add))

        val event = GoalExecutionEvent.Queued("goal-1", "open camera", 1)
        hub.onEvent(event)

        assertEquals(listOf(event), first)
        assertEquals(listOf(event), second)
        assertEquals(2, hub.listenerCount())
    }

    @Test
    fun `subscription handle removes listener exactly once`() {
        var calls = 0
        val hub = GoalExecutionEventHub()
        val listener = GoalExecutionEventListener { calls += 1 }
        val subscription = hub.subscribe(listener)

        hub.onEvent(GoalExecutionEvent.Cancelled("goal-1"))
        subscription.close()
        subscription.close()
        hub.onEvent(GoalExecutionEvent.Cancelled("goal-1"))

        assertEquals(1, calls)
        assertEquals(0, hub.listenerCount())
        assertFalse(hub.unsubscribe(listener))
    }

    @Test
    fun `failing listener does not block healthy listeners`() {
        val failures = mutableListOf<Throwable>()
        var healthyCalls = 0
        val hub = GoalExecutionEventHub { _, error -> failures += error }
        hub.subscribe(GoalExecutionEventListener { error("broken observer") })
        hub.subscribe(GoalExecutionEventListener { healthyCalls += 1 })

        hub.onEvent(GoalExecutionEvent.Cancelled("goal-2"))

        assertEquals(1, failures.size)
        assertEquals("broken observer", failures.single().message)
        assertEquals(1, healthyCalls)
        assertTrue(hub.listenerCount() == 2)
    }
}
