package ai.mayra.app.core.voice

/**
 * Framework-independent conversation state machine for Mayra's voice surfaces.
 *
 * Android SpeechRecognizer/TextToSpeech adapters can feed events into this engine while
 * UI, accessibility, and tests observe deterministic state transitions.
 */
enum class VoiceConversationState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    PAUSED,
    ERROR
}

data class VoiceConversationSnapshot(
    val state: VoiceConversationState = VoiceConversationState.IDLE,
    val turnId: Long = 0L,
    val partialTranscript: String = "",
    val finalTranscript: String? = null,
    val responseText: String? = null,
    val errorMessage: String? = null,
    val updatedAt: Long = 0L
)

sealed interface VoiceConversationEvent {
    data class StartListening(val timestamp: Long) : VoiceConversationEvent
    data class PartialTranscript(val text: String, val timestamp: Long) : VoiceConversationEvent
    data class FinalTranscript(val text: String, val timestamp: Long) : VoiceConversationEvent
    data class ResponseReady(val text: String, val timestamp: Long) : VoiceConversationEvent
    data class SpeechFinished(val timestamp: Long) : VoiceConversationEvent
    data class Pause(val timestamp: Long) : VoiceConversationEvent
    data class Resume(val timestamp: Long) : VoiceConversationEvent
    data class Fail(val message: String, val timestamp: Long) : VoiceConversationEvent
    data class Reset(val timestamp: Long) : VoiceConversationEvent
}

class VoiceConversationEngine(
    private val maxTranscriptLength: Int = DEFAULT_MAX_TRANSCRIPT_LENGTH,
    private val maxResponseLength: Int = DEFAULT_MAX_RESPONSE_LENGTH
) {
    init {
        require(maxTranscriptLength > 0) { "maxTranscriptLength must be greater than zero" }
        require(maxResponseLength > 0) { "maxResponseLength must be greater than zero" }
    }

    fun reduce(
        current: VoiceConversationSnapshot,
        event: VoiceConversationEvent
    ): VoiceConversationSnapshot = when (event) {
        is VoiceConversationEvent.StartListening -> {
            require(current.state in setOf(
                VoiceConversationState.IDLE,
                VoiceConversationState.PAUSED,
                VoiceConversationState.ERROR
            )) { "Cannot start listening from ${current.state}" }

            VoiceConversationSnapshot(
                state = VoiceConversationState.LISTENING,
                turnId = current.turnId + 1,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.PartialTranscript -> {
            require(current.state == VoiceConversationState.LISTENING) {
                "Partial transcript requires LISTENING state"
            }
            current.copy(
                partialTranscript = event.text.normalized(maxTranscriptLength),
                errorMessage = null,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.FinalTranscript -> {
            require(current.state == VoiceConversationState.LISTENING) {
                "Final transcript requires LISTENING state"
            }
            val transcript = event.text.normalized(maxTranscriptLength)
            require(transcript.isNotBlank()) { "Final transcript cannot be blank" }

            current.copy(
                state = VoiceConversationState.PROCESSING,
                partialTranscript = "",
                finalTranscript = transcript,
                responseText = null,
                errorMessage = null,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.ResponseReady -> {
            require(current.state == VoiceConversationState.PROCESSING) {
                "Response requires PROCESSING state"
            }
            val response = event.text.normalized(maxResponseLength)
            require(response.isNotBlank()) { "Response cannot be blank" }

            current.copy(
                state = VoiceConversationState.SPEAKING,
                responseText = response,
                errorMessage = null,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.SpeechFinished -> {
            require(current.state == VoiceConversationState.SPEAKING) {
                "Speech completion requires SPEAKING state"
            }
            current.copy(
                state = VoiceConversationState.IDLE,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.Pause -> {
            require(current.state != VoiceConversationState.PAUSED) {
                "Conversation is already paused"
            }
            current.copy(
                state = VoiceConversationState.PAUSED,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.Resume -> {
            require(current.state == VoiceConversationState.PAUSED) {
                "Resume requires PAUSED state"
            }
            current.copy(
                state = VoiceConversationState.IDLE,
                updatedAt = event.timestamp
            )
        }

        is VoiceConversationEvent.Fail -> current.copy(
            state = VoiceConversationState.ERROR,
            errorMessage = event.message.normalized(MAX_ERROR_LENGTH)
                .ifBlank { "Unknown voice error" },
            updatedAt = event.timestamp
        )

        is VoiceConversationEvent.Reset -> VoiceConversationSnapshot(
            turnId = current.turnId,
            updatedAt = event.timestamp
        )
    }

    fun run(
        initial: VoiceConversationSnapshot = VoiceConversationSnapshot(),
        events: Iterable<VoiceConversationEvent>
    ): VoiceConversationSnapshot = events.fold(initial, ::reduce)

    private fun String.normalized(limit: Int): String =
        trim().replace(WHITESPACE_REGEX, " ").take(limit)

    companion object {
        const val DEFAULT_MAX_TRANSCRIPT_LENGTH = 1_000
        const val DEFAULT_MAX_RESPONSE_LENGTH = 4_000
        const val MAX_ERROR_LENGTH = 500
        private val WHITESPACE_REGEX = Regex("\\s+")
    }
}
