package ai.mayra.app.agents

/**
 * Unit of work exchanged between Mayra agents.
 */
data class AgentTask(
    val id: String,
    val targetAgentId: String,
    val goal: String,
    val payload: Map<String, Any?> = emptyMap(),
    val priority: AgentPriority = AgentPriority.NORMAL,
    val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    val correlationId: String = id,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    init {
        require(id.isNotBlank()) { "Agent task id cannot be blank" }
        require(targetAgentId.isNotBlank()) { "Target agent id cannot be blank" }
        require(goal.isNotBlank()) { "Agent task goal cannot be blank" }
        require(timeoutMillis > 0L) { "Timeout must be greater than zero" }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS: Long = 15_000L
    }
}

enum class AgentPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

sealed interface AgentResult {
    val taskId: String

    data class Success(
        override val taskId: String,
        val message: String,
        val data: Map<String, Any?> = emptyMap(),
        val nextTask: AgentTask? = null
    ) : AgentResult

    data class Failure(
        override val taskId: String,
        val message: String,
        val cause: Throwable? = null,
        val retryable: Boolean = false
    ) : AgentResult

    data class Unsupported(
        override val taskId: String,
        val message: String
    ) : AgentResult

    data class TimedOut(
        override val taskId: String,
        val timeoutMillis: Long
    ) : AgentResult
}

interface MayraAgent {
    val id: String

    suspend fun handle(task: AgentTask, context: AgentContext): AgentResult
}
