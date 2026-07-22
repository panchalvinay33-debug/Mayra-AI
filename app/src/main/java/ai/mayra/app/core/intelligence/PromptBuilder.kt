package ai.mayra.app.core.intelligence

/** Builds bounded provider-neutral prompts while preserving the newest useful context. */
class PromptBuilder {

    fun build(request: PromptRequest): AssembledPrompt {
        val limit = request.budget.inputCharacterLimit
        val selected = mutableListOf<PromptMessage>()
        var used = 0
        var truncated = false

        fun add(message: PromptMessage): Boolean {
            val size = estimate(message)
            if (used + size > limit) return false
            selected += message
            used += size
            return true
        }

        request.systemInstructions
            .map(String::trim)
            .filter(String::isNotBlank)
            .forEach { instruction ->
                if (!add(PromptMessage(ConversationRole.SYSTEM, instruction))) truncated = true
            }

        val acceptedMemories = mutableListOf<MemoryRecord>()
        request.memories
            .asSequence()
            .sortedWith(compareByDescending<MemoryRecord> { it.importance }.thenByDescending { it.createdAt })
            .take(request.budget.maxMemoryItems)
            .forEach { memory ->
                val message = PromptMessage(
                    role = ConversationRole.SYSTEM,
                    content = "Relevant memory: ${memory.content}",
                    metadata = mapOf("memoryId" to memory.id)
                )
                if (add(message)) acceptedMemories += memory else truncated = true
            }

        val contextCandidates = request.context
            .filterNot { it.role == ConversationRole.SYSTEM }
            .map { PromptMessage(it.role, it.content, it.metadata) }

        val newestFirst = mutableListOf<PromptMessage>()
        var contextCharacters = 0
        val reservedForUser = estimate(PromptMessage(ConversationRole.USER, request.userInput))
        for (message in contextCandidates.asReversed()) {
            val size = estimate(message)
            if (used + contextCharacters + size + reservedForUser <= limit) {
                newestFirst += message
                contextCharacters += size
            } else {
                truncated = true
            }
        }
        newestFirst.asReversed().forEach { add(it) }

        val userMessage = PromptMessage(ConversationRole.USER, request.userInput.trim())
        if (!add(userMessage)) {
            val available = (limit - used - 8).coerceAtLeast(1)
            val clipped = request.userInput.trim().takeLast(available)
            selected += PromptMessage(
                role = ConversationRole.USER,
                content = clipped,
                metadata = mapOf("truncated" to "true")
            )
            used += estimate(selected.last())
            truncated = true
        }

        return AssembledPrompt(
            sessionId = request.sessionId,
            messages = selected.toList(),
            includedMemoryIds = acceptedMemories.map { it.id },
            estimatedCharacters = used,
            truncated = truncated,
            metadata = request.metadata
        )
    }

    private fun estimate(message: PromptMessage): Int = message.content.length + 8
}