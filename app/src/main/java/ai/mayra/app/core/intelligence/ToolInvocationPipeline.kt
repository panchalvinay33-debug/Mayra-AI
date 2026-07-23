package ai.mayra.app.core.intelligence

import java.time.Duration
import java.time.Instant

data class ToolInvocationEvent(
    val toolId: String,
    val status: ToolExecutionStatus,
    val startedAt: Instant,
    val completedAt: Instant,
    val detail: String? = null
) {
    val duration: Duration get() = Duration.between(startedAt, completedAt)
}

class ToolInvocationPipeline(
    private val registry: ToolRegistry,
    private val permissionEngine: ToolPermissionPolicyEngine = ToolPermissionPolicyEngine(),
    private val executor: ResilientToolExecutor = ResilientToolExecutor(),
    private val maxEvents: Int = 200,
    private val now: () -> Instant = Instant::now
) {
    private val events = ArrayDeque<ToolInvocationEvent>()

    init { require(maxEvents > 0) { "Maximum event count must be positive." } }

    suspend fun invoke(invocation: ToolInvocation, confirmed: Boolean = false): ToolResult {
        val startedAt = now()
        val tool = registry.resolve(invocation.toolId)
            ?: return finish(
                ToolResult(invocation.toolId, ToolExecutionStatus.NOT_FOUND, errorCode = "tool_not_found"),
                startedAt
            )

        validateArguments(tool.manifest, invocation.arguments)?.let { error ->
            return finish(
                ToolResult(tool.manifest.id, ToolExecutionStatus.FAILED, errorCode = error),
                startedAt
            )
        }

        val permission = permissionEngine.evaluate(tool.manifest, invocation.context)
        if (permission.decision == ToolPermissionDecision.DENY) {
            return finish(
                ToolResult(
                    tool.manifest.id,
                    ToolExecutionStatus.DENIED,
                    errorCode = permission.reason,
                    metadata = mapOf("missingPermissions" to permission.missingPermissions.sorted().joinToString(","))
                ),
                startedAt
            )
        }
        if (permission.decision == ToolPermissionDecision.REQUIRE_CONFIRMATION && !confirmed) {
            return finish(
                ToolResult(tool.manifest.id, ToolExecutionStatus.DENIED, errorCode = "confirmation_required"),
                startedAt
            )
        }

        val executionInvocation = invocation.copy(
            context = invocation.context.copy(
                metadata = invocation.context.metadata + (CONFIRMED_METADATA_KEY to confirmed.toString())
            )
        )

        val result = executor.execute(tool, executionInvocation)
        return finish(result, startedAt)
    }

    @Synchronized
    fun eventSnapshot(toolId: String? = null): List<ToolInvocationEvent> = events
        .filter { toolId == null || it.toolId == toolId }

    fun telemetrySnapshot(toolId: String? = null): List<ToolExecutionMetric> =
        executor.telemetrySnapshot(toolId)

    fun telemetrySummary(toolId: String): ToolExecutionSummary =
        executor.telemetrySummary(toolId)

    private fun validateArguments(manifest: ToolManifest, arguments: Map<String, String>): String? {
        val known = manifest.parameters.mapTo(linkedSetOf()) { it.name }
        if (arguments.keys.any { it !in known }) return "unknown_argument"
        val missing = manifest.parameters.filter { it.required && arguments[it.name].isNullOrBlank() }
        return if (missing.isEmpty()) null else "missing_required_argument"
    }

    private fun finish(result: ToolResult, startedAt: Instant, detail: String? = null): ToolResult {
        record(ToolInvocationEvent(result.toolId, result.status, startedAt, now(), detail ?: result.errorCode))
        return result
    }

    @Synchronized
    private fun record(event: ToolInvocationEvent) {
        events.addLast(event)
        while (events.size > maxEvents) events.removeFirst()
    }

    companion object {
        const val CONFIRMED_METADATA_KEY: String = "tool.confirmed"
    }
}
