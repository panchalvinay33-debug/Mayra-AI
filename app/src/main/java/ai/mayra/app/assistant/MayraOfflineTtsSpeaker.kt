package ai.mayra.app.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Zero-cost Android system TTS fallback.
 *
 * This remains the safe default until a neural voice pack passes license, latency, thermal,
 * battery and quality checks on the target Motorola device.
 */
class MayraOfflineTtsSpeaker(context: Context) : TextToSpeech.OnInitListener, MayraSpeechOutput {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private var ready = false
    private var pending: String? = null

    override fun onInit(status: Int) {
        val engine = tts ?: return
        ready = status == TextToSpeech.SUCCESS
        if (!ready) return

        engine.setSpeechRate(0.95f)
        engine.setPitch(1.0f)
        selectBestOfflineVoice(engine)
        pending?.let {
            pending = null
            speak(it)
        }
    }

    override fun speak(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        val engine = tts
        if (!ready || engine == null) {
            pending = clean
            return
        }
        engine.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "mayra-${System.nanoTime()}")
    }

    override fun stop() {
        pending = null
        tts?.stop()
    }

    override fun shutdown() {
        pending = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun selectBestOfflineVoice(engine: TextToSpeech) {
        val voices = engine.voices.orEmpty().filterNot(Voice::isNetworkConnectionRequired)
        val preferred = listOf(Locale("hi", "IN"), Locale("en", "IN"), Locale.US)
        val selected = preferred.firstNotNullOfOrNull { locale ->
            voices.firstOrNull { voice ->
                voice.locale.language.equals(locale.language, true) &&
                    (locale.country.isBlank() || voice.locale.country.equals(locale.country, true))
            }
        } ?: voices.firstOrNull()

        if (selected != null) {
            engine.voice = selected
        } else {
            engine.language = Locale("hi", "IN")
        }
    }
}
