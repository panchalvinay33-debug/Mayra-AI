package ai.mayra.app.core.orchestration

import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.runtime.RuntimeKernel
import ai.mayra.app.core.runtime.RuntimeTask
import ai.mayra.app.core.runtime.TaskPriority
import ai.mayra.app.core.voice.VoiceConversationEngine
import ai.mayra.app.core.voice.VoiceConversationEvent
import ai.mayra.app.core.voice.VoiceConversationSnapshot

/**
 * Coordinates user input, conversation context, runtime execution, and voice-state transitions.
 *
 * This class deliberately contains no Android framework dependencies. UI, SpeechRecognizer,
 * TextToSpeech, and persistence adapters can call it from their own lifecycle-aware boundaries.
 */
class MayraAiOrchestrator(
    private val commandEngine: LocalCommandEngine = LocalCommandEngine(),
    private val runtimeKernel: RuntimeKernel = RuntimeKernel(),
    private val voiceEngine: VoiceConversationEngine = VoiceConversationEngine(),
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun processText(
        input: String,
        recentMessages: List<MayraMessage> = emptyList(),
        priority: TaskPriority = TaskPriority.NORMAL
    ): OrchestrationResult {
        val normalizedInput = input.trim().replace(WHITESPACE_REGEX, " ")
        if (normalizedInput.isBlank()) {
            return OrchestrationResult.Rejected("Please say or type a command.")
        }

        var response: String? = null
        var failure: Throwable? = null
        val task = RuntimeTask(
            name = "assistant-turn",
            priority = priority,
            createdAt = clock()
        ) {
            try {
                response = commandEngine.respond(normalizedInput, recentMessages)
            } catch (error: Throwable) {
                failure = error
                throw error
            }
        }

        val taskId = runtimeKernel.submit(task)
        runtimeKernel.runNext()

        failure?.let {
            return OrchestrationResult.Failed(
                taskId = taskId,
                message = it.message ?: "Mayra could not process that request.",
                cause = it
            )
        }

        return OrchestrationResult.Completed(
            taskId = taskId,
            input = normalizedInput,
            response = requireNotNull(response) { "Runtime completed without a response" }
        )
    }

    suspend fun processVoiceTurn(
        current: VoiceConversationSnapshot,
        transcript: String,
        recentMessages: List<MayraMessage> = emptyList(),
        timestamp: Long = clock()
    ): VoiceOrchestrationResult {
        val processing = voiceEngine.reduce(
            current,
            VoiceConversationEvent.FinalTranscript(transcript, timestamp)
        )

        return when (val result = processText(
            input = processing.finalTranscript.orEmpty(),
            recentMessages = recentMessages,
            priority = TaskPriority.HIGH
        )) {
            is OrchestrationResult.Completed -> VoiceOrchestrationResult.Completed(
                orchestration = result,
                snapshot = voiceEngine.reduce(
                    processing,
                    VoiceConversationEvent.ResponseReady(result.response, clock())
                )
            )

            is OrchestrationResult.Rejected -> VoiceOrchestrationResult.Failed(
                orchestration = result,
                snapshot = voiceEngine.reduce(
                    processing,
                    VoiceConversationEvent.Fail(result.reason, clock())
                )
            )

            is OrchestrationResult.Failed -> VoiceOrchestrationResult.Failed(
                orchestration = result,
                snapshot = voiceEngine.reduce(
                    processing,
                    VoiceConversationEvent.Fail(result.message, clock())
                )
            )
        }
    }

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
    }
}

sealed interface OrchestrationResult {
    data class Completed(
        val taskId: String,
        val input: String,
        val response: String
    ) : OrchestrationResult

    data class Rejected(val reason: String) : OrchestrationResult

    data class Failed(
        val taskId: String,
        val message: String,
        val cause: Throwable
    ) : OrchestrationResult
}

sealed interface VoiceOrchestrationResult {
    val orchestration: OrchestrationResult
    val snapshot: VoiceConversationSnapshot

    data class Completed(
        override val orchestration: OrchestrationResult.Completed,
        override val snapshot: VoiceConversationSnapshot
    ) : VoiceOrchestrationResult

    data class Failed(
        override val orchestration: OrchestrationResult,
        override val snapshot: VoiceConversationSnapshot
    ) : VoiceOrchestrationResult
}
