package ai.mayra.app.core.intelligence

data class AiTurnRuntimeRequest(
    val sessionId: String,
    val userInput: String,
    val memoryTags: Set<String> = emptySet(),
    val preferredProviderId: String? = null,
    val options: LlmGenerationOptions = LlmGenerationOptions(),
    val cacheable: Boolean = true
) {
    init {
        require(sessionId.isNotBlank()) { "Session id cannot be blank." }
        require(userInput.isNotBlank()) { "User input cannot be blank." }
    }
}

data class AiTurnRuntimeResult(
    val response: LlmResponse,
    val execution: LlmExecutionResult,
    val compression: ContextCompressionResult,
    val summary: ConversationSummary?,
    val consolidation: MemoryConsolidationResult,
    val budget: PromptBudgetAllocation
)

/** Production conversation turn runtime connecting context, memory, prompt and execution layers. */
class AiTurnRuntime(
    private val sessions: ConversationSessionManager,
    private val memoryRetriever: MemoryRetriever,
    private val promptBuilder: PromptBuilder,
    private val executionEngine: LlmExecutionEngine,
    private val memoryConsolidator: MemoryConsolidator,
    private val memoryExtractor: ConversationMemoryExtractor = ConversationMemoryExtractor(),
    private val summarizer: ConversationSummarizer = ConversationSummarizer(),
    private val budgetAllocator: PromptBudgetAllocator = PromptBudgetAllocator(),
    private val diagnostics: ConversationRuntimeDiagnostics = ConversationRuntimeDiagnostics(),
    private val defaultSystemInstructions: List<String> = listOf(
        "You are Mayra, a helpful and reliable mobile assistant.",
        "Be truthful, practical, and preserve the user's language when appropriate."
    )
) {
    suspend fun execute(request: AiTurnRuntimeRequest): AiTurnRuntimeResult {
        val startedAt = diagnostics.now()
        val session = sessions.requireActive(request.sessionId)
        val before = sessions.snapshot(request.sessionId)
        val allocation = budgetAllocator.allocate(
            contextCharacters = before.estimatedCharacters,
            requestedOutputCharacters = request.options.maxOutputCharacters
        )
        val compression = ContextCompressor(
            ContextCompressionPolicy(
                maxCharacters = allocation.contextCharacterTarget,
                preserveNewestMessages = 8,
                preserveSystemMessages = true
            )
        ).compress(before.messages)
        val droppedMessages = before.messages.filter { it.id in compression.droppedMessageIds }
        val summary = droppedMessages.takeIf { it.isNotEmpty() }?.let(summarizer::summarize)
        val memories = memoryRetriever.retrieve(
            text = request.userInput,
            tags = request.memoryTags,
            limit = allocation.memoryItemLimit.coerceAtLeast(1)
        )
        val instructions = buildList {
            addAll(defaultSystemInstructions)
            summary?.text?.takeIf(String::isNotBlank)?.let { add("Earlier conversation summary:\n$it") }
        }
        val prompt = promptBuilder.build(
            PromptRequest(
                sessionId = request.sessionId,
                userInput = request.userInput,
                systemInstructions = instructions,
                context = compression.messages,
                memories = memories,
                metadata = mapOf(
                    "sessionTitle" to session.title.orEmpty(),
                    "contextCompressed" to compression.truncated.toString()
                ),
                budget = allocation.budget
            )
        )

        sessions.append(
            request.sessionId,
            ConversationMessage(role = ConversationRole.USER, content = request.userInput.trim())
        )

        try {
            val execution = executionEngine.execute(
                LlmExecutionRequest(
                    request = LlmRequest(prompt = prompt, options = request.options),
                    preferredProviderId = request.preferredProviderId,
                    cacheable = request.cacheable,
                    cacheNamespace = "conversation:${request.sessionId}"
                )
            )
            sessions.append(
                request.sessionId,
                ConversationMessage(
                    role = ConversationRole.ASSISTANT,
                    content = execution.response.content,
                    metadata = mapOf(
                        "providerId" to execution.response.providerId,
                        "fromCache" to execution.fromCache.toString(),
                        "attempts" to execution.attempts.toString()
                    )
                )
            )
            val recentTurn = sessions.snapshot(request.sessionId).messages.takeLast(2)
            val consolidation = memoryConsolidator.consolidate(memoryExtractor.extract(recentTurn))
            diagnostics.record(
                ConversationTurnDiagnostic(
                    sessionId = request.sessionId,
                    startedAt = startedAt,
                    completedAt = diagnostics.now(),
                    originalContextCharacters = before.estimatedCharacters,
                    promptCharacters = prompt.estimatedCharacters,
                    droppedMessages = compression.droppedMessageIds.size,
                    retrievedMemories = memories.size,
                    savedMemories = consolidation.saved.size,
                    providerId = execution.response.providerId,
                    attempts = execution.attempts,
                    fromCache = execution.fromCache,
                    success = true
                )
            )
            return AiTurnRuntimeResult(
                response = execution.response,
                execution = execution,
                compression = compression,
                summary = summary,
                consolidation = consolidation,
                budget = allocation
            )
        } catch (error: Throwable) {
            sessions.append(
                request.sessionId,
                ConversationMessage(
                    role = ConversationRole.TOOL,
                    content = "Generation failed: ${error.message ?: error::class.simpleName.orEmpty()}",
                    metadata = mapOf("error" to "true")
                )
            )
            diagnostics.record(
                ConversationTurnDiagnostic(
                    sessionId = request.sessionId,
                    startedAt = startedAt,
                    completedAt = diagnostics.now(),
                    originalContextCharacters = before.estimatedCharacters,
                    promptCharacters = prompt.estimatedCharacters,
                    droppedMessages = compression.droppedMessageIds.size,
                    retrievedMemories = memories.size,
                    savedMemories = 0,
                    providerId = request.preferredProviderId,
                    attempts = 0,
                    fromCache = false,
                    success = false,
                    detail = error.message
                )
            )
            throw error
        }
    }

    fun runtimeMetrics(): ConversationRuntimeMetrics = diagnostics.metrics()

    fun runtimeEvents(sessionId: String? = null): List<ConversationTurnDiagnostic> =
        diagnostics.snapshot(sessionId)
}
