package ai.mayra.app.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraConversationalProviderTest {
    private val fallback = object : MayraAssistant {
        override suspend fun reply(message: String, conversation: List<MayraMessage>): Result<String> =
            Result.success("offline:$message")
    }

    @Test fun returnsRemoteSuccessWithoutFallback() = runBlocking {
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider { MayraProviderResult.Success(" remote answer ") },
            fallback = fallback,
            retryDelayMillis = 0
        )
        assertEquals("remote answer", assistant.reply("hello").getOrThrow())
    }

    @Test fun retriesTemporaryFailureThenSucceeds() = runBlocking {
        var attempts = 0
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider {
                attempts++
                if (attempts == 1) MayraProviderResult.TemporaryFailure("retry")
                else MayraProviderResult.Success("ok")
            },
            fallback = fallback,
            maxAttempts = 2,
            retryDelayMillis = 0
        )
        assertEquals("ok", assistant.reply("hello").getOrThrow())
        assertEquals(2, attempts)
    }

    @Test fun permanentFailureUsesOfflineFallbackWithoutRetry() = runBlocking {
        var attempts = 0
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider { attempts++; MayraProviderResult.PermanentFailure("bad request") },
            fallback = fallback,
            maxAttempts = 3,
            retryDelayMillis = 0
        )
        assertEquals("offline:hello", assistant.reply("hello").getOrThrow())
        assertEquals(1, attempts)
    }

    @Test fun exhaustedTemporaryFailuresUseFallback() = runBlocking {
        var attempts = 0
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider { attempts++; MayraProviderResult.TemporaryFailure("offline") },
            fallback = fallback,
            maxAttempts = 2,
            retryDelayMillis = 0
        )
        assertEquals("offline:hello", assistant.reply("hello").getOrThrow())
        assertEquals(2, attempts)
    }

    @Test fun cancellationIsNeverConvertedIntoFallback() = runBlocking {
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider { throw CancellationException("cancel") },
            fallback = fallback,
            retryDelayMillis = 0
        )
        val error = runCatching { assistant.reply("hello") }.exceptionOrNull()
        assertTrue(error is CancellationException)
    }

    @Test fun requestRejectsUnboundedConversation() {
        val messages = List(101) { MayraMessage("x", MayraMessage.Sender.USER, it.toLong()) }
        assertTrue(runCatching { MayraProviderRequest("hello", messages) }.isFailure)
    }
}
