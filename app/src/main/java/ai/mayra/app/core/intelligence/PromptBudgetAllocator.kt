package ai.mayra.app.core.intelligence

data class PromptBudgetAllocation(
    val budget: PromptBudget,
    val contextCharacterTarget: Int,
    val memoryItemLimit: Int,
    val rationale: List<String>
)

/** Allocates prompt space from session size and requested response size. */
class PromptBudgetAllocator(
    private val minimumInputCharacters: Int = 2_000,
    private val maximumPromptCharacters: Int = 24_000,
    private val defaultResponseCharacters: Int = 4_000
) {
    init {
        require(minimumInputCharacters > 0) { "Minimum input characters must be positive." }
        require(maximumPromptCharacters > minimumInputCharacters) {
            "Maximum prompt characters must exceed minimum input characters."
        }
        require(defaultResponseCharacters > 0) { "Default response characters must be positive." }
        require(defaultResponseCharacters < maximumPromptCharacters) {
            "Default response characters must fit inside prompt budget."
        }
    }

    fun allocate(
        contextCharacters: Int,
        requestedOutputCharacters: Int = defaultResponseCharacters,
        memoryCandidates: Int = 8
    ): PromptBudgetAllocation {
        require(contextCharacters >= 0) { "Context characters cannot be negative." }
        require(requestedOutputCharacters > 0) { "Requested output characters must be positive." }
        require(memoryCandidates >= 0) { "Memory candidate count cannot be negative." }

        val reservedOutput = requestedOutputCharacters
            .coerceAtMost(maximumPromptCharacters - minimumInputCharacters)
        val inputLimit = maximumPromptCharacters - reservedOutput
        val contextTarget = when {
            contextCharacters <= inputLimit / 2 -> contextCharacters
            contextCharacters <= inputLimit -> (inputLimit * 0.8).toInt()
            else -> (inputLimit * 0.65).toInt()
        }.coerceAtLeast(minimumInputCharacters.coerceAtMost(inputLimit))

        val memoryLimit = when {
            contextCharacters > inputLimit -> 4
            contextCharacters > inputLimit / 2 -> 6
            else -> 8
        }.coerceAtMost(memoryCandidates)

        val rationale = buildList {
            add("reserved_output=$reservedOutput")
            add("input_limit=$inputLimit")
            add("context_target=$contextTarget")
            add("memory_limit=$memoryLimit")
            if (contextCharacters > inputLimit) add("context_compression_required")
        }

        return PromptBudgetAllocation(
            budget = PromptBudget(
                maxCharacters = maximumPromptCharacters,
                reservedResponseCharacters = reservedOutput,
                maxMemoryItems = memoryLimit
            ),
            contextCharacterTarget = contextTarget,
            memoryItemLimit = memoryLimit,
            rationale = rationale
        )
    }
}
