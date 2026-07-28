package ai.mayra.app.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalCommandEngineCapabilityTest {
    private val engine = LocalCommandEngine()

    @Test
    fun hinglishCapabilityQuestionReturnsUsefulFeatureSummary() = runBlocking {
        val reply = engine.respond("Tmhari capability kya kya he")

        assertTrue(reply.contains("Hindi/Hinglish"))
        assertTrue(reply.contains("local memory"))
        assertTrue(reply.contains("PDF"))
        assertTrue(reply.contains("safe confirmation"))
        assertFalse(reply.contains("I understood:"))
    }

    @Test
    fun capabilitySummaryClearlyStatesOfflineLimitation() = runBlocking {
        val reply = engine.respond("What are your capabilities?")

        assertTrue(reply.contains("internet-based general AI"))
        assertTrue(reply.contains("fresh web knowledge"))
        assertTrue(reply.contains("connected nahi hai"))
    }
}
