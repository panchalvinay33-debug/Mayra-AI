package ai.mayra.app.agent

/** Keeps older test fixtures source-compatible with the explicit executor constructor. */
internal fun FunctionalAgentTool(
    descriptor: AgentToolDescriptor,
    executor: suspend (AgentToolCall, AgentExecutionContext) -> AgentToolResult
): FunctionalAgentTool = FunctionalAgentTool(
    descriptor = descriptor,
    executor = executor,
    compensator = { _, _, _ -> AgentToolResult.NotSupported("Compensation is not configured") }
)
