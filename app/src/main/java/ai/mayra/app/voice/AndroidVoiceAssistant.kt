package ai.mayra.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class AndroidVoiceAssistant(
    context: Context,
    private val onState: (VoiceState) -> Unit
) : VoiceAssistantContract, RecognitionListener, TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val recognizer = if (SpeechRecognizer.isRecognitionAvailable(appContext)) SpeechRecognizer.createSpeechRecognizer(appContext) else null
    private val tts = TextToSpeech(appContext, this)
    private var ttsReady = false

    init { recognizer?.setRecognitionListener(this) }

    override fun startListening() {
        val speechRecognizer = recognizer ?: run {
            onState(VoiceState(error = "Speech recognition is not available on this device"))
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        onState(VoiceState(isListening = true))
        speechRecognizer.startListening(intent)
    }

    override fun stopListening() { recognizer?.stopListening() }

    override fun speak(text: String) {
        if (ttsReady && text.isNotBlank()) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mayra-response")
    }

    override fun release() {
        recognizer?.destroy()
        tts.stop()
        tts.shutdown()
    }

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts.language = Locale.getDefault()
    }

    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        onState(VoiceState(isListening = false, transcript = text))
    }
    override fun onPartialResults(partialResults: Bundle?) {
        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
        onState(VoiceState(isListening = true, transcript = text))
    }
    override fun onError(error: Int) = onState(VoiceState(error = "Voice recognition error ($error)"))
    override fun onReadyForSpeech(params: Bundle?) = Unit
    override fun onBeginningOfSpeech() = Unit
    override fun onRmsChanged(rmsdB: Float) = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
}
