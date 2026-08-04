package ai.mayra.app.j3

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
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

/**
 * Zero-permission, zero-network neural Hindi TTS benchmark.
 *
 * sherpa-onnx runs in J3NeuralTtsService's secondary process. A native abort cannot kill this
 * launcher process. Android's own ApplicationExitInfo is then used to classify the secondary
 * process death so the next device screenshot can distinguish native crash, ANR, LMK and timeout.
 */
class J3NeuralTtsTestActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var loadGeneration = 0
    private var speakGeneration = 0
    private var loadAttemptWallMs = 0L

    private var ready by mutableStateOf(false)
    private var loading by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var status by mutableStateOf("App ready. Tap Load Neural Voice to start the offline model.")
    private var lastMetrics by mutableStateOf("Neural engine not loaded yet")
    private var speed by mutableStateOf(1.0f)

    private val phrases = listOf(
        "नमस्ते, मैं मायरा हूँ। मैं आपकी बात सुन रही हूँ।",
        "कल सुबह सात बजे मुझे दवा लेने की याद दिलाना।",
        "आज मौसम कैसा है और बारिश होने की संभावना कितनी है?",
        "WhatsApp खोलने से पहले मैं आपसे पुष्टि करूँगी।",
        "Hello, main Mayra hoon. Aap bataiye main kya help kar sakti hoon?",
        "आज चार अगस्त है और समय नौ बजकर बत्तीस मिनट है।"
    )

    private val resultReceiver = object : ResultReceiver(handler) {
        override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
            val message = resultData?.getString(J3NeuralTtsService.EXTRA_MESSAGE).orEmpty()
            when (resultCode) {
                J3NeuralTtsService.RESULT_READY -> {
                    loadGeneration++
                    loading = false
                    ready = true
                    status = "Ready ✓ Hindi neural model loaded locally"
                    lastMetrics = message.ifBlank { "Native model ready" }
                }
                J3NeuralTtsService.RESULT_PLAYING -> {
                    speakGeneration++
                    busy = false
                    status = "Playing neural Mayra voice…"
                    lastMetrics = message.ifBlank { "Playback started" }
                }
                J3NeuralTtsService.RESULT_STOPPED -> {
                    busy = false
                    status = "Playback stopped"
                    if (message.isNotBlank()) lastMetrics = message
                }
                J3NeuralTtsService.RESULT_ERROR -> {
                    loadGeneration++
                    speakGeneration++
                    loading = false
                    busy = false
                    status = "Neural runtime error"
                    lastMetrics = message.ifBlank { "Unknown neural runtime error" }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Mayra J3 Neural Voice Test", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Free • offline • zero permissions • benchmark only")
                        Spacer(Modifier.height(12.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Text(lastMetrics, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(14.dp))

                        Button(onClick = ::loadModel, enabled = !ready && !loading && !busy, modifier = Modifier.fillMaxWidth()) {
                            Text(if (loading) "Loading…" else if (ready) "Neural Voice Loaded ✓" else "Load Neural Voice")
                        }
                        Spacer(Modifier.height(12.dp))

                        phrases.forEachIndexed { index, phrase ->
                            Button(onClick = { synthesize(phrase) }, enabled = ready && !busy, modifier = Modifier.fillMaxWidth()) {
                                Text("${index + 1}. ${phrase.take(34)}${if (phrase.length > 34) "…" else ""}")
                            }
                            Spacer(Modifier.height(6.dp))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { speed = 0.92f; status = "Speed 0.92× — softer/slower comparison" }, enabled = ready && !busy) { Text("0.92×") }
                            Button(onClick = { speed = 1.0f; status = "Speed 1.00× — model default" }, enabled = ready && !busy) { Text("1.00×") }
                            Button(onClick = ::stopPlayback, enabled = ready || busy) { Text("Stop") }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Native neural runtime is crash-isolated. On failure Android process-exit diagnostics are shown here instead of closing the app.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    private fun loadModel() {
        if (loading || ready || busy) return
        loading = true
        status = "Loading free offline Hindi neural voice…"
        lastMetrics = "Starting crash-isolated sherpa-onnx process"
        loadAttemptWallMs = System.currentTimeMillis()
        val generation = ++loadGeneration

        runCatching {
            startService(Intent(this, J3NeuralTtsService::class.java)
                .setAction(J3NeuralTtsService.ACTION_LOAD)
                .putExtra(J3NeuralTtsService.EXTRA_RECEIVER, resultReceiver))
        }.onFailure { error ->
            loading = false
            ready = false
            status = "Could not start neural runtime"
            lastMetrics = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}"
            return
        }

        handler.postDelayed({
            if (loading && generation == loadGeneration) {
                loading = false
                ready = false
                val exit = latestNeuralExit(loadAttemptWallMs)
                if (exit != null) {
                    val description = exit.description.orEmpty()
                    status = "Neural process exited: ${reasonName(exit.reason)}"
                    lastMetrics = buildString {
                        append("Launcher stayed alive ✓")
                        append(" • status ${exit.status}")
                        if (description.isNotBlank()) append(" • ${description.take(120)}")
                    }
                } else {
                    status = "Neural process timed out"
                    lastMetrics = "Launcher stayed alive ✓ No recorded process exit; model did not return within 45 s"
                }
            }
        }, LOAD_TIMEOUT_MS)
    }

    private fun latestNeuralExit(sinceMs: Long): ApplicationExitInfo? {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return runCatching {
            manager.getHistoricalProcessExitReasons(packageName, 0, 12)
                .filter { it.processName.endsWith(":neuraltts") }
                .filter { it.timestamp >= sinceMs - 2_000L }
                .maxByOrNull { it.timestamp }
        }.getOrNull()
    }

    private fun reasonName(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
        ApplicationExitInfo.REASON_CRASH -> "JAVA_CRASH"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "RESOURCE_LIMIT"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        ApplicationExitInfo.REASON_OTHER -> "OTHER"
        else -> "REASON_$reason"
    }

    private fun synthesize(text: String) {
        if (!ready || busy) return
        busy = true
        status = "Generating locally in isolated neural process…"
        val generation = ++speakGeneration

        runCatching {
            startService(Intent(this, J3NeuralTtsService::class.java)
                .setAction(J3NeuralTtsService.ACTION_SPEAK)
                .putExtra(J3NeuralTtsService.EXTRA_RECEIVER, resultReceiver)
                .putExtra(J3NeuralTtsService.EXTRA_TEXT, text)
                .putExtra(J3NeuralTtsService.EXTRA_SPEED, speed))
        }.onFailure { error ->
            busy = false
            status = "Could not reach neural runtime"
            lastMetrics = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(180)}"
            return
        }

        handler.postDelayed({
            if (busy && generation == speakGeneration) {
                busy = false
                ready = false
                status = "Neural process crashed or synthesis timed out"
                lastMetrics = "Launcher stayed alive ✓ Reload Neural Voice before retrying"
            }
        }, SPEAK_TIMEOUT_MS)
    }

    private fun stopPlayback() {
        speakGeneration++
        busy = false
        runCatching {
            startService(Intent(this, J3NeuralTtsService::class.java)
                .setAction(J3NeuralTtsService.ACTION_STOP)
                .putExtra(J3NeuralTtsService.EXTRA_RECEIVER, resultReceiver))
        }
    }

    override fun onStop() {
        stopPlayback()
        super.onStop()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        runCatching { stopService(Intent(this, J3NeuralTtsService::class.java)) }
        super.onDestroy()
    }

    companion object {
        private const val LOAD_TIMEOUT_MS = 45_000L
        private const val SPEAK_TIMEOUT_MS = 60_000L
    }
}
