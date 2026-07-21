package ai.mayra.app.skills

/** A deterministic local capability that may answer before a remote AI provider is used. */
interface MayraSkill {
    val id: String
    val description: String

    fun canHandle(message: String): Boolean
    suspend fun execute(message: String): SkillResult
}

data class SkillResult(
    val text: String,
    val isSuccess: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
)
