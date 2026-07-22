package ai.mayra.app.core.orchestration

import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.runtime.RuntimeKernel
import ai.mayra.app.core.voice.VoiceConversationEvent
import ai.mayra.app.core.voice.VoiceConversationEngine
import ai.mayra.app.core.voice.VoiceConversationSnapshot
import ai.mayra.app.core.voice.VoiceConversationState
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MayraAiOrchestratorTest {
    private val commandEngine = LocalCommandEngine(
        dateProvider = { LocalDate.of(2026, 7, 22) },
        timeProvider = { LocalTime.of(9, 30) }
    )

    @Test
    fun `text turn normalizes input and returns completed response`() = runTest {
        val orchestrator = MayraAiOrchestrator(
            commandEngine = commandEngine,
            runtimeKernel = RuntimeKernel(),
            clock = { 100L }
        )

        val result = orchestrator.processText("   what   time   ")

        assertTrue(result is OrchestrationResult.Completed)
        result as OrchestrationResult.Completed
        assertEquals("what time", result.input)
        assertEquals("It’s 9:30 AM.", result.response)
        assertTrue(result.taskId.isNotBlank())
    }

    @Test
    fun `blank text is rejected without runtime work`() = runTest {
        val runtime = RuntimeKernel()
        val orchestrator = MayraAiOrchestrator(
            commandEngine = commandEngine,
            runtimeKernel = runtime
        )

        val result = orchestrator.processText("   ")

        assertEquals(
            OrchestrationResult.Rejected("Please say or type a command."),
            result
        )
        assertEquals(0, runtime.snapshot().queuedTasks)
        assertEquals(0L, runtime.snapshot().completedTasks)
    }

    @Test
    fun `voice turn moves from listening to speaking with response`() = runTest {
        val voiceEngine = VoiceConversationEngine()
        val listening = voiceEngine.reduce(
            VoiceConversationSnapshot(),
            VoiceConversationEvent.StartListening(timestamp = 10L)
        )
        val orchestrator = MayraAiOrchestrator(
            commandEngine = commandEngine,
            runtimeKernel = RuntimeKernel(),
            voiceEngine = voiceEngine,
            clock = { 20L }
        )

        val result = orchestrator.processVoiceTurn(
            current = listening,
            transcript = "hello",
            timestamp = 15L
        )

        assertTrue(result is VoiceOrchestrationResult.Completed)
        result as VoiceOrchestrationResult.Completed
        assertEquals(VoiceConversationState.SPEAKING, result.snapshot.state)
        assertEquals("hello", result.snapshot.finalTranscript)
        assertEquals(
            "Good morning! I’m Mayra. How can I help you?",
            result.snapshot.responseText
        )
        assertEquals(1L, result.snapshot.turnId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `voice turn requires listening state`() = runTest {
        val orchestrator = MayraAiOrchestrator(
            commandEngine = commandEngine,
            runtimeKernel = RuntimeKernel()
        )

        orchestrator.processVoiceTurn(
            current = VoiceConversationSnapshot(),
            transcript = "hello"
        )
    }
}
