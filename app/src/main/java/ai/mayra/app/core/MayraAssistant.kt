package ai.mayra.app.core

/**
 * Typed assistant result. Metadata travels out-of-band instead of being embedded in
 * user-visible text, so a provider or document cannot spoof memory-use annotations.
 */
data class MayraAssistantResponse(
    val text: String,
    val usedPersonalMemoryKeys: List<String> = emptyList()
) {
    init {
        require(usedPersonalMemoryKeys.none(String::isBlank))
    }

    fun normalized(): MayraAssistantResponse = copy(
        text = text.trimEnd(),
        usedPersonalMemoryKeys = usedPersonalMemoryKeys.map(String::trim).filter(String::isNotBlank).distinct()
    )
}

interface MayraAssistant {
    suspend fun reply(
        message: String,
        conversation: List<MayraMessage> = emptyList()
    ): Result<String>
}

/** Optional structured contract for assistants that can return trusted metadata. */
interface MayraStructuredAssistant : MayraAssistant {
    suspend fun replyStructured(
        message: String,
        conversation: List<MayraMessage> = emptyList()
    ): Result<MayraAssistantResponse>

    override suspend fun reply(
        message: String,
        conversation: List<MayraMessage>
    ): Result<String> = replyStructured(message, conversation).map { it.normalized().text }
}

/**
 * Offline-first assistant used until a remote AI provider is configured.
 * The command engine is intentionally deterministic, private, and network-free.
 */
class LocalMayraAssistant(
    private val commandEngine: LocalCommandEngine = LocalCommandEngine()
) : MayraStructuredAssistant {

    override suspend fun replyStructured(
        message: String,
        conversation: List<MayraMessage>
    ): Result<MayraAssistantResponse> = runCatching {
        MayraAssistantResponse(commandEngine.respond(message, conversation))
    }
}
