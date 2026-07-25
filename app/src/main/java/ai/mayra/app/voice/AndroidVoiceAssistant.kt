package ai.mayra.app.voice

import ai.mayra.app.settings.MayraSettings
import ai.mayra.app.settings.MayraSettingsStore
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
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID
import kotlin.math.max

class AndroidVoiceAssistant(
    context: Context,
    private val onState: (VoiceState) -> Unit
) : VoiceAssistantContract, RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val settingsStore = MayraSettingsStore(appContext)
    private val handler = Handler(Looper.getMainLooper())
    private val recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(appContext)
    private val recognizer = if (recognitionAvailable) SpeechRecognizer.createSpeechRecognizer(appContext) else null
    private val tts = TextToSpeech(appContext, this)

    private var settings: MayraSettings = settingsStore.read()
    private var state = VoiceState(speechAvailable = recognitionAvailable)
    private var released = false
    private var recognitionActive = false
    private var continuous = false
    private var listenAfterSpeech = false
    private var ttsReady = false
    private var restartAttempts = 0
    private var lastStartAt = 0L

    init {
        recognizer?.setRecognitionListener(this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                emitFromAnyThread(state.copy(transportState = VoiceTransportState.SPEAKING, isListening = false, isSpeaking = true, lastUtteranceId = id, error = null))
            }
            override fun onDone(id: String?) {
                handler.post {
                    if (released) return@post
                    emit(state.copy(transportState = VoiceTransportState.IDLE, isSpeaking = false, lastUtteranceId = id))
                    val reopen = listenAfterSpeech || continuous
                    listenAfterSpeech = false
                    if (reopen) scheduleStart(RESTART_AFTER_SPEECH_MS)
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) = onError(id, TextToSpeech.ERROR)
            override fun onError(id: String?, errorCode: Int) {
                handler.post {
                    fail("Text to speech error ($errorCode)", true)
                    val reopen = listenAfterSpeech || continuous
                    listenAfterSpeech = false
                    if (reopen) scheduleStart(RESTART_AFTER_ERROR_MS)
                }
            }
            override fun onStop(id: String?, interrupted: Boolean) {
                emitFromAnyThread(state.copy(transportState = if (interrupted) VoiceTransportState.INTERRUPTED else VoiceTransportState.IDLE, isSpeaking = false, lastUtteranceId = id))
            }
        })
        emit(state)
    }

    fun refreshVoiceSettings() {
        settings = settingsStore.read()
        if (ttsReady) configureVoice(settings.language.speechTag)
    }

    override fun startListening() {
        if (released || recognitionActive) return
        val speechRecognizer = recognizer
        if (!recognitionAvailable || speechRecognizer == null) {
            emit(state.copy(transportState = VoiceTransportState.UNAVAILABLE, speechAvailable = false, error = "Speech recognition is not available"))
            return
        }
        if (state.isSpeaking) tts.stop()
        val elapsed = System.currentTimeMillis() - lastStartAt
        if (elapsed in 0 until MIN_START_INTERVAL_MS) { scheduleStart(MIN_START_INTERVAL_MS - elapsed); return }
        recognitionActive = true
        lastStartAt = System.currentTimeMillis()
        emit(state.copy(transportState = VoiceTransportState.PREPARING, isListening = false, isSpeaking = false, transcript = "", partialTranscript = "", isFinalTranscript = false, recognitionConfidence = 0.0, error = null, recoverableError = false))
        runCatching { speechRecognizer.startListening(recognitionIntent()) }.onFailure {
            recognitionActive = false
            fail("Unable to start recognition: ${it.message.orEmpty()}", true)
            recover()
        }
    }

    override fun stopListening() {
        recognitionActive = false
        runCatching { recognizer?.stopListening() }
        emit(state.copy(transportState = VoiceTransportState.IDLE, isListening = false))
    }

    override fun speak(text: String, listenAfter: Boolean) {
        if (released || text.isBlank()) return
        val spoken = MayraSpeechTextPolicy.prepare(text)
        if (spoken.isBlank()) return
        settings = settingsStore.read()
        configureVoice(MayraSpeechTextPolicy.languageTag(spoken, settings.language))
        listenAfterSpeech = listenAfter
        recognitionActive = false
        runCatching { recognizer?.cancel() }
        if (!ttsReady) {
            fail("Text to speech is not ready", true)
            if (listenAfter || continuous) scheduleStart(RESTART_AFTER_ERROR_MS)
            return
        }
        val id = "mayra-${UUID.randomUUID()}"
        if (tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, id) == TextToSpeech.ERROR) {
            fail("Unable to speak response", true)
            if (listenAfter || continuous) scheduleStart(RESTART_AFTER_ERROR_MS)
        }
    }

    override fun interruptSpeech(resumeListening: Boolean) {
        listenAfterSpeech = false
        runCatching { tts.stop() }
        emit(state.copy(transportState = VoiceTransportState.INTERRUPTED, isSpeaking = false, isListening = false))
        if (resumeListening) scheduleStart(INTERRUPT_DELAY_MS)
    }

    override fun setContinuousMode(enabled: Boolean) {
        continuous = enabled
        emit(state.copy(continuousMode = enabled))
        if (enabled && !state.isListening && !state.isSpeaking && !recognitionActive) startListening()
        if (!enabled) {
            listenAfterSpeech = false
            recognitionActive = false
            handler.removeCallbacks(startRunnable)
            runCatching { recognizer?.cancel() }
            emit(state.copy(transportState = VoiceTransportState.IDLE, isListening = false, continuousMode = false))
        }
    }

    override fun release() {
        if (released) return
        released = true
        handler.removeCallbacksAndMessages(null)
        recognitionActive = false
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        runCatching { tts.stop() }
        runCatching { tts.shutdown() }
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) configureVoice(settings.language.speechTag)
        emit(state.copy(ttsReady = ttsReady, error = if (ttsReady) state.error else "Text to speech initialization failed"))
    }

    private fun configureVoice(languageTag: String) {
        if (!ttsReady) return
        val locale = Locale.forLanguageTag(languageTag)
        val available = tts.isLanguageAvailable(locale)
        if (available >= TextToSpeech.LANG_AVAILABLE) tts.language = locale
        else tts.language = Locale.getDefault()
        if (settings.preferHighQualityOfflineVoice) selectBestVoice(locale)?.let { tts.voice = it }
        tts.setSpeechRate(settings.normalizedVoiceRate)
        tts.setPitch(settings.normalizedVoicePitch)
    }

    private fun selectBestVoice(locale: Locale): Voice? = runCatching {
        tts.voices.orEmpty()
            .filter { voice -> voice.locale.language.equals(locale.language, true) }
            .sortedWith(compareByDescending<Voice> { !it.isNetworkConnectionRequired }
                .thenByDescending { it.quality }
                .thenBy { it.latency }
                .thenBy { it.name })
            .firstOrNull()
    }.getOrNull()

    override fun onReadyForSpeech(params: Bundle?) { restartAttempts = 0; emit(state.copy(transportState = VoiceTransportState.LISTENING, isListening = true, isSpeaking = false, error = null)) }
    override fun onBeginningOfSpeech() = emit(state.copy(transportState = VoiceTransportState.LISTENING, isListening = true))
    override fun onRmsChanged(rmsdB: Float) = emit(state.copy(rmsDb = max(0f, rmsdB)))
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = emit(state.copy(transportState = VoiceTransportState.PROCESSING, isListening = false))
    override fun onPartialResults(results: Bundle?) {
        val text = recognitionTexts(results).firstOrNull().orEmpty().trim()
        emit(state.copy(transportState = VoiceTransportState.LISTENING, isListening = true, transcript = text, partialTranscript = text, isFinalTranscript = false))
    }
    override fun onResults(results: Bundle?) {
        recognitionActive = false
        val text = recognitionTexts(results).firstOrNull().orEmpty().trim()
        val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull()?.toDouble()?.takeIf { it >= 0.0 } ?: 0.72
        emit(state.copy(transportState = VoiceTransportState.IDLE, isListening = false, transcript = text, partialTranscript = text, isFinalTranscript = text.isNotBlank(), recognitionConfidence = confidence.coerceIn(0.0, 1.0), error = null))
        if (text.isBlank()) recover()
    }
    override fun onError(error: Int) {
        recognitionActive = false
        val failure = recognitionFailure(error)
        if (failure.silent) emit(state.copy(transportState = VoiceTransportState.IDLE, isListening = false, error = null, recoverableError = true)) else fail(failure.message, failure.recoverable)
        if (failure.recoverable) recover()
    }
    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun recognitionIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 950L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 450L)
    }

    private fun recognitionTexts(bundle: Bundle?) = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
    private fun recover() {
        if (!continuous || released) return
        restartAttempts++
        if (restartAttempts > 5) { continuous = false; fail("Voice loop paused after repeated failures", true); emit(state.copy(continuousMode = false)); return }
        scheduleStart((500L * restartAttempts).coerceAtMost(3_000L))
    }
    private fun scheduleStart(delay: Long) {
        if (released || !continuous) return
        handler.removeCallbacks(startRunnable)
        handler.postDelayed(startRunnable, delay.coerceAtLeast(0L))
    }
    private val startRunnable = Runnable { if (!released && continuous && !state.isSpeaking && !recognitionActive) startListening() }
    private fun fail(message: String, recoverable: Boolean) = emit(state.copy(transportState = VoiceTransportState.ERROR, isListening = false, isSpeaking = false, error = message, recoverableError = recoverable))
    private fun emitFromAnyThread(next: VoiceState) { handler.post { emit(next) } }
    private fun emit(next: VoiceState) {
        state = next.copy(continuousMode = continuous, speechAvailable = recognitionAvailable, ttsReady = ttsReady)
        if (!released) onState(state)
    }
    private fun languageTag(): String = settingsStore.read().language.speechTag
    private fun recognitionFailure(error: Int): Failure = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Failure("Microphone permission is required", false)
        SpeechRecognizer.ERROR_AUDIO -> Failure("Microphone audio error", true)
        SpeechRecognizer.ERROR_NETWORK -> Failure("Voice recognition network error", true)
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Failure("Voice recognition network timeout", true)
        SpeechRecognizer.ERROR_NO_MATCH -> Failure("No clear speech detected", true, continuous)
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Failure("Voice recognizer is busy", true, true)
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Failure("Listening timed out", true, continuous)
        SpeechRecognizer.ERROR_SERVER -> Failure("Voice recognition service error", true)
        else -> Failure("Voice recognition error ($error)", true)
    }
    private data class Failure(val message: String, val recoverable: Boolean, val silent: Boolean = false)
    companion object {
        private const val MIN_START_INTERVAL_MS = 450L
        private const val RESTART_AFTER_SPEECH_MS = 280L
        private const val RESTART_AFTER_ERROR_MS = 700L
        private const val INTERRUPT_DELAY_MS = 180L
    }
}