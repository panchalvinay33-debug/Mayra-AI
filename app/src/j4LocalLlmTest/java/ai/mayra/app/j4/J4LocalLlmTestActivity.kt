package ai.mayra.app.j4

import android.app.ActivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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

    private var status by mutableStateOf("Ready. Select a .litertlm model file to verify and import locally.")
    private var details by mutableStateOf("No model imported")
    private var deviceDetails by mutableStateOf("")
    private var busy by mutableStateOf(false)

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importModel(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceDetails = deviceDiagnostics()
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Mayra J4 Local Brain Test", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Offline model storage + SHA-256 integrity • zero permissions")
                        Spacer(Modifier.height(18.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(details, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Text(deviceDetails, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(18.dp))
                        Button(
                            enabled = !busy,
                            onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (busy) "Working…" else "Select Local Model") }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            enabled = !busy && importedModelFile().exists(),
                            onClick = ::verifyImportedModel,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Verify Imported Model") }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            enabled = !busy && importedModelFile().exists(),
                            onClick = ::removeImportedModel,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Remove Imported Model") }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "Next gate: initialize LiteRT-LM from this verified private path in a crash-isolated runtime, then run fixed Hindi/Hinglish/English prompts and measure load time, RAM and tokens/sec.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        refreshExistingState()
    }

    private fun importModel(uri: Uri) {
        if (busy) return
        val selected = selectedMetadata(uri)
        val name = selected.name
        if (!MayraLocalModelIntegrity.isLiteRtLmName(name)) {
            status = "Model import rejected"
            details = "Expected a .litertlm file, selected: $name"
            return
        }
        if (selected.size != null && selected.size <= 0L) {
            status = "Model import rejected"
            details = "Selected model is empty"
            return
        }

        val available = availablePrivateBytes()
        if (selected.size != null && !MayraLocalModelIntegrity.hasEnoughStorage(available, selected.size)) {
            status = "Not enough private storage"
            details = "Need model size plus ${formatBytes(MayraLocalModelIntegrity.DEFAULT_STORAGE_HEADROOM_BYTES)} safety headroom • available ${formatBytes(available)}"
            return
        }

        busy = true
        status = "Copying selected model into Mayra private storage…"
        details = buildString {
            append(name)
            selected.size?.let { append(" • ${formatBytes(it)}") }
        }

        worker.execute {
            val result = runCatching {
                val target = importedModelFile()
                val temp = File(target.parentFile, "${target.name}.partial")
                target.parentFile?.mkdirs()
                temp.delete()

                val digest = MessageDigest.getInstance("SHA-256")
                var bytes = 0L
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Unable to open selected model" }
                    temp.outputStream().buffered().use { output ->
                        val buffer = ByteArray(MayraLocalModelIntegrity.COPY_BUFFER_BYTES)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            bytes += read
                        }
                        output.flush()
                    }
                }
                check(bytes > 0L) { "Selected file is empty" }
                selected.size?.let { expected -> check(bytes == expected) { "Copy size mismatch: expected $expected, copied $bytes" } }
                check(temp.length() == bytes) { "Private copy size mismatch" }
                if (target.exists()) check(target.delete()) { "Could not replace previous model" }
                check(temp.renameTo(target)) { "Could not finalize private model atomically" }

                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                saveMetadata(name, bytes, sha)
                ImportResult(name, bytes, sha)
            }

            runOnUiThread {
                busy = false
                deviceDetails = deviceDiagnostics()
                result.onSuccess { imported ->
                    status = "Model import verified ✓"
                    details = "${imported.name} • ${formatBytes(imported.bytes)} • SHA-256 ${imported.sha256}"
                }.onFailure { error ->
                    File(importedModelFile().parentFile, "${importedModelFile().name}.partial").delete()
                    status = "Model import failed"
                    details = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(220)}"
                }
            }
        }
    }

    private fun verifyImportedModel() {
        if (busy) return
        val file = importedModelFile()
        if (!file.exists() || file.length() <= 0L) {
            status = "No imported model to verify"
            details = "Private model path is empty"
            return
        }
        busy = true
        status = "Recomputing SHA-256 from private model…"
        details = "Reading ${formatBytes(file.length())}; this can take a little while for a large model"
        worker.execute {
            val result = runCatching {
                val digest = MayraLocalModelIntegrity.sha256(file)
                val expected = prefs.getString(KEY_SHA, null)
                check(expected == null || expected == digest) {
                    "Integrity mismatch • expected ${expected.orEmpty().take(20)}… • got ${digest.take(20)}…"
                }
                if (expected == null) saveMetadata(prefs.getString(KEY_NAME, file.name) ?: file.name, file.length(), digest)
                digest
            }
            runOnUiThread {
                busy = false
                result.onSuccess { digest ->
                    status = "Private model integrity PASS ✓"
                    details = "${formatBytes(file.length())} • SHA-256 $digest"
                }.onFailure { error ->
                    status = "Private model integrity FAIL"
                    details = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(220)}"
                }
            }
        }
    }

    private fun removeImportedModel() {
        if (busy) return
        val file = importedModelFile()
        val removed = !file.exists() || runCatching { file.delete() }.getOrDefault(false)
        File(file.parentFile, "${file.name}.partial").delete()
        if (removed) prefs.edit().clear().apply()
        status = if (removed) "Imported model removed ✓" else "Could not remove imported model"
        details = if (removed) "Private model path and saved checksum metadata are clear" else file.absolutePath
        deviceDetails = deviceDiagnostics()
    }

    private fun refreshExistingState() {
        val file = importedModelFile()
        if (file.exists() && file.length() > 0L) {
            val name = prefs.getString(KEY_NAME, file.name) ?: file.name
            val sha = prefs.getString(KEY_SHA, null)
            status = "Imported model present ✓"
            details = buildString {
                append("$name • ${formatBytes(file.length())}")
                if (!sha.isNullOrBlank()) append(" • saved SHA-256 $sha")
                append(" • tap Verify before runtime benchmark")
            }
        }
    }

    private fun selectedMetadata(uri: Uri): SelectedFile {
        var name: String? = null
        var size: Long? = null
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        }
        return SelectedFile(name ?: uri.lastPathSegment.orEmpty().ifBlank { "selected-model.litertlm" }, size)
    }

    private fun saveMetadata(name: String, bytes: Long, sha: String) {
        prefs.edit().putString(KEY_NAME, name).putLong(KEY_BYTES, bytes).putString(KEY_SHA, sha)
            .putLong(KEY_IMPORTED_AT, System.currentTimeMillis()).apply()
    }

    private fun importedModelFile(): File = File(filesDir, "models/j4-model.litertlm")
    private fun availablePrivateBytes(): Long = StatFs(filesDir.absolutePath).availableBytes

    private fun deviceDiagnostics(): String {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        return buildString {
            append("Device ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}")
            append(" • ABI ${Build.SUPPORTED_ABIS.firstOrNull().orEmpty()}")
            append(" • RAM ${formatBytes(memoryInfo.totalMem)}")
            append(" • app heap class ${activityManager.memoryClass} MB")
            append(" • private free ${formatBytes(availablePrivateBytes())}")
        }
    }

    private fun formatBytes(bytes: Long): String = when {
        bytes >= 1024L * 1024L * 1024L -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }

    override fun onDestroy() {
        worker.shutdownNow()
        super.onDestroy()
    }

    private data class SelectedFile(val name: String, val size: Long?)
    private data class ImportResult(val name: String, val bytes: Long, val sha256: String)

    companion object {
        private const val PREFS = "j4_model_metadata"
        private const val KEY_NAME = "name"
        private const val KEY_BYTES = "bytes"
        private const val KEY_SHA = "sha256"
        private const val KEY_IMPORTED_AT = "imported_at"
    }
}
