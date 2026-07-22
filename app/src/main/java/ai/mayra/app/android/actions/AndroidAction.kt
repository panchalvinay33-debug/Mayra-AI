package ai.mayra.app.android.actions

/**
 * Typed boundary between Mayra skills and Android platform APIs.
 *
 * Keeping platform work behind this contract makes skills testable and lets
 * the planner reason about risk, permissions, and failures consistently.
 */
interface AndroidAction<in Request> {

    val id: String

    val risk: ActionRisk
        get() = ActionRisk.SAFE

    suspend fun execute(request: Request): ActionResult
}
