package ai.mayra.app.brain

/** Replaceable contract for local or remote AI implementations. */
interface AIProvider {
    val name: String
    suspend fun generate(request: AIRequest): AIResponse
}
