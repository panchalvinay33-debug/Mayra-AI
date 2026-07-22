package ai.mayra.app.core.intelligence

import java.security.MessageDigest
import kotlinx.coroutines.delay

data class LlmExecutionRequest(
    val request: LlmRequest,
    val preferredProviderId: String? = null,
    val cacheable: Boolean = true,
    val cacheNamespace: String = "default"
) {
    init {
        require(cacheNamespace.isNotBlank()) { "Cache namespace cannot be blank." }
    }
}

data class LlmExecutionResult(
    val response: LlmResponse,
    val attempts: Int,
    val fromCache: Boolean,
    val cacheKey: String,
    val validation: ResponseValidationResult
)

/** Coordinates cache lookup, provider routing, retry, validation, health and telemetry. */
class LlmExecutionEngine(
    private val router: LlmProviderRouter,
    private val validator: ResponseValidator = ResponseValidator(),
    private val cache: PromptCache? = null,
    private val retryPolicy: LlmRetryPolicy = LlmRetryPolicy(),
    private val healthTracker: LlmProviderHealthTracker = LlmProviderHealthTracker(),
    private val telemetry: LlmExecutionTelemetry = LlmExecutionTelemetry(),
    private val sleeper: suspend (Long) -> Unit = { delay(it) }
) {
    suspend fun execute(execution: LlmExecutionRequest): LlmExecutionResult {
        val startedAt = telemetry.now()
        val prompt = execution.request.prompt
        val cacheKey = createCacheKey(execution)

        if (execution.cacheable) {
            cache?.get(cacheKey)?.let { cached ->
                val validation = validator.validate(cached.content)
                if (validation.valid) {
                    val normalized = cached.copy(content = validation.normalizedContent)
                    telemetry.record(
                        event(prompt, normalized, LlmExecutionOutcome.CACHE_HIT, 0, startedAt)
                    )
                    return LlmExecutionResult(normalized, 0, true, cacheKey, validation)
                }
                cache.remove(cacheKey)
            }
        }

        var attempt = 1
        while (attempt <= retryPolicy.maxAttempts) {
            val generation = runCatching {
                router.generate(execution.request, execution.preferredProviderId)
            }

            if (generation.isFailure) {
                val error = generation.exceptionOrNull()!!
                execution.preferredProviderId?.let(healthTracker::recordFailure)
                if (!retryPolicy.shouldRetry(attempt, error)) {
                    telemetry.record(
                        event(
                            prompt = prompt,
                            response = null,
                            outcome = LlmExecutionOutcome.GENERATION_FAILED,
                            attempts = attempt,
                            startedAt = startedAt,
                            detail = error.message
                        )
                    )
                    throw error
                }
            } else {
                val generated = generation.getOrThrow()
                val validation = validator.validate(generated.content)
                if (validation.valid) {
                    val normalized = generated.copy(content = validation.normalizedContent)
                    healthTracker.recordSuccess(normalized.providerId)
                    if (execution.cacheable) cache?.put(cacheKey, normalized)
                    telemetry.record(
                        event(prompt, normalized, LlmExecutionOutcome.SUCCESS, attempt, startedAt)
                    )
                    return LlmExecutionResult(normalized, attempt, false, cacheKey, validation)
                }

                healthTracker.recordFailure(generated.providerId)
                val error = IllegalArgumentException(
                    "Invalid LLM response: ${validation.violations.joinToString()}"
                )
                if (!retryPolicy.shouldRetry(attempt, error)) {
                    telemetry.record(
                        event(
                            prompt = prompt,
                            response = generated,
                            outcome = LlmExecutionOutcome.VALIDATION_FAILED,
                            attempts = attempt,
                            startedAt = startedAt,
                            detail = error.message
                        )
                    )
                    throw error
                }
            }

            attempt += 1
            sleeper(retryPolicy.delayBeforeAttempt(attempt))
        }

        error("LLM execution exhausted unexpectedly.")
    }

    fun providerHealth(providerId: String): LlmProviderHealth = healthTracker.snapshot(providerId)

    fun executionMetrics(): LlmExecutionMetrics = telemetry.metrics()

    fun executionEvents(sessionId: String? = null): List<LlmExecutionEvent> = telemetry.snapshot(sessionId)

    private fun event(
        prompt: AssembledPrompt,
        response: LlmResponse?,
        outcome: LlmExecutionOutcome,
        attempts: Int,
        startedAt: java.time.Instant,
        detail: String? = null
    ) = LlmExecutionEvent(
        sessionId = prompt.sessionId,
        providerId = response?.providerId,
        outcome = outcome,
        attempts = attempts,
        promptCharacters = prompt.estimatedCharacters,
        responseCharacters = response?.content?.length ?: 0,
        startedAt = startedAt,
        completedAt = telemetry.now(),
        detail = detail
    )

    private fun createCacheKey(execution: LlmExecutionRequest): String {
        val source = buildString {
            append(execution.cacheNamespace.trim())
            append('|')
            append(execution.preferredProviderId.orEmpty())
            append('|')
            append(execution.request.options.temperature)
            append('|')
            append(execution.request.options.maxOutputCharacters)
            execution.request.options.stopSequences.forEach { append('|').append(it) }
            execution.request.prompt.messages.forEach { message ->
                append('|').append(message.role.name).append(':').append(message.content)
                message.metadata.toSortedMap().forEach { (key, value) ->
                    append(';').append(key).append('=').append(value)
                }
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
