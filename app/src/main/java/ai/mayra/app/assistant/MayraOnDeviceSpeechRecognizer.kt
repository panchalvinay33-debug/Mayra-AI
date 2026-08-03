package ai.mayra.app.assistant

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class MayraOnDeviceSpeechRecognizer(
    private val context: Context,
    private val onState: (MayraVoiceSessionState) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var localeCandidates: List<String> = emptyList()
    private var localeIndex = 0

    fun isAvailable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    fun start(): Boolean {
        stop()
        if (!isAvailable()) {
            onState(MayraVoiceSessionState.OnDeviceUnavailable)
            return false
        }

        localeCandidates = MayraSpeechLocalePolicy.candidates(
            MayraSpeechLocalePolicy.currentDeviceLocaleTag()
        )
        localeIndex = 0
        active = true
        onState(MayraVoiceSessionState.Preparing)
        return startCurrentLocale()
    }

    fun stop() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        localeCandidates = emptyList()
        localeIndex = 0
    }

    private fun startCurrentLocale(): Boolean {
        if (!active || localeIndex !in localeCandidates.indices) return false

        destroyRecognizer()
        val created = try {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } catch (_: UnsupportedOperationException) {
            active = false
            onState(MayraVoiceSessionState.OnDeviceUnavailable)
            return false
        } catch (_: RuntimeException) {
            active = false
            onState(MayraVoiceSessionState.Error("Speech recognizer unavailable"))
            return false
        }

        recognizer = created
        created.setRecognitionListener(listener)
        val languageTag = localeCandidates[localeIndex]
        val request = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        return try {
            created.startListening(request)
            true
        } catch (_: SecurityException) {
            active = false
            destroyRecognizer()
            onState(MayraVoiceSessionState.PermissionRequired)
            false
        } catch (_: RuntimeException) {
            active = false
            destroyRecognizer()
            onState(MayraVoiceSessionState.Error("Could not start listening"))
            false
        }
    }

    private fun retryNextLocale(): Boolean {
        if (!active || localeIndex + 1 >= localeCandidates.size) return false
        localeIndex += 1
        onState(MayraVoiceSessionState.Preparing)
        mainHandler.post {
            if (active) startCurrentLocale()
        }
        return true
    }

    private fun destroyRecognizer() {
        recognizer?.runCatching { cancel() }
        recognizer?.runCatching { destroy() }
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (active) onState(MayraVoiceSessionState.Listening)
        }

        override fun onBeginningOfSpeech() {
            if (active) onState(MayraVoiceSessionState.Listening)
        }

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            if (active) onState(MayraVoiceSessionState.Processing)
        }

        override fun onError(error: Int) {
            if (!active) return
            if (
                (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ||
                    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) &&
                retryNextLocale()
            ) {
                return
            }

            active = false
            destroyRecognizer()
            onState(MayraVoiceSessionState.Error(errorText(error)))
        }

        override fun onResults(results: Bundle?) {
            if (!active) return
            active = false
            val text = results.bestText()
            destroyRecognizer()
            onState(
                if (text.isNullOrBlank()) {
                    MayraVoiceSessionState.Error("I didn't catch that")
                } else {
                    MayraVoiceSessionState.Heard(text)
                }
            )
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!active) return
            val text = partialResults.bestText()
            if (!text.isNullOrBlank()) onState(MayraVoiceSessionState.Partial(text))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun Bundle?.bestText(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error"
        SpeechRecognizer.ERROR_CLIENT -> "Listening cancelled"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission needed"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech service network error"
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech recognizer unavailable"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
        SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many speech requests"
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Speech language not supported"
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Speech language unavailable"
        else -> "Speech recognition error ($code)"
    }
}
