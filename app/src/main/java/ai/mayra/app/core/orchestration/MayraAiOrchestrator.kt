package ai.mayra.app.core.orchestration

import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.MayraMessage
import ai.mayra.app.core.execution.GoalExecutionEngine
import ai.mayra.app.core.execution.GoalSession
import ai.mayra.app.core.execution.GoalState
import ai.mayra.app.core.memory.LongTermMemoryEngine
import ai.mayra.app.core.memory.MemoryKind
import ai.mayra.app.core.planning.StepAction
import ai.mayra.app.core.runtime.RuntimeKernel
import ai.mayra.app.core.runtime.RuntimeTask
import ai.mayra.app.core.runtime.TaskPriority
import ai.mayra.app.core.voice.VoiceConversationEngine
import ai.mayra.app.core.voice.VoiceConversationEvent
import ai.mayra.app.core.voice.VoiceConversationSnapshot

/**
 * Coordinates user input, conversation context, runtime execution, goal planning, memory, and
 * voice-state transitions.
 *
 * This class deliberately contains no Android framework dependencies. UI, SpeechRecognizer,
 * TextToSpeech, and persistence adapters can call it from their own lifecycle-aware boundaries.
 */
class MayraAiOrchestrator(
    private val commandEngine: LocalCommandEngine = LocalCommandEngine(),
    private val runtimeKernel: RuntimeKernel = RuntimeKernel(),
    private val voiceEngine: VoiceConversationEngine = VoiceConversationEngine(),
    private val memoryEngine: LongTermMemoryEngine = LongTermMemoryEngine(),
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

    /**
     * Plans and executes a potentially multi-step goal through the same runtime used by normal
     * assistant turns. Successful and failed outcomes are recorded in long-term project memory.
     */
    suspend fun processGoal(
        goal: String,
        recentMessages: List<MayraMessage> = emptyList(),
        priority: TaskPriority = TaskPriority.NORMAL
    ): GoalOrchestrationResult {
        val normalizedGoal = goal.trim().replace(WHITESPACE_REGEX, " ")
        if (normalizedGoal.isBlank()) {
            return GoalOrchestrationResult.Rejected("Please provide a goal.")
        }

        val engine = GoalExecutionEngine(
            action = StepAction { step ->
                when (val result = processText(step.command, recentMessages, priority)) {
                    is OrchestrationResult.Completed -> result.response
                    is OrchestrationResult.Rejected -> error(result.reason)
                    is OrchestrationResult.Failed -> throw result.cause
                }
            },
            clock = clock
        )

        val submitted = engine.submit(normalizedGoal)
        val finished = requireNotNull(engine.runNext()) { "Submitted goal was not executed." }
        recordGoalOutcome(finished)

        return if (finished.state == GoalState.COMPLETED) {
            GoalOrchestrationResult.Completed(finished)
        } else {
            GoalOrchestrationResult.Failed(
                session = finished,
                message = finished.failureMessage ?: "Mayra could not complete that goal."
            )
        }
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

    fun memorySnapshot() = memoryEngine.snapshot()

    private fun recordGoalOutcome(session: GoalSession) {
        val timestamp = session.finishedAt ?: clock()
        val status = session.state.name.lowercase()
        val completed = session.report?.completedSteps ?: 0
        val total = session.plan?.steps?.size ?: 0
        val detail = buildString {
            append("status=").append(status)
            append("; completed=").append(completed).append('/').append(total)
            session.failureMessage?.let { append("; error=").append(it) }
        }

        memoryEngine.remember(
            namespace = GOAL_MEMORY_NAMESPACE,
            key = session.id,
            value = "${session.goal}; $detail",
            kind = MemoryKind.PROJECT,
            confidence = 1.0,
            timestamp = timestamp,
            source = "goal-execution"
        )
    }

    private companion object {
        const val GOAL_MEMORY_NAMESPACE = "goal_history"
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

sealed interface GoalOrchestrationResult {
    data class Completed(val session: GoalSession) : GoalOrchestrationResult
    data class Failed(val session: GoalSession, val message: String) : GoalOrchestrationResult
    data class Rejected(val reason: String) : GoalOrchestrationResult
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
