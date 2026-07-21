package ai.mayra.app.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

/**
 * Thin Android SpeechRecognizer adapter. It owns the platform recognizer and
 * forwards final transcripts and readable errors to the voice session layer.
 */
class AndroidSpeechRecognizer(
    context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val onStateChanged: (VoiceState) -> Unit = {},
    private val onTranscript: (String) -> Unit,
    private val onFailure: (String) -> Unit
) : RecognitionListener {

    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            fail("Speech recognition is not available on this device.")
            return
        }

        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            fail("Microphone permission is required.")
            return
        }

        if (isListening) return

        val speechRecognizer = recognizer ?: SpeechRecognizer
            .createSpeechRecognizer(appContext)
            .also {
                it.setRecognitionListener(this)
                recognizer = it
            }

        isListening = true
        onStateChanged(VoiceState.Listening)
        speechRecognizer.startListening(createRecognizerIntent())
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        recognizer?.stopListening()
        onStateChanged(VoiceState.Idle)
    }

    fun cancel() {
        isListening = false
        recognizer?.cancel()
        onStateChanged(VoiceState.Idle)
    }

    fun destroy() {
        isListening = false
        recognizer?.destroy()
        recognizer = null
        onStateChanged(VoiceState.Idle)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        onStateChanged(VoiceState.Listening)
    }

    override fun onBeginningOfSpeech() = Unit

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        isListening = false
        onStateChanged(VoiceState.Processing)
    }

    override fun onError(error: Int) {
        isListening = false
        fail(errorMessage(error))
    }

    override fun onResults(results: Bundle?) {
        isListening = false
        val transcript = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()

        if (transcript.isNullOrEmpty()) {
            fail("No speech was detected.")
            return
        }

        onStateChanged(VoiceState.Processing)
        onTranscript(transcript)
    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

    private fun createRecognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    private fun fail(message: String) {
        onStateChanged(VoiceState.Error(message))
        onFailure(message)
    }

    private fun errorMessage(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was cancelled."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network error while recognizing speech."
        SpeechRecognizer.ERROR_NO_MATCH -> "I could not understand that. Please try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Please try again."
        SpeechRecognizer.ERROR_SERVER -> "Speech recognition service error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected."
        else -> "Speech recognition failed (error $code)."
    }
}
