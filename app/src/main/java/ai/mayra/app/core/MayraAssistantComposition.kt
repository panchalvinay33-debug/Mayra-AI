package ai.mayra.app.core

import ai.mayra.app.MayraRuntime
import ai.mayra.app.context.MayraContextRepository
import ai.mayra.app.context.MayraLocalContextAssistant
import ai.mayra.app.context.MayraLocalNextSuggestionAssistant
import ai.mayra.app.context.MayraRemoteContextPolicy
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
        val contextRepository = MayraContextRepository(appContext)
        val localAssistant = LocalMayraAssistant(
            LocalCommandEngine(
                actionDispatcher = ActionDispatcher(AndroidActionExecutor(appContext))
            )
        )
        val settings = AndroidMayraProviderSettingsStore(appContext).read()
        val credentials = AndroidMayraProviderCredentialStore(appContext)
        val providerConfig = settings.validatedConfig().getOrThrow()
        val providerOrLocalAssistant: MayraAssistant = if (
            providerConfig.enabled && credentials.hasCredential()
        ) {
            ResilientMayraProviderAssistant(
                provider = MayraHttpConversationalProvider(providerConfig, credentials),
                fallback = localAssistant,
                timeoutMillis = providerConfig.readTimeoutMillis.toLong().coerceAtMost(60_000L),
                maxAttempts = 2,
                retryDelayMillis = 350,
                trustedContextSource = {
                    MayraRemoteContextPolicy.lines(contextRepository.snapshot())
                }
            )
        } else {
            localAssistant
        }

        // Explicit context/status questions are answered before any remote provider call. The local
        // layer sees only J6 normalized aggregates; normal conversation and actions delegate unchanged.
        val contextAssistant: MayraAssistant = MayraLocalContextAssistant(
            delegate = providerOrLocalAssistant,
            contextSource = { contextRepository.snapshot() }
        )

        // "What next" guidance is also local-only and uses the same coarse Context Fabric boundary.
        val conversationalAssistant: MayraAssistant = MayraLocalNextSuggestionAssistant(
            delegate = contextAssistant,
            contextSource = { contextRepository.snapshot() }
        )

        MayraRuntime.assistant = PersonalMemoryAwareMayraAssistant(
            delegate = DocumentInsightAwareMayraAssistant(
                delegate = conversationalAssistant,
                context = appContext
            ),
            memory = MayraRuntime.personalMemory
        )
    }
}
