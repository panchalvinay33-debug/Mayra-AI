package ai.mayra.app.voice

/**
 * Coordinates speech-recognition text with the command processor while keeping
 * Android framework classes out of the ViewModel.
 */
class VoiceSessionManager(
    private val processor: VoiceCommandProcessor = VoiceCommandProcessor(),
    private val requireWakeWord: Boolean = false
) {
    private var state: VoiceState = VoiceState.Idle

    fun currentState(): VoiceState = state

    fun startListening(): VoiceState = setState(VoiceState.Listening)

    fun stopListening(): VoiceState = setState(VoiceState.Idle)

    fun beginSpeaking(): VoiceState = setState(VoiceState.Speaking)

    fun finishSpeaking(): VoiceState = setState(VoiceState.Idle)

    fun fail(message: String): VoiceState =
        setState(VoiceState.Error(message.ifBlank { "Voice interaction failed." }))

    fun processTranscript(rawText: String): VoiceSessionResult {
        state = VoiceState.Processing

        return when (val result = processor.process(rawText)) {
            is VoiceCommandResult.Accepted -> {
                if (requireWakeWord && !result.usedWakeWord) {
                    state = VoiceState.Listening
                    VoiceSessionResult.Ignored("Say Hey Mayra before the command.")
                } else {
                    state = VoiceState.Idle
                    VoiceSessionResult.Command(result.command, result.usedWakeWord)
                }
            }

            is VoiceCommandResult.Rejected -> {
                state = VoiceState.Error(result.reason)
                VoiceSessionResult.Ignored(result.reason)
            }
        }
    }

    private fun setState(newState: VoiceState): VoiceState {
        state = newState
        return newState
    }
}

sealed interface VoiceSessionResult {
    data class Command(
        val text: String,
        val usedWakeWord: Boolean
    ) : VoiceSessionResult

    data class Ignored(val reason: String) : VoiceSessionResult
}
