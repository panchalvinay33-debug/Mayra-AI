package ai.mayra.app.assistant

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Recognition-service contract required by Android's voice-assistant metadata.
 *
 * The Android 16 assistant path can use Mayra's dedicated interaction/session audio pipeline.
 * Until the local wake-word/streaming recognizer is connected, external RecognitionService clients
 * receive a deterministic unavailable error instead of a fake transcript.
 */
class MayraRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback) {
        listener.error(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
    }

    override fun onStopListening(listener: Callback) = Unit

    override fun onCancel(listener: Callback) = Unit

    override fun onCheckRecognitionSupport(
        recognizerIntent: Intent,
        supportCallback: android.speech.RecognitionSupportCallback
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            supportCallback.onError(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)
        }
    }

    override fun onTriggerModelDownload(recognizerIntent: Intent) = Unit

    private fun Callback.error(code: Int) {
        runCatching { error(code) }
    }
}
