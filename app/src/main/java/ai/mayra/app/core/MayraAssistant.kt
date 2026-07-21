package ai.mayra.app.core

interface MayraAssistant {
    suspend fun reply(message: String): Result<String>
}

class LocalMayraAssistant : MayraAssistant {
    override suspend fun reply(message: String): Result<String> {
        val clean = message.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        return Result.success(
            "I heard you: \"$clean\". My AI connection will be added in the next development phase."
        )
    }
}
