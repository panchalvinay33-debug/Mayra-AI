package ai.mayra.app.settings

/** Runtime configuration used by the Phase 2 AI and voice layers. */
data class MayraSettings(
    val assistantName: String = "Mayra",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val voiceEnabled: Boolean = true,
    val wakeWordEnabled: Boolean = true,
    val rememberConversation: Boolean = true,
    val maxMemoryEntries: Int = 20
) {
    init {
        require(assistantName.isNotBlank()) { "Assistant name cannot be blank." }
        require(maxMemoryEntries in 1..200) { "Memory entry limit must be between 1 and 200." }
    }

    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            "You are Mayra, a warm, practical and privacy-conscious personal AI assistant."
    }
}

/**
 * Small replaceable settings contract. A DataStore-backed implementation can be plugged in
 * without coupling the AI manager or UI to Android persistence APIs.
 */
interface MayraSettingsStore {
    suspend fun load(): MayraSettings
    suspend fun save(settings: MayraSettings)
}

class InMemoryMayraSettingsStore(
    initial: MayraSettings = MayraSettings()
) : MayraSettingsStore {
    @Volatile
    private var current = initial

    override suspend fun load(): MayraSettings = current

    override suspend fun save(settings: MayraSettings) {
        current = settings
    }
}
