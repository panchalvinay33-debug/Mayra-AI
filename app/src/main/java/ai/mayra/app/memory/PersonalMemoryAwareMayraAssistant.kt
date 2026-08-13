package ai.mayra.app.memory

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraAssistantResponse
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.MayraStructuredAssistant

/** Read-only approved-memory context decorator with trusted typed usage metadata. */
class PersonalMemoryAwareMayraAssistant(
    private val delegate: MayraAssistant,
    private val memory: MayraPersonalMemoryManager,
    private val maxMemories: Int = 5
) : MayraStructuredAssistant {
    init { require(maxMemories in 1..10) }

    override suspend fun replyStructured(
        message: String,
        conversation: List<MayraMessage>
    ): Result<MayraAssistantResponse> {
        val relevant = memory.retrieve(message, maxMemories)
        if (relevant.isEmpty()) return delegate.structuredReply(message, conversation)

        val context = relevant.joinToString("\n") { record ->
            "- ${record.key}: ${record.value} (approved memory; source ${record.provenance.sourceType})"
        }
        val groundedMessage =
            "$message\n\n[Mayra approved personal context — use only when relevant; do not claim more than shown]\n$context"

        return delegate.structuredReply(groundedMessage, conversation).map { response ->
            response.copy(
                usedPersonalMemoryKeys = response.usedPersonalMemoryKeys + relevant.map { it.key }
            ).normalized()
        }
    }

    private suspend fun MayraAssistant.structuredReply(
        message: String,
        conversation: List<MayraMessage>
    ): Result<MayraAssistantResponse> = when (this) {
        is MayraStructuredAssistant -> replyStructured(message, conversation)
        else -> reply(message, conversation).map(::MayraAssistantResponse)
    }
}
