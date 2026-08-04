package ai.mayra.app.j3

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.ResultReceiver
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * J3 deliberately hosts sherpa-onnx in a secondary process.
 *
 * A JNI/native abort cannot be caught by Kotlin. Keeping the neural runtime outside the launcher
 * process means an OEM/ABI/model native crash does not make the benchmark UI disappear. Progress
 * markers are sent before every risky native/model boundary so a process death still tells us the
 * last stage reached on the physical device.
 */
class J3NeuralTtsService : Service() {
    private val worker = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY
        val receiver = intent.getParcelableExtra<ResultReceiver>(EXTRA_RECEIVER)

        when (action) {
            ACTION_LOAD -> worker.execute { load(receiver) }
            ACTION_SPEAK -> {
                val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
                val speed = intent.getFloatExtra(EXTRA_SPEED, 1.0f)
                worker.execute { speak(text, speed, receiver) }
            }
            ACTION_STOP -> worker.execute {
                stopPlayback()
                send(receiver, RESULT_STOPPED, "Playback stopped")
            }
        }
        return START_NOT_STICKY
    }

    private fun load(receiver: ResultReceiver?) {
        if (tts != null) {
            send(receiver, RESULT_READY, "Neural model already loaded")
            return
        }

        val started = SystemClock.elapsedRealtimeNanos()
        val modelDir = "vits-piper-hi_IN-priyamvada-medium"
        val model = "$modelDir/hi_IN-priyamvada-medium.onnx"
        val tokens = "$modelDir/tokens.txt"
        val dataDir = "$modelDir/espeak-ng-data"

        send(
            receiver,
            RESULT_PROGRESS,
            "Stage 1/4 • process alive • ABI ${Build.SUPPORTED_ABIS.joinToString(",")}"
        )

        runCatching {
            requireAssetReadable(model)
            requireAssetReadable(tokens)
            val espeakEntries = assets.list(dataDir).orEmpty()
            check(espeakEntries.isNotEmpty()) { "espeak-ng-data is missing or empty" }
        }.onFailure { error ->
            send(receiver, RESULT_ERROR, "Asset check ${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}")
            return
        }

        send(receiver, RESULT_PROGRESS, "Stage 2/4 • model, tokens and espeak assets readable")

        val config = runCatching {
            OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = model,
                        tokens = tokens,
                        dataDir = dataDir
                    ),
                    numThreads = 2,
                    debug = true
                ),
                maxNumSentences = 1,
                silenceScale = 0.2f
            )
        }.getOrElse { error ->
            send(receiver, RESULT_ERROR, "Config ${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}")
            return
        }

        send(receiver, RESULT_PROGRESS, "Stage 3/4 • config built • entering sherpa native constructor")

        runCatching {
            OfflineTts(assetManager = assets, config = config)
        }.onSuccess { engine ->
            send(receiver, RESULT_PROGRESS, "Stage 4/4 • native constructor returned")
            tts = engine
            val loadMs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0
            send(
                receiver,
                RESULT_READY,
                "Load ${loadMs.roundToInt()} ms • ${engine.sampleRate()} Hz • speakers ${engine.numSpeakers()}"
            )
        }.onFailure { error ->
            send(receiver, RESULT_ERROR, "${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}")
        }
    }

    private fun requireAssetReadable(path: String) {
        assets.open(path).use { stream ->
            check(stream.read() >= 0) { "Asset is empty: $path" }
        }
    }

    private fun speak(text: String, speed: Float, receiver: ResultReceiver?) {
        val engine = tts
        if (engine == null) {
            send(receiver, RESULT_ERROR, "Neural engine is not loaded")
            return
        }
        if (text.isBlank()) {
            send(receiver, RESULT_ERROR, "No text to synthesize")
            return
        }

        val started = SystemClock.elapsedRealtimeNanos()
        runCatching {
            engine.generateWithConfig(
                text = text,
                config = GenerationConfig(sid = 0, speed = speed, silenceScale = 0.2f)
            )
        }.onSuccess { audio ->
            val generationMs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0
            val audioMs = if (audio.sampleRate > 0) audio.samples.size * 1000.0 / audio.sampleRate else 0.0
            val rtf = if (audioMs > 0.0) generationMs / audioMs else Double.NaN
            runCatching { play(audio.samples, audio.sampleRate) }
                .onSuccess {
                    send(
                        receiver,
                        RESULT_PLAYING,
                        "Generate ${generationMs.roundToInt()} ms • audio ${audioMs.roundToInt()} ms • RTF ${"%.2f".format(rtf)}"
                    )
                }
                .onFailure { error ->
                    send(receiver, RESULT_ERROR, "Playback ${error.javaClass.simpleName}: ${error.message.orEmpty().take(160)}")
                }
        }.onFailure { error ->
            send(receiver, RESULT_ERROR, "Synthesis ${error.javaClass.simpleName}: ${error.message.orEmpty().take(160)}")
        }
    }

    private fun play(samples: FloatArray, sampleRate: Int) {
        require(samples.isNotEmpty()) { "Generated audio is empty" }
        require(sampleRate > 0) { "Invalid sample rate" }
        stopPlayback()

        val pcm = ShortArray(samples.size) { index ->
            (samples[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes((pcm.size * 2).coerceAtLeast(4096))
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        audioTrack = track
        check(track.write(pcm, 0, pcm.size) >= 0) { "AudioTrack write failed" }
        track.play()
    }

    private fun stopPlayback() {
        audioTrack?.let { track ->
            runCatching { track.stop() }
            runCatching { track.flush() }
            runCatching { track.release() }
        }
        audioTrack = null
    }

    private fun send(receiver: ResultReceiver?, code: Int, message: String) {
        receiver?.send(code, android.os.Bundle().apply { putString(EXTRA_MESSAGE, message) })
    }

    override fun onDestroy() {
        stopPlayback()
        worker.shutdownNow()
        runCatching { tts?.release() }
        tts = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_LOAD = "ai.mayra.app.j3.LOAD"
        const val ACTION_SPEAK = "ai.mayra.app.j3.SPEAK"
        const val ACTION_STOP = "ai.mayra.app.j3.STOP"
        const val EXTRA_RECEIVER = "receiver"
        const val EXTRA_TEXT = "text"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_MESSAGE = "message"
        const val RESULT_READY = 1
        const val RESULT_PLAYING = 2
        const val RESULT_ERROR = 3
        const val RESULT_STOPPED = 4
        const val RESULT_PROGRESS = 5
    }
}
