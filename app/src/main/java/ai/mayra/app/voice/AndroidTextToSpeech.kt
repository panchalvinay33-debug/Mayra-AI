package ai.mayra.app.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Android TextToSpeech adapter with lifecycle-safe initialization and speech
 * callbacks that can be reflected in the chat UI.
 */
class AndroidTextToSpeech(
    context: Context,
    private val locale: Locale = Locale.getDefault(),
    private val onStateChanged: (VoiceState) -> Unit = {},
    private val onFinished: () -> Unit = {},
    private val onFailure: (String) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var engine: TextToSpeech? = TextToSpeech(appContext, this)
    private var isReady = false
    private var pendingText: String? = null

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            fail("Text-to-speech initialization failed.")
            return
        }

        val languageResult = engine?.setLanguage(locale) ?: TextToSpeech.ERROR
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            fail("The selected voice language is not supported.")
            return
        }

        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                onStateChanged(VoiceState.Speaking)
            }

            override fun onDone(utteranceId: String?) {
                onStateChanged(VoiceState.Idle)
                onFinished()
            }

            @Deprecated("Deprecated by Android, retained for older platform callbacks")
            override fun onError(utteranceId: String?) {
                fail("Mayra could not speak the response.")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                fail("Mayra could not speak the response (error $errorCode).")
            }
        })

        isReady = true
        pendingText?.let {
            pendingText = null
            speak(it)
        }
    }

    fun speak(text: String) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        if (!isReady) {
            pendingText = cleanText
            return
        }

        val result = engine?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            Bundle(),
            "mayra-${UUID.randomUUID()}"
        ) ?: TextToSpeech.ERROR

        if (result == TextToSpeech.ERROR) {
            fail("Mayra could not start speaking.")
        }
    }

    fun setSpeechRate(rate: Float) {
        engine?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
    }

    fun setPitch(pitch: Float) {
        engine?.setPitch(pitch.coerceIn(0.5f, 2.0f))
    }

    fun stop() {
        pendingText = null
        engine?.stop()
        onStateChanged(VoiceState.Idle)
    }

    fun shutdown() {
        pendingText = null
        isReady = false
        engine?.stop()
        engine?.shutdown()
        engine = null
        onStateChanged(VoiceState.Idle)
    }

    private fun fail(message: String) {
        isReady = false
        onStateChanged(VoiceState.Error(message))
        onFailure(message)
    }
}
