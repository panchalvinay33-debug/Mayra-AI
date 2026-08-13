package ai.mayra.app.assistant

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi

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

        if (!ensureRecognizer()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            probeInstalledLanguagesAndStart()
            true
        } else {
            startCurrentLocale()
        }
    }

    fun stop() {
        active = false
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
        localeCandidates = emptyList()
        localeIndex = 0
    }

    private fun ensureRecognizer(): Boolean {
        if (recognizer != null) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            active = false
            onState(MayraVoiceSessionState.OnDeviceUnavailable)
            return false
        }

        val created = try {
            createOnDeviceRecognizerApi31()
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
        return true
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createOnDeviceRecognizerApi31(): SpeechRecognizer =
        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun probeInstalledLanguagesAndStart() {
        val created = recognizer ?: return
        val probeLocale = localeCandidates.firstOrNull() ?: "en-IN"
        val probeIntent = recognitionRequest(probeLocale)

        try {
            created.checkRecognitionSupport(
                probeIntent,
                context.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        if (!active) return
                        val installed = MayraSpeechLocalePolicy.installedCandidates(
                            preferred = localeCandidates,
                            installed = recognitionSupport.installedOnDeviceLanguages
                        )
                        if (installed.isNotEmpty()) {
                            localeCandidates = installed
                            localeIndex = 0
                            startCurrentLocale()
                            return
                        }

                        val downloadable = recognitionSupport.supportedOnDeviceLanguages
                        active = false
                        destroyRecognizer()
                        onState(
                            MayraVoiceSessionState.Error(
                                if (downloadable.isNotEmpty()) {
                                    "On-device speech language pack needed"
                                } else {
                                    "No installed on-device speech language"
                                }
                            )
                        )
                    }

                    override fun onError(error: Int) {
                        if (!active) return
                        // Some OEM recognizers cannot report support. Fall back to a bounded
                        // locale trial while reusing the same recognizer instance.
                        startCurrentLocale()
                    }
                }
            )
        } catch (_: RuntimeException) {
            if (active) startCurrentLocale()
        }
    }

    private fun startCurrentLocale(): Boolean {
        if (!active || localeIndex !in localeCandidates.indices) return false
        val created = recognizer ?: if (ensureRecognizer()) recognizer else null
        if (created == null) return false

        val languageTag = localeCandidates[localeIndex]
        val request = recognitionRequest(languageTag)

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

    private fun recognitionRequest(languageTag: String): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

    private fun retryNextLocale(): Boolean {
        if (!active || localeIndex + 1 >= localeCandidates.size) return false
        localeIndex += 1
        onState(MayraVoiceSessionState.Preparing)
        recognizer?.runCatching { cancel() }
        mainHandler.postDelayed(
            {
                if (active) startCurrentLocale()
            },
            RETRY_DELAY_MS
        )
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

    private companion object {
        const val RETRY_DELAY_MS = 450L
    }
}
