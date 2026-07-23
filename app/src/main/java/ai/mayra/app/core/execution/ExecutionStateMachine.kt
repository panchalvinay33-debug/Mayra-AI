package ai.mayra.app.core.execution

import java.time.Instant

enum class ExecutionState {
    PENDING,
    PLANNING,
    WAITING_PERMISSION,
    EXECUTING,
    RETRYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    TIMED_OUT;

    val isTerminal: Boolean
        get() = this in setOf(COMPLETED, FAILED, CANCELLED, TIMED_OUT)
}

data class ExecutionTransition(
    val from: ExecutionState,
    val to: ExecutionState,
    val occurredAt: Instant,
    val reason: String? = null
)

class InvalidExecutionTransitionException(
    from: ExecutionState,
    to: ExecutionState
) : IllegalStateException("Invalid execution transition: $from -> $to")

class ExecutionStateMachine(
    initialState: ExecutionState = ExecutionState.PENDING,
    private val now: () -> Instant = Instant::now,
    private val maxHistory: Int = 100
) {
    private var currentState: ExecutionState = initialState
    private val history = ArrayDeque<ExecutionTransition>()

    init {
        require(maxHistory > 0) { "Maximum history size must be positive." }
    }

    @Synchronized
    fun state(): ExecutionState = currentState

    @Synchronized
    fun canTransitionTo(target: ExecutionState): Boolean = target in allowedTransitions.getValue(currentState)

    @Synchronized
    fun transitionTo(target: ExecutionState, reason: String? = null): ExecutionTransition {
        val source = currentState
        if (target !in allowedTransitions.getValue(source)) {
            throw InvalidExecutionTransitionException(source, target)
        }

        val transition = ExecutionTransition(source, target, now(), reason?.trim()?.takeIf { it.isNotEmpty() })
        currentState = target
        history.addLast(transition)
        while (history.size > maxHistory) history.removeFirst()
        return transition
    }

    @Synchronized
    fun historySnapshot(): List<ExecutionTransition> = history.toList()

    companion object {
        private val terminalTransitions = emptySet<ExecutionState>()
        private val allowedTransitions: Map<ExecutionState, Set<ExecutionState>> = mapOf(
            ExecutionState.PENDING to setOf(
                ExecutionState.PLANNING,
                ExecutionState.CANCELLED,
                ExecutionState.TIMED_OUT,
                ExecutionState.FAILED
            ),
            ExecutionState.PLANNING to setOf(
                ExecutionState.WAITING_PERMISSION,
                ExecutionState.EXECUTING,
                ExecutionState.CANCELLED,
                ExecutionState.TIMED_OUT,
                ExecutionState.FAILED
            ),
            ExecutionState.WAITING_PERMISSION to setOf(
                ExecutionState.EXECUTING,
                ExecutionState.CANCELLED,
                ExecutionState.TIMED_OUT,
                ExecutionState.FAILED
            ),
            ExecutionState.EXECUTING to setOf(
                ExecutionState.RETRYING,
                ExecutionState.COMPLETED,
                ExecutionState.CANCELLED,
                ExecutionState.TIMED_OUT,
                ExecutionState.FAILED
            ),
            ExecutionState.RETRYING to setOf(
                ExecutionState.EXECUTING,
                ExecutionState.CANCELLED,
                ExecutionState.TIMED_OUT,
                ExecutionState.FAILED
            ),
            ExecutionState.COMPLETED to terminalTransitions,
            ExecutionState.FAILED to terminalTransitions,
            ExecutionState.CANCELLED to terminalTransitions,
            ExecutionState.TIMED_OUT to terminalTransitions
        )
    }
}
