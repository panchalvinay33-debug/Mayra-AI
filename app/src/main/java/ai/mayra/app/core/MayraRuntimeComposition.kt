package ai.mayra.app.core

import android.content.Context
import android.content.Intent

/**
 * Android composition root for the typed Mayra runtime.
 *
 * The first concrete device action is intentionally narrow: opening Android's system document
 * picker. Destructive actions remain confirmation-gated and unsupported until a dedicated executor
 * is reviewed and registered.
 */
class MayraAndroidRuntimeComposition(
    context: Context,
    answerProvider: MayraAnswerProvider,
    enableSafeFilePickerAction: Boolean = true
) {
    private val appContext = context.applicationContext
    val activityLog = MayraPersistentActivityLog(
        appContext.getSharedPreferences(ACTIVITY_PREFERENCES, Context.MODE_PRIVATE)
    )
    private val activityRecorder = MayraActivityRecorder(activityLog)
    private val confirmationTokens = MayraConfirmationTokenStore()
    private val idempotency = MayraInMemoryIdempotencyStore()
    private val safeActionExecutor = MayraDeviceActionExecutor { message, _ ->
        require(message.trim().lowercase().replace(Regex("\\s+"), " ") == "open file manager") {
            "This Android action is not registered in the safe executor."
        }
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(intent)
        "Opened Android's system file picker."
    }

    val runtime: MayraRoutingRuntime = MayraRoutingRuntime(
        capabilities = MayraRuntimeCapabilities(
            coreAssistant = true,
            documentLibrary = true,
            deviceActions = enableSafeFilePickerAction,
            documentOcr = false,
            legacyDocParser = false
        ),
        handlers = MayraConcreteRuntimeAdapters.create(
            context = appContext,
            answerProvider = answerProvider,
            actionExecutor = safeActionExecutor.takeIf { enableSafeFilePickerAction }
        ),
        idempotencyStore = idempotency,
        activityRecorder = activityRecorder,
        confirmationTokens = confirmationTokens
    )

    companion object {
        const val ACTIVITY_PREFERENCES = "mayra_runtime_activity"
    }
}
