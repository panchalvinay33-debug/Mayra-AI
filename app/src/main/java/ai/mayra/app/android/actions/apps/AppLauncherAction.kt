package ai.mayra.app.android.actions.apps

import android.content.Context
import android.content.Intent
import ai.mayra.app.android.actions.ActionResult
import ai.mayra.app.android.actions.AndroidAction


data class LaunchAppRequest(
    val packageName: String,
)

class AppLauncherAction(
    private val context: Context,
) : AndroidAction<LaunchAppRequest> {

    override val id: String = "device.apps.launch"

    override suspend fun execute(request: LaunchAppRequest): ActionResult {
        val packageName = request.packageName.trim()
        if (packageName.isEmpty()) {
            return ActionResult.Failure("Package name cannot be empty")
        }

        val launchIntent = context.packageManager
            .getLaunchIntentForPackage(packageName)
            ?: return ActionResult.Unsupported("App is not installed or cannot be launched")

        return runCatching {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionResult.Success(
                message = "App opened",
                data = mapOf("packageName" to packageName),
            )
        }.getOrElse { error ->
            ActionResult.Failure(
                message = error.message ?: "Unable to open app",
                recoverable = true,
                cause = error,
            )
        }
    }
}
