package ai.mayra.app.voice

/**
 * Pure decision layer for the Android voice UI. Keeping this free of Android APIs makes the
 * turn-taking rules deterministic and unit-testable.
 */
data class VoiceLoopDecision(
    val submitTranscript: String? = null,
    val speakResponse: String? = null,
    val listenAfterSpeech: Boolean = false,
    val startListening: Boolean = false,
    val stopListening: Boolean = false,
    val ignoreReason: String? = null
)

class RealtimeVoiceLoopPolicy(
    private val duplicateWindowMs: Long = DEFAULT_DUPLICATE_WINDOW_MS,
    private val minimumTranscriptLength: Int = DEFAULT_MIN_TRANSCRIPT_LENGTH,
    private val maximumTranscriptLength: Int = DEFAULT_MAX_TRANSCRIPT_LENGTH,
    private val maximumResponseLength: Int = DEFAULT_MAX_RESPONSE_LENGTH
) {
    init {
        require(duplicateWindowMs >= 0L)
        require(minimumTranscriptLength > 0)
        require(maximumTranscriptLength >= minimumTranscriptLength)
        require(maximumResponseLength > 0)
    }

    private var lastSubmittedTranscript = ""
    private var lastSubmittedAt = 0L
    private var lastSpokenResponseKey = ""

    @Synchronized
    fun onVoiceState(
        state: VoiceState,
        assistantBusy: Boolean,
        now: Long = System.currentTimeMillis()
    ): VoiceLoopDecision {
        if (!state.isFinalTranscript) return VoiceLoopDecision(ignoreReason = "not_final")
        val transcript = state.transcript.trim()
        if (transcript.length < minimumTranscriptLength) {
            return VoiceLoopDecision(ignoreReason = "transcript_too_short")
        }
        if (transcript.length > maximumTranscriptLength) {
            return VoiceLoopDecision(stopListening = true, ignoreReason = "transcript_too_long")
        }
        if (assistantBusy) return VoiceLoopDecision(ignoreReason = "assistant_busy")

        val normalized = normalize(transcript)
        if (normalized.isBlank()) return VoiceLoopDecision(ignoreReason = "transcript_empty_after_normalization")
        val elapsed = (now - lastSubmittedAt).coerceAtLeast(0L)
        val duplicate = normalized == lastSubmittedTranscript && elapsed <= duplicateWindowMs
        if (duplicate) return VoiceLoopDecision(ignoreReason = "duplicate_transcript")

        lastSubmittedTranscript = normalized
        lastSubmittedAt = now
        return VoiceLoopDecision(submitTranscript = transcript, stopListening = true)
    }

    @Synchronized
    fun onAssistantResponse(
        responseText: String,
        responseKey: String,
        continuousMode: Boolean
    ): VoiceLoopDecision {
        val text = responseText.trim()
        if (text.isBlank()) return VoiceLoopDecision(ignoreReason = "blank_response")
        if (text.length > maximumResponseLength) {
            return VoiceLoopDecision(ignoreReason = "response_too_long")
        }
        val stableKey = responseKey.trim().ifBlank { "text:${normalize(text)}" }
        if (stableKey == lastSpokenResponseKey) {
            return VoiceLoopDecision(ignoreReason = "response_already_spoken")
        }
        lastSpokenResponseKey = stableKey
        return VoiceLoopDecision(
            speakResponse = text,
            listenAfterSpeech = continuousMode
        )
    }

    @Synchronized
    fun onAssistantFailure(continuousMode: Boolean): VoiceLoopDecision =
        VoiceLoopDecision(startListening = continuousMode)

    @Synchronized
    fun reset() {
        lastSubmittedTranscript = ""
        lastSubmittedAt = 0L
        lastSpokenResponseKey = ""
    }

    private fun normalize(value: String): String = value
        .lowercase()
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    companion object {
        const val DEFAULT_DUPLICATE_WINDOW_MS = 8_000L
        const val DEFAULT_MIN_TRANSCRIPT_LENGTH = 2
        const val DEFAULT_MAX_TRANSCRIPT_LENGTH = 2_000
        const val DEFAULT_MAX_RESPONSE_LENGTH = 8_000
    }
}
