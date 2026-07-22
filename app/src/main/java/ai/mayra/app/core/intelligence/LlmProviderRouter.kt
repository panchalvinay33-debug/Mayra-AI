package ai.mayra.app.core.intelligence

class NoAvailableLlmProviderException(message: String) : IllegalStateException(message)

/** Selects the highest-priority available provider and fails over on generation errors. */
class LlmProviderRouter(
    providers: List<LlmProvider>
) {
    private val providers = providers
        .also { require(it.isNotEmpty()) { "At least one LLM provider is required." } }
        .also { list -> require(list.map { it.id }.distinct().size == list.size) { "Provider ids must be unique." } }
        .sortedWith(compareByDescending<LlmProvider> { it.priority }.thenBy { it.id })

    suspend fun generate(
        request: LlmRequest,
        preferredProviderId: String? = null
    ): LlmResponse {
        val ordered = if (preferredProviderId == null) {
            providers
        } else {
            val preferred = providers.firstOrNull { it.id == preferredProviderId }
            requireNotNull(preferred) { "Unknown preferred provider: $preferredProviderId" }
            listOf(preferred) + providers.filterNot { it.id == preferredProviderId }
        }

        val failures = mutableListOf<String>()
        for (provider in ordered) {
            val available = runCatching { provider.isAvailable() }
                .getOrElse {
                    failures += "${provider.id}: availability check failed"
                    false
                }
            if (!available) continue

            val response = runCatching { provider.generate(request) }
            if (response.isSuccess) return response.getOrThrow()
            failures += "${provider.id}: ${response.exceptionOrNull()?.message ?: "generation failed"}"
        }

        val detail = failures.joinToString(separator = "; ").ifBlank { "all providers unavailable" }
        throw NoAvailableLlmProviderException("Unable to generate an LLM response: $detail")
    }

    fun providerIds(): List<String> = providers.map { it.id }
}