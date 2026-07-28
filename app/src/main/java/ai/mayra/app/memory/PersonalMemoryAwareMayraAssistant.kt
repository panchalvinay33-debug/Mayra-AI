package ai.mayra.app.memory

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import android.util.Base64

/** Read-only approved-memory context decorator with machine-readable usage metadata. */
class PersonalMemoryAwareMayraAssistant(
    private val delegate: MayraAssistant,
    private val memory: MayraPersonalMemoryManager,
    private val maxMemories: Int = 5
) : MayraAssistant {
    init { require(maxMemories in 1..10) }

    override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
        val relevant = memory.retrieve(message, maxMemories)
        if (relevant.isEmpty()) return delegate.reply(message, conversation)
        val context = relevant.joinToString("\n") { record ->
            "- ${record.key}: ${record.value} (approved memory; source ${record.provenance.sourceType})"
        }
        val groundedMessage = "$message\n\n[Mayra approved personal context — use only when relevant; do not claim more than shown]\n$context"
        return delegate.reply(groundedMessage, conversation).map { answer ->
            val encodedKeys = relevant.joinToString(",") { record ->
                Base64.encodeToString(record.key.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            }
            "$answer\n$USAGE_MARKER$encodedKeys$USAGE_SUFFIX"
        }
    }

    companion object {
        const val USAGE_MARKER = "[[mayra-memory-keys:"
        const val USAGE_SUFFIX = "]]"
    }
}
