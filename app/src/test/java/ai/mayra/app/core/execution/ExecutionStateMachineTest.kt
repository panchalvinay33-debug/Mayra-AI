package ai.mayra.app.core.execution

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionStateMachineTest {
    @Test
    fun `valid lifecycle reaches completion and records transitions`() {
        val times = ArrayDeque(
            listOf(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:01Z"),
                Instant.parse("2026-01-01T00:00:02Z")
            )
        )
        val machine = ExecutionStateMachine(now = { times.removeFirst() })

        machine.transitionTo(ExecutionState.PLANNING)
        machine.transitionTo(ExecutionState.EXECUTING)
        machine.transitionTo(ExecutionState.COMPLETED, "goal satisfied")

        assertEquals(ExecutionState.COMPLETED, machine.state())
        assertTrue(machine.state().isTerminal)
        assertEquals(3, machine.historySnapshot().size)
        assertEquals("goal satisfied", machine.historySnapshot().last().reason)
    }

    @Test
    fun `retry path returns to execution`() {
        val machine = ExecutionStateMachine()
        machine.transitionTo(ExecutionState.PLANNING)
        machine.transitionTo(ExecutionState.EXECUTING)
        machine.transitionTo(ExecutionState.RETRYING, "temporary failure")

        assertTrue(machine.canTransitionTo(ExecutionState.EXECUTING))
        machine.transitionTo(ExecutionState.EXECUTING)
        machine.transitionTo(ExecutionState.COMPLETED)

        assertEquals(ExecutionState.COMPLETED, machine.state())
    }

    @Test
    fun `invalid transition leaves state unchanged`() {
        val machine = ExecutionStateMachine()

        assertFailsWith<InvalidExecutionTransitionException> {
            machine.transitionTo(ExecutionState.COMPLETED)
        }

        assertEquals(ExecutionState.PENDING, machine.state())
        assertTrue(machine.historySnapshot().isEmpty())
    }

    @Test
    fun `terminal states reject all further transitions`() {
        val machine = ExecutionStateMachine()
        machine.transitionTo(ExecutionState.CANCELLED)

        assertFalse(machine.canTransitionTo(ExecutionState.PLANNING))
        assertFailsWith<InvalidExecutionTransitionException> {
            machine.transitionTo(ExecutionState.FAILED)
        }
    }

    @Test
    fun `history is bounded to configured size`() {
        val machine = ExecutionStateMachine(maxHistory = 2)
        machine.transitionTo(ExecutionState.PLANNING)
        machine.transitionTo(ExecutionState.EXECUTING)
        machine.transitionTo(ExecutionState.RETRYING)

        val history = machine.historySnapshot()
        assertEquals(2, history.size)
        assertEquals(ExecutionState.EXECUTING, history.first().from)
        assertEquals(ExecutionState.RETRYING, history.last().to)
    }
}
