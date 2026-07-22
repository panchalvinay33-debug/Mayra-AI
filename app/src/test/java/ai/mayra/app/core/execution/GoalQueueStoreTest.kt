package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.StepAction
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalQueueStoreTest {
    @Test
    fun `submit persists normalized pending goal`() {
        val store = InMemoryGoalQueueStore()
        val engine = GoalExecutionEngine(
            action = StepAction { "done" },
            idFactory = { "goal-1" },
            clock = { 42L }
        )
        val queue = PersistentGoalQueue(engine, store)

        queue.submit("  open camera   ")

        assertEquals(
            listOf(PersistedGoal("goal-1", "open camera", 42L)),
            store.load()
        )
    }

    @Test
    fun `restored goals survive a new engine instance and execute in order`() = runTest {
        val store = InMemoryGoalQueueStore(
            listOf(
                PersistedGoal("goal-1", "first", 1L),
                PersistedGoal("goal-2", "second", 2L)
            )
        )
        val executed = mutableListOf<String>()
        val engine = GoalExecutionEngine(
            action = StepAction { step ->
                executed += step.command
                "done"
            }
        )
        val queue = PersistentGoalQueue(engine, store)

        assertEquals(2, queue.restore())
        val results = queue.runUntilIdle()

        assertEquals(listOf("first", "second"), executed)
        assertTrue(results.all { it.state == GoalState.COMPLETED })
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun `successful cancellation removes goal from durable queue`() {
        val store = InMemoryGoalQueueStore()
        val engine = GoalExecutionEngine(
            action = StepAction { "done" },
            idFactory = { "goal-cancel" }
        )
        val queue = PersistentGoalQueue(engine, store)
        val session = queue.submit("open settings")

        assertTrue(queue.cancel(session.id))
        assertTrue(store.load().isEmpty())
        assertEquals(GoalState.CANCELLED, engine.get(session.id)?.state)
    }

    @Test
    fun `failed cancellation leaves durable queue unchanged`() {
        val persisted = PersistedGoal("goal-1", "open settings", 10L)
        val store = InMemoryGoalQueueStore(listOf(persisted))
        val engine = GoalExecutionEngine(action = StepAction { "done" })
        val queue = PersistentGoalQueue(engine, store)

        assertFalse(queue.cancel("missing"))
        assertEquals(listOf(persisted), store.load())
    }

    @Test
    fun `run next clears completed goal but keeps remaining goals persisted`() = runTest {
        var nextId = 0
        val store = InMemoryGoalQueueStore()
        val engine = GoalExecutionEngine(
            action = StepAction { "done" },
            idFactory = { "goal-${++nextId}" }
        )
        val queue = PersistentGoalQueue(engine, store)
        queue.submit("one")
        queue.submit("two")

        val completed = queue.runNext()

        assertEquals(GoalState.COMPLETED, completed?.state)
        assertEquals(listOf("goal-2"), store.load().map { it.id })
        assertNull(queue.runUntilIdle().lastOrNull()?.failureMessage)
        assertTrue(store.load().isEmpty())
    }
}
