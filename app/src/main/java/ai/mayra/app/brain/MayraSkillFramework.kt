package ai.mayra.app.brain

import java.util.concurrent.ConcurrentHashMap

data class SkillDescriptor(
    val id: String,
    val displayName: String,
    val version: Int = 1,
    val supportedIntents: Set<String>,
    val requiredPermissions: Set<String> = emptySet(),
    val sensitive: Boolean = false,
    val enabledByDefault: Boolean = true
)

data class SkillRequest(
    val intent: String,
    val utterance: String,
    val parameters: Map<String, String> = emptyMap(),
    val context: BrainContextSnapshot,
    val confirmed: Boolean = false
)

sealed interface SkillResult {
    data class Success(
        val message: String,
        val data: Map<String, String> = emptyMap()
    ) : SkillResult

    data class NeedsConfirmation(
        val prompt: String,
        val actionType: String,
        val payload: String
    ) : SkillResult

    data class MissingPermission(
        val permissions: Set<String>,
        val explanation: String
    ) : SkillResult

    data class Failure(
        val reason: String,
        val retryable: Boolean = false
    ) : SkillResult

    data object NotHandled : SkillResult
}

interface MayraSkill {
    val descriptor: SkillDescriptor

    /** 0.0 means no match; 1.0 means exact match. */
    fun confidence(request: SkillRequest): Double

    suspend fun execute(request: SkillRequest): SkillResult
}

data class SkillMatch(
    val skill: MayraSkill,
    val confidence: Double
)

data class SkillRegistryDiagnostics(
    val registeredSkills: Int,
    val enabledSkills: Int,
    val disabledSkills: Int,
    val intentCount: Int
)

class MayraSkillRegistry {
    private val skills = ConcurrentHashMap<String, MayraSkill>()
    private val enabledOverrides = ConcurrentHashMap<String, Boolean>()

    fun register(skill: MayraSkill) {
        require(skill.descriptor.id.isNotBlank()) { "Skill id cannot be blank" }
        require(skill.descriptor.supportedIntents.isNotEmpty()) { "Skill must support at least one intent" }
        skills[skill.descriptor.id] = skill
    }

    fun unregister(skillId: String): Boolean {
        enabledOverrides.remove(skillId)
        return skills.remove(skillId) != null
    }

    fun setEnabled(skillId: String, enabled: Boolean) {
        require(skills.containsKey(skillId)) { "Unknown skill: $skillId" }
        enabledOverrides[skillId] = enabled
    }

    fun isEnabled(skillId: String): Boolean {
        val skill = skills[skillId] ?: return false
        return enabledOverrides[skillId] ?: skill.descriptor.enabledByDefault
    }

    fun list(): List<SkillDescriptor> = skills.values
        .map(MayraSkill::descriptor)
        .sortedBy(SkillDescriptor::displayName)

    fun resolve(request: SkillRequest, minimumConfidence: Double = 0.25): List<SkillMatch> = skills.values
        .asSequence()
        .filter { isEnabled(it.descriptor.id) }
        .filter { request.intent in it.descriptor.supportedIntents || "*" in it.descriptor.supportedIntents }
        .map { SkillMatch(it, it.confidence(request).coerceIn(0.0, 1.0)) }
        .filter { it.confidence >= minimumConfidence }
        .sortedByDescending(SkillMatch::confidence)
        .toList()

    suspend fun executeBest(request: SkillRequest): SkillResult {
        val match = resolve(request).firstOrNull() ?: return SkillResult.NotHandled
        val missingPermissions = match.skill.descriptor.requiredPermissions
            .filterNot { permission -> request.parameters["permission:$permission"] == "granted" }
            .toSet()

        if (missingPermissions.isNotEmpty()) {
            return SkillResult.MissingPermission(
                permissions = missingPermissions,
                explanation = "${match.skill.descriptor.displayName} needs permission before it can continue."
            )
        }

        if (match.skill.descriptor.sensitive && !request.confirmed) {
            return SkillResult.NeedsConfirmation(
                prompt = "Please confirm ${match.skill.descriptor.displayName.lowercase()}.",
                actionType = match.skill.descriptor.id,
                payload = request.utterance
            )
        }

        return runCatching { match.skill.execute(request) }
            .getOrElse { SkillResult.Failure(it.message ?: "Skill execution failed", retryable = true) }
    }

    fun diagnostics(): SkillRegistryDiagnostics {
        val all = skills.values.toList()
        val enabled = all.count { isEnabled(it.descriptor.id) }
        return SkillRegistryDiagnostics(
            registeredSkills = all.size,
            enabledSkills = enabled,
            disabledSkills = all.size - enabled,
            intentCount = all.flatMap { it.descriptor.supportedIntents }.toSet().size
        )
    }
}

abstract class KeywordSkill : MayraSkill {
    protected abstract val keywords: Set<String>

    override fun confidence(request: SkillRequest): Double {
        if (keywords.isEmpty()) return 0.0
        val normalized = request.utterance.lowercase()
        val matches = keywords.count(normalized::contains)
        return (matches.toDouble() / keywords.size.coerceAtMost(3)).coerceAtMost(1.0)
    }
}

class HelpSkill : KeywordSkill() {
    override val descriptor = SkillDescriptor(
        id = "help",
        displayName = "Help",
        supportedIntents = setOf("help", "capabilities"),
        sensitive = false
    )

    override val keywords = setOf("help", "what can you do", "kya kar sakti", "madad")

    override suspend fun execute(request: SkillRequest): SkillResult.Success = SkillResult.Success(
        message = "I can help with device actions, reminders, notifications and planned tasks. Sensitive actions always require confirmation."
    )
}
