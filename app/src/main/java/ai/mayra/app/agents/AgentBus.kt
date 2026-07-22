package ai.mayra.app.agents

import kotlinx.coroutines.withTimeoutOrNull

class AgentBus(
    private val registry: AgentRegistry
) {
    suspend fun dispatch(
        task: AgentTask,
        context: AgentContext = AgentContext(),
        retryPolicy: RetryPolicy = RetryPolicy.None
    ): AgentResult {
        val agent = registry.find(task.targetAgentId)
            ?: return AgentResult.Unsupported(
                taskId = task.id,
                message = "No agent registered for id: ${task.targetAgentId}"
            )

        var lastFailure: AgentResult.Failure? = null

        for (attempt in 1..retryPolicy.maxAttempts) {
            val result = runCatching {
                withTimeoutOrNull(task.timeoutMillis) {
                    agent.handle(task, context)
                }
            }.fold(
                onSuccess = { value ->
                    value ?: AgentResult.TimedOut(task.id, task.timeoutMillis)
                },
                onFailure = { error ->
                    AgentResult.Failure(
                        taskId = task.id,
                        message = error.message ?: "Agent execution failed",
                        cause = error,
                        retryable = true
                    )
                }
            )

            when (result) {
                is AgentResult.Success,
                is AgentResult.Unsupported -> return result

                is AgentResult.TimedOut -> {
                    if (attempt == retryPolicy.maxAttempts) return result
                }

                is AgentResult.Failure -> {
                    lastFailure = result
                    if (!result.retryable || attempt == retryPolicy.maxAttempts) return result
                }
            }

            retryPolicy.waitBeforeRetry(attempt)
        }

        return lastFailure ?: AgentResult.Failure(
            taskId = task.id,
            message = "Agent execution ended without a result"
        )
    }
}
