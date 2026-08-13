package ai.mayra.app.core

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** Remote providers return text only; actions and memory writes remain outside this boundary. */
fun interface MayraConversationalProvider {
    suspend fun answer(request: MayraProviderRequest): MayraProviderResult
}

data class MayraProviderRequest(
    val message: String,
    val conversation: List<MayraMessage>,
    val localeTag: String = "en-IN",
    val trustedContext: List<String> = emptyList()
) {
    init {
        require(message.isNotBlank())
        require(conversation.size <= 100)
        require(localeTag.isNotBlank())
        require(trustedContext.size <= MAX_TRUSTED_CONTEXT_LINES)
        require(trustedContext.all { it.isNotBlank() && it.length <= MAX_TRUSTED_CONTEXT_LINE_CHARS })
    }

    private companion object {
        const val MAX_TRUSTED_CONTEXT_LINES = 12
        const val MAX_TRUSTED_CONTEXT_LINE_CHARS = 160
    }
}

sealed interface MayraProviderResult {
    data class Success(val text: String) : MayraProviderResult {
        init { require(text.isNotBlank()) }
    }
    data class TemporaryFailure(val reason: String) : MayraProviderResult
    data class PermanentFailure(val reason: String) : MayraProviderResult
}

/**
 * Production-safe adapter: bounded timeout/retry, cancellation propagation and deterministic
 * offline fallback. It never retries permanent failures and never executes actions.
 */
class ResilientMayraProviderAssistant(
    private val provider: MayraConversationalProvider,
    private val fallback: MayraAssistant,
    private val timeoutMillis: Long = 20_000,
    private val maxAttempts: Int = 2,
    private val retryDelayMillis: Long = 350,
    private val trustedContextSource: () -> List<String> = { emptyList() }
) : MayraAssistant {
    init {
        require(timeoutMillis in 1_000..60_000)
        require(maxAttempts in 1..3)
        require(retryDelayMillis in 0..5_000)
    }

    override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> {
        val trustedContext = runCatching { trustedContextSource() }
            .getOrDefault(emptyList())
            .filter(String::isNotBlank)
            .distinct()
            .take(12)
        val request = MayraProviderRequest(
            message = message.trim(),
            conversation = conversation.takeLast(100),
            trustedContext = trustedContext
        )
        repeat(maxAttempts) { index ->
            val result = try {
                withTimeout(timeoutMillis) { provider.answer(request) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                MayraProviderResult.TemporaryFailure(error.message ?: "Network unavailable")
            } catch (error: Exception) {
                MayraProviderResult.PermanentFailure(error.message ?: "Provider failed")
            }
            when (result) {
                is MayraProviderResult.Success -> return Result.success(result.text.trim())
                is MayraProviderResult.PermanentFailure -> return fallback.reply(message, conversation)
                is MayraProviderResult.TemporaryFailure -> {
                    if (index + 1 < maxAttempts && retryDelayMillis > 0) delay(retryDelayMillis)
                }
            }
        }
        return fallback.reply(message, conversation)
    }
}

/** Provider secrets must come from runtime-secure configuration, never source control or memory. */
fun interface MayraProviderCredentialSource {
    fun bearerToken(): String?
}
