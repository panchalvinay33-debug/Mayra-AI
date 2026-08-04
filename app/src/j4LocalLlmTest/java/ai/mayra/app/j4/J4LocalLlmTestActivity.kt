package ai.mayra.app.j4

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.StatFs
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import ai.mayra.app.localmodel.MayraLocalModelIntegrity
import ai.mayra.app.ui.theme.MayraAITheme
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors

class J4LocalLlmTestActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val mainHandler = Handler(Looper.getMainLooper())

    private var status by mutableStateOf("Ready. Select a .litertlm model file to verify and import locally.")
    private var details by mutableStateOf("No model imported")
    private var deviceDetails by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var runtimeBound by mutableStateOf(false)
    private var runtimeLoaded by mutableStateOf(false)
    private var runtimeClosing by mutableStateOf(false)
    private var runtimeMessenger: Messenger? = null

    private val replyMessenger = Messenger(Handler(Looper.getMainLooper()) { msg ->
        if (msg.what == J4LocalBrainRuntimeService.MSG_STATUS) {
            status = msg.data.getString(J4LocalBrainRuntimeService.KEY_STAGE).orEmpty()
            details = msg.data.getString(J4LocalBrainRuntimeService.KEY_DETAIL).orEmpty()
            busy = status.startsWith("Stage 4/5") || status.startsWith("Generating") || status.startsWith("Closing runtime")
            if (status.startsWith("Stage 5/5")) {
                runtimeLoaded = true
                busy = false
            }
            if (status.startsWith("Runtime load failed") || status.startsWith("Generation failed") || status.startsWith("Generation PASS") || status.startsWith("Generation blocked")) busy = false
            if (status.startsWith("Runtime closed")) {
                runtimeLoaded = false
                runtimeClosing = true
                busy = false
            }
            true
        } else false
    })

    private val runtimeConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            runtimeMessenger = service?.let(::Messenger)
            runtimeBound = runtimeMessenger != null
            if (runtimeClosing) {
                runtimeClosing = false
                status = "Runtime ready after close ✓"
                details = "Fresh isolated :localbrain process rebound; model remains imported"
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            runtimeMessenger = null
            runtimeBound = false
            runtimeLoaded = false
            busy = false
            if (runtimeClosing) {
                status = "Runtime closed ✓"
                details = "Isolated :localbrain process exited; launcher stayed alive"
                mainHandler.postDelayed({ bindRuntimeIfNeeded() }, 500L)
            } else {
                status = "Runtime process disconnected"
                details = "The isolated :localbrain process exited. Launcher stayed alive."
                mainHandler.postDelayed({ bindRuntimeIfNeeded() }, 500L)
            }
        }

        override fun onBindingDied(name: ComponentName?) = onServiceDisconnected(name)
    }

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importModel(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceDetails = deviceDiagnostics()
        bindRuntimeIfNeeded()
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Text("Mayra J4 Local Brain Test", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Model integrity + crash-isolated LiteRT-LM 0.15.0 CPU init + fixed generation • zero permissions")
                        Spacer(Modifier.height(18.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(details, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Text(deviceDetails, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(18.dp))
                        Button(enabled = !busy, onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Working…" else "Select Local Model") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && importedModelFile().exists(), onClick = ::verifyImportedModel, modifier = Modifier.fillMaxWidth()) { Text("Verify Imported Model") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && importedModelFile().exists() && runtimeBound, onClick = ::initializeRuntime, modifier = Modifier.fillMaxWidth()) { Text("Initialize LiteRT-LM CPU") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && runtimeLoaded && runtimeBound, onClick = { generateFixed(HINDI_PROMPT) }, modifier = Modifier.fillMaxWidth()) { Text("Run Hindi Prompt") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && runtimeLoaded && runtimeBound, onClick = { generateFixed(HINGLISH_PROMPT) }, modifier = Modifier.fillMaxWidth()) { Text("Run Hinglish Prompt") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && runtimeLoaded && runtimeBound, onClick = { generateFixed(ENGLISH_PROMPT) }, modifier = Modifier.fillMaxWidth()) { Text("Run English Prompt") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && runtimeBound, onClick = ::closeRuntime, modifier = Modifier.fillMaxWidth()) { Text("Close Runtime") }
                        Spacer(Modifier.height(8.dp))
                        Button(enabled = !busy && importedModelFile().exists(), onClick = ::removeImportedModel, modifier = Modifier.fillMaxWidth()) { Text("Remove Imported Model") }
                        Spacer(Modifier.height(16.dp))
                        Text("Engineering gate: local CPU generation only. No tool calls, memory writes, messages or device actions.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        refreshExistingState()
    }

    private fun bindRuntimeIfNeeded() {
        if (isFinishing || isDestroyed || runtimeBound) return
        runCatching {
            bindService(Intent(this, J4LocalBrainRuntimeService::class.java), runtimeConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun initializeRuntime() {
        val model = importedModelFile()
        if (!model.isFile || model.length() <= 0L) return
        val messenger = runtimeMessenger ?: return
        busy = true
        runtimeLoaded = false
        status = "Starting isolated runtime…"
        details = "LiteRT-LM 0.15.0 • CPU • fixed prompts only"
        val msg = Message.obtain(null, J4LocalBrainRuntimeService.MSG_LOAD).apply {
            data = Bundle().apply { putString(J4LocalBrainRuntimeService.KEY_MODEL_PATH, model.absolutePath) }
            replyTo = replyMessenger
        }
        runCatching { messenger.send(msg) }.onFailure {
            busy = false
            status = "Runtime start failed"
            details = "${it.javaClass.simpleName}: ${it.message.orEmpty()}"
        }
    }

    private fun generateFixed(prompt: String) {
        val messenger = runtimeMessenger ?: return
        busy = true
        val msg = Message.obtain(null, J4LocalBrainRuntimeService.MSG_GENERATE).apply {
            data = Bundle().apply { putString(J4LocalBrainRuntimeService.KEY_PROMPT, prompt) }
            replyTo = replyMessenger
        }
        runCatching { messenger.send(msg) }.onFailure {
            busy = false
            status = "Generation send failed"
            details = "${it.javaClass.simpleName}: ${it.message.orEmpty()}"
        }
    }

    private fun closeRuntime() {
        val messenger = runtimeMessenger ?: return
        runtimeClosing = true
        busy = true
        status = "Closing runtime…"
        details = "Native close gets a 2 s grace period, then :localbrain is reclaimed"
        val msg = Message.obtain(null, J4LocalBrainRuntimeService.MSG_CLOSE).apply { replyTo = replyMessenger }
        runCatching { messenger.send(msg) }.onFailure {
            runtimeClosing = false
            busy = false
            status = "Runtime close request failed"
            details = "${it.javaClass.simpleName}: ${it.message.orEmpty()}"
        }
    }

    private fun importModel(uri: Uri) {
        if (busy) return
        val selected = selectedMetadata(uri)
        val name = selected.name
        if (!MayraLocalModelIntegrity.isLiteRtLmName(name)) { status = "Model import rejected"; details = "Expected a .litertlm file, selected: $name"; return }
        if (selected.size != null && selected.size <= 0L) { status = "Model import rejected"; details = "Selected model is empty"; return }
        val available = availablePrivateBytes()
        if (selected.size != null && !MayraLocalModelIntegrity.hasEnoughStorage(available, selected.size)) { status = "Not enough private storage"; details = "Need model size plus ${formatBytes(MayraLocalModelIntegrity.DEFAULT_STORAGE_HEADROOM_BYTES)} safety headroom • available ${formatBytes(available)}"; return }
        busy = true
        status = "Copying selected model into Mayra private storage…"
        details = buildString { append(name); selected.size?.let { append(" • ${formatBytes(it)}") } }
        worker.execute {
            val result = runCatching {
                val target = importedModelFile(); val temp = File(target.parentFile, "${target.name}.partial")
                target.parentFile?.mkdirs(); temp.delete()
                val digest = MessageDigest.getInstance("SHA-256"); var bytes = 0L
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Unable to open selected model" }
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(MayraLocalModelIntegrity.COPY_BUFFER_BYTES)
                        while (true) { val read = input.read(buffer); if (read < 0) break; if (read == 0) continue; output.write(buffer, 0, read); digest.update(buffer, 0, read); bytes += read }
                        output.flush()
                    }
                }
                check(bytes > 0L); selected.size?.let { expected -> check(bytes == expected) }; check(temp.length() == bytes)
                if (target.exists()) check(target.delete()); check(temp.renameTo(target))
                val sha = digest.digest().joinToString("") { "%02x".format(it) }; saveMetadata(name, bytes, sha); ImportResult(name, bytes, sha)
            }
            runOnUiThread {
                busy = false; deviceDetails = deviceDiagnostics()
                result.onSuccess { imported -> status = "Model import verified ✓"; details = "${imported.name} • ${formatBytes(imported.bytes)} • SHA-256 ${imported.sha256}" }
                    .onFailure { error -> File(importedModelFile().parentFile, "${importedModelFile().name}.partial").delete(); status = "Model import failed"; details = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(220)}" }
            }
        }
    }

    private fun verifyImportedModel() {
        if (busy) return
        val file = importedModelFile(); if (!file.exists() || file.length() <= 0L) { status = "No imported model to verify"; return }
        busy = true; status = "Recomputing SHA-256 from private model…"; details = "Reading ${formatBytes(file.length())}"
        worker.execute {
            val result = runCatching { val digest = MayraLocalModelIntegrity.sha256(file); val expected = prefs.getString(KEY_SHA, null); check(expected == null || expected == digest); if (expected == null) saveMetadata(prefs.getString(KEY_NAME, file.name) ?: file.name, file.length(), digest); digest }
            runOnUiThread { busy = false; result.onSuccess { status = "Private model integrity PASS ✓"; details = "${formatBytes(file.length())} • SHA-256 $it" }.onFailure { status = "Private model integrity FAIL"; details = "${it.javaClass.simpleName}: ${it.message.orEmpty()}" } }
        }
    }

    private fun removeImportedModel() {
        if (runtimeLoaded || runtimeBound) closeRuntime()
        val file = importedModelFile(); val removed = !file.exists() || runCatching { file.delete() }.getOrDefault(false); File(file.parentFile, "${file.name}.partial").delete(); if (removed) prefs.edit().clear().apply(); status = if (removed) "Imported model removed ✓" else "Could not remove imported model"; details = if (removed) "Private model path and saved checksum metadata are clear" else file.absolutePath; deviceDetails = deviceDiagnostics()
    }

    private fun refreshExistingState() { val file = importedModelFile(); if (file.exists() && file.length() > 0L) { val name = prefs.getString(KEY_NAME, file.name) ?: file.name; val sha = prefs.getString(KEY_SHA, null); status = "Imported model present ✓"; details = "$name • ${formatBytes(file.length())}${if (!sha.isNullOrBlank()) " • saved SHA-256 $sha" else ""}" } }

    private fun selectedMetadata(uri: Uri): SelectedFile { var name: String? = null; var size: Long? = null; runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor -> if (cursor.moveToFirst()) { val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); val si = cursor.getColumnIndex(OpenableColumns.SIZE); if (ni >= 0 && !cursor.isNull(ni)) name = cursor.getString(ni); if (si >= 0 && !cursor.isNull(si)) size = cursor.getLong(si) } } }; return SelectedFile(name ?: uri.lastPathSegment.orEmpty().ifBlank { "selected-model.litertlm" }, size) }
    private fun saveMetadata(name: String, bytes: Long, sha: String) { prefs.edit().putString(KEY_NAME, name).putLong(KEY_BYTES, bytes).putString(KEY_SHA, sha).putLong(KEY_IMPORTED_AT, System.currentTimeMillis()).apply() }
    private fun importedModelFile(): File = File(filesDir, "models/j4-model.litertlm")
    private fun availablePrivateBytes(): Long = StatFs(filesDir.absolutePath).availableBytes
    private fun deviceDiagnostics(): String { val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager; val mi = ActivityManager.MemoryInfo().also(am::getMemoryInfo); return "Device ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE} • ABI ${Build.SUPPORTED_ABIS.firstOrNull().orEmpty()} • RAM ${formatBytes(mi.totalMem)} • app heap ${am.memoryClass} MB • private free ${formatBytes(availablePrivateBytes())}" }
    private fun formatBytes(bytes: Long): String = when { bytes >= 1024L*1024L*1024L -> "%.2f GB".format(bytes/(1024.0*1024.0*1024.0)); bytes >= 1024L*1024L -> "%.1f MB".format(bytes/(1024.0*1024.0)); bytes >= 1024L -> "%.1f KB".format(bytes/1024.0); else -> "$bytes B" }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (runtimeBound) runCatching { unbindService(runtimeConnection) }
        runtimeBound = false; runtimeMessenger = null; worker.shutdownNow(); super.onDestroy()
    }

    private data class SelectedFile(val name: String, val size: Long?)
    private data class ImportResult(val name: String, val bytes: Long, val sha256: String)

    companion object {
        private const val PREFS = "j4_model_metadata"
        private const val KEY_NAME = "name"
        private const val KEY_BYTES = "bytes"
        private const val KEY_SHA = "sha256"
        private const val KEY_IMPORTED_AT = "imported_at"
        private const val HINDI_PROMPT = "केवल एक छोटे वाक्य में जवाब दो: भारत की राजधानी क्या है?"
        private const val HINGLISH_PROMPT = "Sirf ek short line me batao: kal subah 7 baje uthne ke liye ek simple reminder sentence kya hoga?"
        private const val ENGLISH_PROMPT = "Answer in one short sentence: What is two plus two?"
    }
}
