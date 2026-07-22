package ai.mayra.app.core

interface MayraAssistant {
    suspend fun reply(
        message: String,
        conversation: List<MayraMessage> = emptyList()
    ): Result<String>
}

/**
 * Offline-first assistant used until a remote AI provider is configured.
 * The command engine is intentionally deterministic, private, and network-free.
 */
class LocalMayraAssistant(
    private val commandEngine: LocalCommandEngine = LocalCommandEngine()
) : MayraAssistant {

    override suspend fun reply(
        message: String,
        conversation: List<MayraMessage>
    ): Result<String> = runCatching {
        commandEngine.respond(message, conversation)
    }
}
