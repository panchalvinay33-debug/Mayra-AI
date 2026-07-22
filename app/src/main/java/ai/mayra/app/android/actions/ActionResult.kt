package ai.mayra.app.android.actions

/**
 * A stable result contract shared by every Android action.
 *
 * Actions should return a structured result rather than throwing platform
 * exceptions through the brain pipeline.
 */
sealed interface ActionResult {

    data class Success(
        val message: String? = null,
        val data: Map<String, String> = emptyMap(),
    ) : ActionResult

    data class PermissionRequired(
        val permissions: Set<String>,
        val reason: String,
    ) : ActionResult

    data class ConfirmationRequired(
        val prompt: String,
        val risk: ActionRisk,
    ) : ActionResult

    data class Unsupported(
        val reason: String,
    ) : ActionResult

    data class Failure(
        val message: String,
        val recoverable: Boolean = true,
        val cause: Throwable? = null,
    ) : ActionResult
}

enum class ActionRisk {
    SAFE,
    CONFIRMATION,
    HIGH_RISK,
}
