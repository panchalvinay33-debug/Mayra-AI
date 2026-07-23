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
    private val minimumTranscriptLength: Int = DEFAULT_MIN_TRANSCRIPT_LENGTH
) {
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
        if (assistantBusy) return VoiceLoopDecision(ignoreReason = "assistant_busy")

        val normalized = normalize(transcript)
        val duplicate = normalized == lastSubmittedTranscript && now - lastSubmittedAt <= duplicateWindowMs
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
        if (responseKey == lastSpokenResponseKey) return VoiceLoopDecision(ignoreReason = "response_already_spoken")
        lastSpokenResponseKey = responseKey
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
    }
}
