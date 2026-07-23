package ai.mayra.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class AndroidVoiceAssistant(
    context: Context,
    private val onState: (VoiceState) -> Unit
) : VoiceAssistantContract, RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val recognizerAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
    private val recognizer = if (recognizerAvailable) SpeechRecognizer.createSpeechRecognizer(appContext) else null
    private val tts = TextToSpeech(appContext, this)

    private var state = VoiceState(speechAvailable = recognizerAvailable)
    private var released = false
    private var ttsReady = false
    private var continuousMode = false
    private var restartAfterSpeech = false
    private var recognitionActive = false
    private var lastStartAt = 0L
    private var restartAttempts = 0

    init {
        recognizer?.setRecognitionListener(this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                publish(
                    state.copy(
                        transportState = VoiceTransportState.SPEAKING,
                        isSpeaking = true,
                        isListening = false,
                        lastUtteranceId = utteranceId,
                        error = null
                    )
                )
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    if (released) return@post
                    publish(
                        state.copy(
                            transportState = VoiceTransportState.IDLE,
                            isSpeaking = false,
                            lastUtteranceId = utteranceId
                        )
                    )
                    val shouldListen = restartAfterSpeech || continuousMode
                    restartAfterSpeech = false
                    if (shouldListen) scheduleListening(RESTART_AFTER_TTS_MS)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onError(utteranceId, TextToSpeech.ERROR)
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                mainHandler.post {
                    publishError("Text to speech error ($errorCode)", recoverable = true)
                    val shouldListen = restartAfterSpeech || continuousMode
                    restartAfterSpeech = false
                    if (shouldListen) scheduleListening(RESTART_AFTER_ERROR_MS)
                }
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                mainHandler.post {
                    publish(
                        state.copy(
                            transportState = if (interrupted) VoiceTransportState.INTERRUPTED else VoiceTransportState.IDLE,
                            isSpeaking = false,
                            lastUtteranceId = utteranceId
                        )
                    )
                }
            }
        })
        publish(state)
    }

    override fun startListening() {
        if (released) return
        if (!recognizerAvailable || recognizer == null) {
            publish(
                state.copy(
                    transportState = VoiceTransportState.UNAVAILABLE,
                    speechAvailable = false,
                    error = "Speech recognition is not available on this device"
                )
            )
            return
        }
        if (recognitionActive) return
        if (state.isSpeaking) tts.stop()

        val now = System.currentTimeMillis()
        if (now - lastStartAt < MIN_START_INTERVAL_MS) {
            scheduleListening(MIN_START_INTERVAL_MS - (now - lastStartAt))
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, preferredLanguageTag())
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, preferredLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_RECOGNITION_RESULTS)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, COMPLETE_SILENCE_MS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, POSSIBLY_COMPLETE_SILENCE_MS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MINIMUM_INPUT_MS)
        }

        recognitionActive = true
        lastStartAt = now
        publish(
            state.copy(
                transportState = VoiceTransportState.PREPARING,
                isListening = false,
                isSpeaking = false,
                transcript = "",
                partialTranscript = "",
                isFinalTranscript = false,
                recognitionConfidence = 0.0,
                error = null,
                recoverableError = false
            )
        )
        runCatching { recognizer.startListening(intent) }
            .onFailure {
                recognitionActive = false
                publishError("Unable to start voice recognition: ${it.message.orEmpty()}", recoverable = true)
                recoverIfContinuous()
            }
    }

    override fun stopListening() {
        continuousMode = false
        restartAfterSpeech = false
        recognitionActive = false
        runCatching { recognizer?.stopListening() }
        publish(
            state.copy(
                transportState = VoiceTransportState.IDLE,
                isListening = false,
                continuousMode = false
            )
        )
    }

    override fun speak(text: String, listenAfter: Boolean) {
        if (released || text.isBlank()) return
        restartAfterSpeech = listenAfter
        recognitionActive = false
        runCatching { recognizer?.cancel() }
        if (!ttsReady) {
            publishError("Text to speech is not ready yet", recoverable = true)
            if (listenAfter || continuousMode) scheduleListening(RESTART_AFTER_ERROR_MS)
            return
        }
        val utteranceId = "mayra-${UUID.randomUUID()}"
        val result = tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result == TextToSpeech.ERROR) {
            publishError("Unable to speak the response", recoverable = true)
            if (listenAfter || continuousMode) scheduleListening(RESTART_AFTER_ERROR_MS)
        }
    }

    override fun interruptSpeech(resumeListening: Boolean) {
        restartAfterSpeech = false
        runCatching { tts.stop() }
        publish(
            state.copy(
                transportState = VoiceTransportState.INTERRUPTED,
                isSpeaking = false,
                isListening = false
            )
        )
        if (resumeListening) scheduleListening(INTERRUPT_TO_LISTEN_MS)
    }

    override fun setContinuousMode(enabled: Boolean) {
        continuousMode = enabled
        publish(state.copy(continuousMode = enabled))
        if (enabled && !state.isListening && !state.isSpeaking) startListening()
        if (!enabled && state.isListening) stopListening()
    }

    override fun release() {
        if (released) return
        released = true
        mainHandler.removeCallbacksAndMessages(null)
        recognitionActive = false
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) {
            val preferred = Locale.forLanguageTag(preferredLanguageTag())
            val support = tts.isLanguageAvailable(preferred)
            tts.language = if (support >= TextToSpeech.LANG_AVAILABLE) preferred else Locale.getDefault()
            tts.setSpeechRate(DEFAULT_SPEECH_RATE)
            tts.setPitch(DEFAULT_PITCH)
        }
        publish(
            state.copy(
                ttsReady = ttsReady,
                error = if (ttsReady) state.error else "Text to speech initialization failed",
                recoverableError = !ttsReady
            )
        )
    }

    override fun onReadyForSpeech(params: Bundle?) {
        restartAttempts = 0
        publish(
            state.copy(
                transportState = VoiceTransportState.LISTENING,
                isListening = true,
                isSpeaking = false,
                error = null,
                recoverableError = false
            )
        )
    }

    override fun onBeginningOfSpeech() {
        publish(state.copy(transportState = VoiceTransportState.LISTENING, isListening = true))
    }

    override fun onRmsChanged(rmsdB: Float) {
        publish(state.copy(rmsDb = max(0f, rmsdB)))
    }

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        publish(state.copy(transportState = VoiceTransportState.PROCESSING, isListening = false))
    }

    override fun onResults(results: Bundle?) {
        recognitionActive = false
        val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        val best = candidates.firstOrNull().orEmpty().trim()
        val confidence = confidences?.firstOrNull()?.toDouble()?.takeIf { it >= 0.0 } ?: DEFAULT_CONFIDENCE
        publish(
            state.copy(
                transportState = VoiceTransportState.IDLE,
                isListening = false,
                transcript = best,
                partialTranscript = best,
                isFinalTranscript = best.isNotBlank(),
                recognitionConfidence = confidence.coerceIn(0.0, 1.0),
                error = null,
                recoverableError = false
            )
        )
        if (best.isBlank()) recoverIfContinuous()
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull().orEmpty().trim()
        publish(
            state.copy(
                transportState = VoiceTransportState.LISTENING,
                isListening = true,
                partialTranscript = text,
                transcript = text,
                isFinalTranscript = false
            )
        )
    }

    override fun onError(error: Int) {
        recognitionActive = false
        val mapped = mapRecognitionError(error)
        if (mapped.silent) {
            publish(
                state.copy(
                    transportState = VoiceTransportState.IDLE,
                    isListening = false,
                    error = null,
                    recoverableError = true
                )
            )
        } else {
            publishError(mapped.message, mapped.recoverable)
        }
        if (mapped.recoverable) recoverIfContinuous()
    }

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun recoverIfContinuous() {
        if (!continuousMode || released) return
        restartAttempts++
        if (restartAttempts > MAX_RESTART_ATTEMPTS) {
            continuousMode = false
            publishError("Voice loop paused after repeated recognition failures", recoverable = true)
            publish(state.copy(continuousMode = false))
            return
        }
        val delay = (RESTART_BASE_DELAY_MS * restartAttempts).coerceAtMost(RESTART_MAX_DELAY_MS)
        scheduleListening(delay)
    }

    private fun scheduleListening(delayMs: Long) {
        if (released) return
        mainHandler.removeCallbacks(startListeningRunnable)
        mainHandler.postDelayed(startListeningRunnable, delayMs.coerceAtLeast(0L))
    }

    private val startListeningRunnable = Runnable {
        if (!released && !state.isSpeaking && !recognitionActive) startListening()
    }

    private fun publishError(message: String, recoverable: Boolean) {
        publish(
            state.copy(
                transportState = VoiceTransportState.ERROR,
                isListening = false,
                isSpeaking = false,
                error = message,
                recoverableError = recoverable
            )
        )
    }

    private fun publish(next: VoiceState) {
        state = next.copy(
            continuousMode = continuousMode,
            speechAvailable = recognizerAvailable,
            ttsReady = ttsReady
        )
        mainHandler.post { if (!released) onState(state) }
    }

    private fun preferredLanguageTag(): String {
        val locale = Locale.getDefault()
        return when (locale.language.lowercase(Locale.ROOT)) {
            "hi" -> "hi-IN"
            "en" -> if (locale.country.equals("IN", true)) "en-IN" else locale.toLanguageTag()
            else -> locale.toLanguageTag().ifBlank { "hi-IN" }
        }
    }

    private fun mapRecognitionError(error: Int): RecognitionFailure = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> RecognitionFailure("Microphone audio error", true)
        SpeechRecognizer.ERROR_CLIENT -> RecognitionFailure("Voice recognition client error", true)
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> RecognitionFailure("Microphone permission is required", false)
        SpeechRecognizer.ERROR_NETWORK -> RecognitionFailure("Voice recognition network error", true)
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> RecognitionFailure("Voice recognition network timeout", true)
        SpeechRecognizer.ERROR_NO_MATCH -> RecognitionFailure("No clear speech detected", true, silent = continuousMode)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> RecognitionFailure("Voice recognizer is busy", true, silent = true)
        SpeechRecognizer.ERROR_SERVER -> RecognitionFailure("Voice recognition service error", true)
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> RecognitionFailure("Listening timed out", true, silent = continuousMode)
        else -> RecognitionFailure("Voice recognition error ($error)", true)
    }

    private data class RecognitionFailure(
        val message: String,
        val recoverable: Boolean,
        val silent: Boolean = false
    )

    companion object {
        private const val MAX_RECOGNITION_RESULTS = 3
        private const val COMPLETE_SILENCE_MS = 950L
        private const val POSSIBLY_COMPLETE_SILENCE_MS = 600L
        private const val MINIMUM_INPUT_MS = 450L
        private const val MIN_START_INTERVAL_MS = 450L
        private const val RESTART_AFTER_TTS_MS = 280L
        private const val RESTART_AFTER_ERROR_MS = 700L
        private const val INTERRUPT_TO_LISTEN_MS = 180L
        private const val RESTART_BASE_DELAY_MS = 500L
        private const val RESTART_MAX_DELAY_MS = 3_000L
        private const val MAX_RESTART_ATTEMPTS = 5
        private const val DEFAULT_CONFIDENCE = 0.72
        private const val DEFAULT_SPEECH_RATE = 0.96f
        private const val DEFAULT_PITCH = 1.02f
    }
}
