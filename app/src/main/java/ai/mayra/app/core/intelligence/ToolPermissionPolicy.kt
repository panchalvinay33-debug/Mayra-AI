package ai.mayra.app.core.intelligence

enum class ToolPermissionDecision { ALLOW, REQUIRE_CONFIRMATION, DENY }

data class ToolPermissionEvaluation(
    val decision: ToolPermissionDecision,
    val missingPermissions: Set<String> = emptySet(),
    val reason: String
)

data class ToolPermissionPolicy(
    val deniedToolIds: Set<String> = emptySet(),
    val confirmationRequiredFor: Set<ToolRiskLevel> = setOf(ToolRiskLevel.HIGH),
    val allowMissingPermissionsForLowRisk: Boolean = false
)

class ToolPermissionPolicyEngine(
    private val policy: ToolPermissionPolicy = ToolPermissionPolicy()
) {
    fun evaluate(manifest: ToolManifest, context: ToolExecutionContext): ToolPermissionEvaluation {
        if (manifest.id in policy.deniedToolIds) {
            return ToolPermissionEvaluation(
                decision = ToolPermissionDecision.DENY,
                reason = "tool_explicitly_denied"
            )
        }

        val missing = manifest.requiredPermissions - context.grantedPermissions
        if (missing.isNotEmpty() && !(manifest.riskLevel == ToolRiskLevel.LOW && policy.allowMissingPermissionsForLowRisk)) {
            return ToolPermissionEvaluation(
                decision = ToolPermissionDecision.DENY,
                missingPermissions = missing,
                reason = "missing_permissions"
            )
        }

        if (manifest.riskLevel in policy.confirmationRequiredFor) {
            return ToolPermissionEvaluation(
                decision = ToolPermissionDecision.REQUIRE_CONFIRMATION,
                reason = "risk_confirmation_required"
            )
        }

        return ToolPermissionEvaluation(
            decision = ToolPermissionDecision.ALLOW,
            reason = "policy_allowed"
        )
    }
}
