package ai.mayra.app.core.execution

/**
 * Thread-safe fan-out listener for execution events.
 *
 * Android UI, notifications, persistence, analytics, and diagnostics can subscribe independently
 * without coupling [GoalExecutionEngine] to any framework component.
 */
class GoalExecutionEventHub(
    private val onListenerFailure: (listener: GoalExecutionEventListener, error: Throwable) -> Unit = { _, _ -> }
) : GoalExecutionEventListener {
    private val listeners = linkedSetOf<GoalExecutionEventListener>()

    /** Adds [listener] and returns a handle that removes it exactly once. */
    fun subscribe(listener: GoalExecutionEventListener): AutoCloseable {
        synchronized(listeners) {
            listeners += listener
        }
        var closed = false
        return AutoCloseable {
            synchronized(listeners) {
                if (!closed) {
                    closed = true
                    listeners -= listener
                }
            }
        }
    }

    fun unsubscribe(listener: GoalExecutionEventListener): Boolean = synchronized(listeners) {
        listeners.remove(listener)
    }

    fun listenerCount(): Int = synchronized(listeners) { listeners.size }

    override fun onEvent(event: GoalExecutionEvent) {
        val currentListeners = synchronized(listeners) { listeners.toList() }
        currentListeners.forEach { listener ->
            try {
                listener.onEvent(event)
            } catch (error: Throwable) {
                onListenerFailure(listener, error)
            }
        }
    }
}
