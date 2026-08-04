package ai.mayra.app.j3

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.mayra.app.ui.theme.MayraAITheme
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * Zero-permission, zero-network neural Hindi TTS benchmark.
 *
 * The model and sherpa-onnx AAR are injected by the dedicated CI workflow. This package does not
 * exercise Mayra actions, STT, contacts, reminders or background services; it exists only to let
 * the owner compare naturalness and on-device generation latency with Android system TTS.
 */
class J3NeuralTtsTestActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private var tts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null

    private var ready by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var status by mutableStateOf("Loading free offline Hindi neural voice…")
    private var lastMetrics by mutableStateOf("No synthesis yet")
    private var speed by mutableStateOf(1.0f)

    private val phrases = listOf(
        "नमस्ते, मैं मायरा हूँ। मैं आपकी बात सुन रही हूँ।",
        "कल सुबह सात बजे मुझे दवा लेने की याद दिलाना।",
        "आज मौसम कैसा है और बारिश होने की संभावना कितनी है?",
        "WhatsApp खोलने से पहले मैं आपसे पुष्टि करूँगी।",
        "Hello, main Mayra hoon. Aap bataiye main kya help kar sakti hoon?",
        "आज चार अगस्त है और समय नौ बजकर बत्तीस मिनट है।"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Mayra J3 Neural Voice Test",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Free • offline • zero permissions • benchmark only")
                        Spacer(Modifier.height(12.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Text(lastMetrics, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(16.dp))

                        phrases.forEachIndexed { index, phrase ->
                            Button(
                                onClick = { synthesize(phrase) },
                                enabled = ready && !busy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${index + 1}. ${phrase.take(34)}${if (phrase.length > 34) "…" else ""}")
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    speed = 0.92f
                                    status = "Speed 0.92× — softer/slower comparison"
                                },
                                enabled = !busy
                            ) { Text("0.92×") }
                            Button(
                                onClick = {
                                    speed = 1.0f
                                    status = "Speed 1.00× — model default"
                                },
                                enabled = !busy
                            ) { Text("1.00×") }
                            Button(
                                onClick = ::stopPlayback,
                                enabled = ready
                            ) { Text("Stop") }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Listening goal: Hindi naturalness, Hinglish clarity, names/numbers and delay. This candidate is not approved for production distribution yet.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        loadModel()
    }

    override fun onStop() {
        stopPlayback()
        super.onStop()
    }

    override fun onDestroy() {
        stopPlayback()
        worker.shutdownNow()
        tts?.release()
        tts = null
        super.onDestroy()
    }

    private fun loadModel() {
        worker.execute {
            runCatching {
                val modelDir = "vits-piper-hi_IN-priyamvada-medium"
                val config = OfflineTtsConfig(
                    model = OfflineTtsModelConfig(
                        vits = OfflineTtsVitsModelConfig(
                            model = "$modelDir/hi_IN-priyamvada-medium.onnx",
                            tokens = "$modelDir/tokens.txt",
                            dataDir = "$modelDir/espeak-ng-data"
                        ),
                        numThreads = 2,
                        debug = false
                    ),
                    maxNumSentences = 1,
                    silenceScale = 0.2f
                )
                OfflineTts(assetManager = assets, config = config)
            }.onSuccess { engine ->
                tts = engine
                runOnUiThread {
                    ready = true
                    status = "Ready ✓ Hindi neural model loaded locally"
                    lastMetrics = "Sample rate ${engine.sampleRate()} Hz • speakers ${engine.numSpeakers()}"
                }
            }.onFailure { error ->
                runOnUiThread {
                    ready = false
                    status = "Model load failed: ${error.message ?: error.javaClass.simpleName}"
                }
            }
        }
    }

    private fun synthesize(text: String) {
        val engine = tts ?: return
        if (busy) return
        stopPlayback()
        busy = true
        status = "Generating locally…"

        worker.execute {
            val started = SystemClock.elapsedRealtimeNanos()
            runCatching {
                engine.generateWithConfig(
                    text = text,
                    config = GenerationConfig(
                        sid = 0,
                        speed = speed,
                        silenceScale = 0.2f
                    )
                )
            }.onSuccess { audio ->
                val generationMs = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000.0
                val audioMs = if (audio.sampleRate > 0) {
                    audio.samples.size * 1000.0 / audio.sampleRate
                } else {
                    0.0
                }
                val rtf = if (audioMs > 0.0) generationMs / audioMs else Double.NaN
                runOnUiThread {
                    busy = false
                    status = "Playing neural Mayra voice…"
                    lastMetrics = "Generate ${generationMs.roundToInt()} ms • audio ${audioMs.roundToInt()} ms • RTF ${"%.2f".format(rtf)}"
                    play(audio.samples, audio.sampleRate)
                }
            }.onFailure { error ->
                runOnUiThread {
                    busy = false
                    status = "Synthesis failed: ${error.message ?: error.javaClass.simpleName}"
                }
            }
        }
    }

    private fun play(samples: FloatArray, sampleRate: Int) {
        if (samples.isEmpty() || sampleRate <= 0) return
        stopPlayback()

        val pcm = ShortArray(samples.size) { index ->
            (samples[index].coerceIn(-1f, 1f) * Short.MAX_VALUE).roundToInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
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
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack = track
        track.write(pcm, 0, pcm.size)
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
}
