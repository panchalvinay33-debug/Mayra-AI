package ai.mayra.app.memory

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage

/**
 * Read-only context decorator. It never writes memory and only exposes approved, active,
 * query-relevant records to the downstream assistant.
 */
class PersonalMemoryAwareMayraAssistant(
    private val delegate: MayraAssistant,
    private val memory: MayraPersonalMemoryManager,
    private val maxMemories: Int = 5
) : MayraAssistant {
    init { require(maxMemories in 1..10) }

    override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
        val relevant = memory.retrieve(message, maxMemories)
        if (relevant.isEmpty()) return delegate.reply(message, conversation)

        val context = relevant.joinToString(separator = "\n") { record ->
            "- ${record.key}: ${record.value} (approved memory; source ${record.provenance.sourceType})"
        }
        val groundedMessage = buildString {
            append(message)
            append("\n\n[Mayra approved personal context — use only when relevant; do not claim more than shown]\n")
            append(context)
        }
        return delegate.reply(groundedMessage, conversation)
    }
}
