package ai.mayra.app.core

import ai.mayra.app.MayraRuntime
import ai.mayra.app.document.DocumentInsightAwareMayraAssistant
import ai.mayra.app.memory.PersonalMemoryAwareMayraAssistant
import ai.mayra.app.platform.device.AndroidActionExecutor
import android.content.Context

/**
 * Rebuilds the user-facing assistant after provider settings change without requiring an app restart.
 * The typed routing runtime reads MayraRuntime.assistant dynamically, so replacing this volatile
 * reference updates conversational answers while action and memory safety boundaries remain intact.
 */
object MayraAssistantComposition {
    fun rebuild(context: Context): Result<Unit> = runCatching {
        check(MayraRuntime.personalMemoryInstalled) { "Personal memory runtime is not ready yet." }

        val appContext = context.applicationContext
        val localAssistant = LocalMayraAssistant(
            LocalCommandEngine(
                actionDispatcher = ActionDispatcher(AndroidActionExecutor(appContext))
            )
        )
        val settings = AndroidMayraProviderSettingsStore(appContext).read()
        val credentials = AndroidMayraProviderCredentialStore(appContext)
        val providerConfig = settings.validatedConfig().getOrThrow()
        val conversationalAssistant: MayraAssistant = if (
            providerConfig.enabled && credentials.hasCredential()
        ) {
            ResilientMayraProviderAssistant(
                provider = MayraHttpConversationalProvider(providerConfig, credentials),
                fallback = localAssistant,
                timeoutMillis = providerConfig.readTimeoutMillis.toLong().coerceAtMost(60_000L),
                maxAttempts = 2,
                retryDelayMillis = 350
            )
        } else {
            localAssistant
        }

        MayraRuntime.assistant = PersonalMemoryAwareMayraAssistant(
            delegate = DocumentInsightAwareMayraAssistant(
                delegate = conversationalAssistant,
                context = appContext
            ),
            memory = MayraRuntime.personalMemory
        )
    }
}
