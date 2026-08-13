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

    @Test fun carriesBoundedTrustedContextWithoutChangingUserMessage() = runBlocking {
        var captured: MayraProviderRequest? = null
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider { request ->
                captured = request
                MayraProviderResult.Success("ok")
            },
            fallback = fallback,
            retryDelayMillis = 0,
            trustedContextSource = {
                listOf("day_part=morning", "battery_percent=80", "day_part=morning")
            }
        )

        assertEquals("ok", assistant.reply("what should I do?").getOrThrow())
        assertEquals("what should I do?", captured?.message)
        assertEquals(listOf("day_part=morning", "battery_percent=80"), captured?.trustedContext)
    }

    @Test fun contextSourceFailureDoesNotBreakConversation() = runBlocking {
        var captured: MayraProviderRequest? = null
        val assistant = ResilientMayraProviderAssistant(
            provider = MayraConversationalProvider { request ->
                captured = request
                MayraProviderResult.Success("ok")
            },
            fallback = fallback,
            retryDelayMillis = 0,
            trustedContextSource = { error("context unavailable") }
        )

        assertEquals("ok", assistant.reply("hello").getOrThrow())
        assertTrue(captured?.trustedContext?.isEmpty() == true)
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
            retryDelayMillis = 0,
            trustedContextSource = { listOf("day_part=night") }
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

    @Test fun requestRejectsUnboundedTrustedContext() {
        assertTrue(
            runCatching {
                MayraProviderRequest(
                    message = "hello",
                    conversation = emptyList(),
                    trustedContext = List(13) { "context_$it=value" }
                )
            }.isFailure
        )
        assertTrue(
            runCatching {
                MayraProviderRequest(
                    message = "hello",
                    conversation = emptyList(),
                    trustedContext = listOf("x".repeat(161))
                )
            }.isFailure
        )
    }
}
