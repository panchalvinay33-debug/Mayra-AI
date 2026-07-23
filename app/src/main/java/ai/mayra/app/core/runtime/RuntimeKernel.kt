package ai.mayra.app.core.runtime

import java.util.PriorityQueue
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class TaskPriority(val weight: Int) {
    LOW(0),
    NORMAL(1),
    HIGH(2),
    CRITICAL(3)
}

data class RuntimeTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val createdAt: Long = System.currentTimeMillis(),
    val block: suspend () -> Unit
)

data class RuntimeSnapshot(
    val queuedTasks: Int,
    val completedTasks: Long,
    val failedTasks: Long,
    val isRunning: Boolean
)

sealed interface RuntimeEvent {
    data class TaskQueued(val taskId: String, val name: String) : RuntimeEvent
    data class TaskStarted(val taskId: String, val name: String) : RuntimeEvent
    data class TaskCompleted(val taskId: String, val name: String) : RuntimeEvent
    data class TaskFailed(val taskId: String, val name: String, val error: Throwable) : RuntimeEvent
    data class TaskCancelled(val taskId: String, val name: String) : RuntimeEvent
}

fun interface RuntimeEventListener {
    fun onEvent(event: RuntimeEvent)
}

class RuntimeKernel(
    private val eventListener: RuntimeEventListener = RuntimeEventListener { }
) {
    private val mutex = Mutex()
    private val queue = PriorityQueue<RuntimeTask>(
        compareByDescending<RuntimeTask> { it.priority.weight }
            .thenBy { it.createdAt }
            .thenBy { it.id }
    )

    private var running = false
    private var completed = 0L
    private var failed = 0L

    suspend fun submit(task: RuntimeTask): String {
        require(task.name.isNotBlank()) { "Task name cannot be blank." }
        mutex.withLock { queue.add(task) }
        eventListener.onEvent(RuntimeEvent.TaskQueued(task.id, task.name))
        return task.id
    }

    suspend fun cancel(taskId: String): Boolean {
        val removed = mutex.withLock {
            val task = queue.firstOrNull { it.id == taskId } ?: return@withLock null
            queue.remove(task)
            task
        } ?: return false

        eventListener.onEvent(RuntimeEvent.TaskCancelled(removed.id, removed.name))
        return true
    }

    suspend fun runNext(): Boolean {
        val task = mutex.withLock {
            if (running) return@withLock null
            queue.poll()?.also { running = true }
        } ?: return false

        eventListener.onEvent(RuntimeEvent.TaskStarted(task.id, task.name))
        try {
            task.block()
            completed += 1
            eventListener.onEvent(RuntimeEvent.TaskCompleted(task.id, task.name))
        } catch (error: CancellationException) {
            eventListener.onEvent(RuntimeEvent.TaskCancelled(task.id, task.name))
            throw error
        } catch (error: Throwable) {
            failed += 1
            eventListener.onEvent(RuntimeEvent.TaskFailed(task.id, task.name, error))
        } finally {
            mutex.withLock { running = false }
        }
        return true
    }

    suspend fun drain(): Int {
        var executed = 0
        while (runNext()) executed += 1
        return executed
    }

    suspend fun snapshot(): RuntimeSnapshot = mutex.withLock {
        RuntimeSnapshot(
            queuedTasks = queue.size,
            completedTasks = completed,
            failedTasks = failed,
            isRunning = running
        )
    }
}
