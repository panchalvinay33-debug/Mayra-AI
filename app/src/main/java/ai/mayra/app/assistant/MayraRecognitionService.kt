package ai.mayra.app.assistant

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Recognition-service contract required by Android's voice-assistant metadata.
 *
 * The Android assistant path can use Mayra's dedicated interaction/session audio pipeline.
 * Until the local wake-word/streaming recognizer is connected, external RecognitionService clients
 * receive a deterministic unavailable error instead of a fake transcript.
 */
class MayraRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback) {
        listener.safeError(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
    }

    override fun onStopListening(listener: Callback) = Unit

    override fun onCancel(listener: Callback) = Unit

    private fun Callback.safeError(code: Int) {
        runCatching { error(code) }
    }
}
