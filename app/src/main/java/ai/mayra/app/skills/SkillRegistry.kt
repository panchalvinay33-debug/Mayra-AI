package ai.mayra.app.skills

/** Finds the first registered local skill that can safely handle a message. */
class SkillRegistry(
    skills: List<MayraSkill> = listOf(
        HelpSkill(),
        ClearMemorySkill(),
        CalculatorSkill(),
        DateTimeSkill(),
        UnitConversionSkill(),
        PercentageSkill()
    )
) {
    private val registered = skills.toMutableList()

    fun register(skill: MayraSkill) {
        registered.removeAll { it.id == skill.id }
        registered += skill
    }

    fun find(message: String): MayraSkill? = registered.firstOrNull { it.canHandle(message) }

    fun descriptions(): List<String> = registered.map { "${it.id}: ${it.description}" }
}

class HelpSkill : MayraSkill {
    override val id = "help"
    override val description = "Shows Mayra's currently available local capabilities"

    override fun canHandle(message: String): Boolean =
        message.trim().lowercase() in setOf("help", "/help", "what can you do", "tum kya kar sakti ho")

    override suspend fun execute(message: String): SkillResult = SkillResult(
        text = "I can chat, accept voice input, remember recent conversation context, calculate offline, work with percentages, tell the current local date or time, and convert common units without internet."
    )
}

/** Marker command. AIManager performs the actual memory clearing. */
class ClearMemorySkill : MayraSkill {
    override val id = "clear-memory"
    override val description = "Clears the current conversation memory"

    override fun canHandle(message: String): Boolean =
        message.trim().lowercase() in setOf("/clear", "clear memory", "forget this chat")

    override suspend fun execute(message: String): SkillResult = SkillResult(
        text = "Conversation memory cleared.",
        metadata = mapOf("action" to "clear_memory")
    )
}
