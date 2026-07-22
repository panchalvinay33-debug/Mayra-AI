package ai.mayra.app.core.execution

/**
 * Storage boundary for queued goals.
 *
 * Android implementations can back this with Room, DataStore, or an encrypted file while the
 * execution engine remains framework-independent and easy to test.
 */
interface GoalQueueStore {
    fun load(): List<PersistedGoal>
    fun save(goals: List<PersistedGoal>)
}

/** Thread-safe store useful for tests, previews, and process-lifetime persistence. */
class InMemoryGoalQueueStore(
    initialGoals: Iterable<PersistedGoal> = emptyList()
) : GoalQueueStore {
    private var goals = initialGoals.toList()

    @Synchronized
    override fun load(): List<PersistedGoal> = goals.toList()

    @Synchronized
    override fun save(goals: List<PersistedGoal>) {
        this.goals = goals.toList()
    }
}

/**
 * Keeps [GoalExecutionEngine]'s pending queue synchronized with a durable [GoalQueueStore].
 *
 * Queue mutations are persisted only after the engine operation succeeds, preventing invalid input
 * or failed cancellation attempts from overwriting the last known-good queue.
 */
class PersistentGoalQueue(
    private val engine: GoalExecutionEngine,
    private val store: GoalQueueStore
) {
    @Synchronized
    fun restore(): Int {
        val restored = engine.restoreQueue(store.load())
        persist()
        return restored
    }

    @Synchronized
    fun submit(goal: String): GoalSession {
        val session = engine.submit(goal)
        persist()
        return session
    }

    @Synchronized
    fun cancel(sessionId: String): Boolean {
        val cancelled = engine.cancel(sessionId)
        if (cancelled) persist()
        return cancelled
    }

    suspend fun runNext(): GoalSession? {
        val result = engine.runNext()
        persist()
        return result
    }

    suspend fun runUntilIdle(): List<GoalSession> {
        val results = engine.runUntilIdle()
        persist()
        return results
    }

    @Synchronized
    fun pending(): List<PersistedGoal> = engine.exportQueue()

    @Synchronized
    fun persist() {
        store.save(engine.exportQueue())
    }
}
