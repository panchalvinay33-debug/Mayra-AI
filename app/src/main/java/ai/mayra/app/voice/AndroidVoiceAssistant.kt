package ai.mayra.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class AndroidVoiceAssistant(
    context: Context,
    private val onState: (AndroidVoiceState) -> Unit
) : VoiceAssistantContract, RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val recognizer = if (SpeechRecognizer.isRecognitionAvailable(appContext)) {
        SpeechRecognizer.createSpeechRecognizer(appContext)
    } else {
        null
    }
    private val tts = TextToSpeech(appContext, this)
    private var ttsReady = false

    init {
        recognizer?.setRecognitionListener(this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onState(AndroidVoiceState(isSpeaking = true))
            }

            override fun onDone(utteranceId: String?) {
                onState(AndroidVoiceState())
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onState(AndroidVoiceState(error = "Mayra could not speak the response."))
            }
        })
    }

    override fun startListening() {
        val speechRecognizer = recognizer ?: run {
            onState(AndroidVoiceState(error = "Speech recognition is not available on this device."))
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        onState(AndroidVoiceState(isListening = true))
        speechRecognizer.startListening(intent)
    }

    override fun stopListening() {
        recognizer?.stopListening()
        onState(AndroidVoiceState())
    }

    override fun speak(text: String) {
        if (text.isBlank()) return
        if (!ttsReady) {
            onState(AndroidVoiceState(error = "Text to speech is not ready yet."))
            return
        }

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mayra-response")
    }

    override fun release() {
        recognizer?.cancel()
        recognizer?.destroy()
        tts.stop()
        tts.shutdown()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (!ttsReady) {
            onState(AndroidVoiceState(error = "Text to speech could not be initialized."))
            return
        }

        val result = tts.setLanguage(Locale.getDefault())
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            onState(AndroidVoiceState(error = "The selected language is not supported for speech."))
        }
    }

    override fun onResults(results: Bundle?) {
        val text = results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        onState(
            AndroidVoiceState(
                isListening = false,
                transcript = text,
                isFinalTranscript = true
            )
        )
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()
        onState(AndroidVoiceState(isListening = true, transcript = text))
    }

    override fun onError(error: Int) {
        val message = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Microphone audio could not be captured."
            SpeechRecognizer.ERROR_CLIENT -> "Voice input was cancelled."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice recognition needs a working internet connection."
            SpeechRecognizer.ERROR_NO_MATCH -> "I could not understand that. Please try again."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition is busy. Please try again."
            SpeechRecognizer.ERROR_SERVER -> "The speech service is temporarily unavailable."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected."
            else -> "Voice recognition failed ($error)."
        }
        onState(AndroidVoiceState(error = message))
    }

    override fun onReadyForSpeech(params: Bundle?) {
        onState(AndroidVoiceState(isListening = true))
    }

    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
