package ai.mayra.app.core.intelligence

data class ContextCompressionPolicy(
    val maxCharacters: Int = 8_000,
    val preserveNewestMessages: Int = 6,
    val preserveSystemMessages: Boolean = true
) {
    init {
        require(maxCharacters > 0) { "Maximum compressed characters must be positive." }
        require(preserveNewestMessages >= 0) { "Preserved message count cannot be negative." }
    }
}

data class ContextCompressionResult(
    val messages: List<ConversationMessage>,
    val originalCharacters: Int,
    val compressedCharacters: Int,
    val droppedMessageIds: List<String>,
    val truncated: Boolean
)

/** Reduces context deterministically while preserving system messages and the newest useful turns. */
class ContextCompressor(
    private val policy: ContextCompressionPolicy = ContextCompressionPolicy()
) {
    fun compress(messages: List<ConversationMessage>): ContextCompressionResult {
        val originalCharacters = messages.sumOf { it.content.length }
        if (originalCharacters <= policy.maxCharacters) {
            return ContextCompressionResult(
                messages = messages.toList(),
                originalCharacters = originalCharacters,
                compressedCharacters = originalCharacters,
                droppedMessageIds = emptyList(),
                truncated = false
            )
        }

        val requiredIds = linkedSetOf<String>()
        if (policy.preserveSystemMessages) {
            messages.filter { it.role == ConversationRole.SYSTEM }.forEach { requiredIds += it.id }
        }
        messages.takeLast(policy.preserveNewestMessages).forEach { requiredIds += it.id }

        val selected = mutableListOf<ConversationMessage>()
        var used = 0

        fun tryAdd(message: ConversationMessage): Boolean {
            if (message.id in selected.map { it.id }) return true
            if (used + message.content.length > policy.maxCharacters) return false
            selected += message
            used += message.content.length
            return true
        }

        messages.filter { it.id in requiredIds }.forEach(::tryAdd)
        messages.asReversed()
            .filterNot { it.id in requiredIds }
            .forEach { tryAdd(it) }

        val ordered = selected.sortedBy { selectedMessage ->
            messages.indexOfFirst { it.id == selectedMessage.id }
        }
        val selectedIds = ordered.mapTo(linkedSetOf()) { it.id }
        val droppedIds = messages.filterNot { it.id in selectedIds }.map { it.id }

        return ContextCompressionResult(
            messages = ordered,
            originalCharacters = originalCharacters,
            compressedCharacters = ordered.sumOf { it.content.length },
            droppedMessageIds = droppedIds,
            truncated = droppedIds.isNotEmpty()
        )
    }
}