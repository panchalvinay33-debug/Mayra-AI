package ai.mayra.app.memory

/**
 * Small, dependency-free memory store. Persistence can replace this implementation later
 * without changing the AI or UI contracts.
 */
class ConversationMemory(
    private val maxEntriesPerConversation: Int = 20
) {
    private val conversations = mutableMapOf<String, MutableList<MemoryEntry>>()

    @Synchronized
    fun remember(conversationId: String, entry: MemoryEntry) {
        val entries = conversations.getOrPut(conversationId) { mutableListOf() }
        entries += entry
        while (entries.size > maxEntriesPerConversation) entries.removeAt(0)
    }

    @Synchronized
    fun recent(conversationId: String, limit: Int = 8): List<MemoryEntry> =
        conversations[conversationId].orEmpty().takeLast(limit.coerceAtLeast(0))

    @Synchronized
    fun clear(conversationId: String) {
        conversations.remove(conversationId)
    }

    @Synchronized
    fun clearAll() {
        conversations.clear()
    }

    fun asPromptContext(conversationId: String, limit: Int = 8): String =
        recent(conversationId, limit).joinToString("\n") { entry ->
            "${entry.role.name.lowercase()}: ${entry.text}"
        }
}
