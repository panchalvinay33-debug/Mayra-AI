package ai.mayra.app.core.intelligence

import java.time.Instant

enum class ToolRiskLevel { LOW, MEDIUM, HIGH }

enum class ToolExecutionStatus { SUCCESS, DENIED, FAILED, NOT_FOUND }

data class ToolParameter(
    val name: String,
    val description: String,
    val required: Boolean = true
) {
    init {
        require(name.isNotBlank()) { "Tool parameter name cannot be blank." }
        require(description.isNotBlank()) { "Tool parameter description cannot be blank." }
    }
}

data class ToolManifest(
    val id: String,
    val displayName: String,
    val description: String,
    val parameters: List<ToolParameter> = emptyList(),
    val requiredPermissions: Set<String> = emptySet(),
    val riskLevel: ToolRiskLevel = ToolRiskLevel.LOW,
    val tags: Set<String> = emptySet(),
    val enabledByDefault: Boolean = true
) {
    init {
        require(id.matches(Regex("[a-z0-9_.-]+"))) { "Tool id must be normalized." }
        require(displayName.isNotBlank()) { "Tool display name cannot be blank." }
        require(description.isNotBlank()) { "Tool description cannot be blank." }
        require(parameters.distinctBy { it.name }.size == parameters.size) {
            "Tool parameter names must be unique."
        }
    }
}

data class ToolExecutionContext(
    val sessionId: String,
    val userId: String? = null,
    val grantedPermissions: Set<String> = emptySet(),
    val metadata: Map<String, String> = emptyMap(),
    val requestedAt: Instant = Instant.now()
) {
    init { require(sessionId.isNotBlank()) { "Session id cannot be blank." } }
}

data class ToolInvocation(
    val toolId: String,
    val arguments: Map<String, String> = emptyMap(),
    val context: ToolExecutionContext
) {
    init { require(toolId.isNotBlank()) { "Tool id cannot be blank." } }
}

data class ToolResult(
    val toolId: String,
    val status: ToolExecutionStatus,
    val output: String? = null,
    val errorCode: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

interface MayraTool {
    val manifest: ToolManifest
    suspend fun execute(invocation: ToolInvocation): ToolResult
}
