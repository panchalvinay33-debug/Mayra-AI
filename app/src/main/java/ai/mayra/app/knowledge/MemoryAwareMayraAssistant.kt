package ai.mayra.app.knowledge

import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.core.MayraMessage
import android.content.Context

/**
 * Adds a bounded, non-sensitive memory snapshot to assistant conversation context.
 * The current user message is never rewritten, so deterministic local commands remain intact.
 */
class MemoryAwareMayraAssistant(
    context: Context,
    private val delegate: MayraAssistant,
    private val maxContextCharacters: Int = 1_200
) : MayraAssistant {
    private val recall = MayraMemoryRecall(context.applicationContext)

    override suspend fun reply(
        message: String,
        conversation: List<MayraMessage>
    ): Result<String> {
        val context = recall.promptContext(
            query = message,
            maxItems = 5,
            maxCharacters = maxContextCharacters
        )
        if (context.isBlank()) return delegate.reply(message, conversation)

        val privateContext = MayraMessage(
            text = "Private owner memory for context only. Do not expose it unless directly relevant:\n$context",
            sender = MayraMessage.Sender.MAYRA
        )
        return delegate.reply(message, listOf(privateContext) + conversation)
    }
}
