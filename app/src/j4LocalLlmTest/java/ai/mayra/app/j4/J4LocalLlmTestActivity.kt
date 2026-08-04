package ai.mayra.app.j4

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import ai.mayra.app.ui.theme.MayraAITheme
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.Executors

/**
 * J4 phase L0: owner-managed local-model import/integrity harness.
 *
 * No LLM runtime is linked yet. This proves the storage/checksum boundary first so a future
 * LiteRT-LM engine can consume a verified app-private model path without adding broad storage
 * permissions or bundling a large model into Mayra's APK.
 */
class J4LocalLlmTestActivity : ComponentActivity() {
    private val worker = Executors.newSingleThreadExecutor()

    private var status by mutableStateOf("Ready. Select a .litertlm model file to verify and import locally.")
    private var details by mutableStateOf("No model imported")
    private var busy by mutableStateOf(false)

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importModel(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Mayra J4 Local Brain Test", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Offline model storage + integrity preflight • no AI runtime yet")
                        Spacer(Modifier.height(18.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(details, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(18.dp))
                        Button(
                            enabled = !busy,
                            onClick = { picker.launch(arrayOf("application/octet-stream", "*/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (busy) "Importing…" else "Select Local Model")
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            enabled = !busy && importedModelFile().exists(),
                            onClick = ::removeImportedModel,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Imported Model")
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(
                            "This J4 step deliberately does not execute model output. Next gate links LiteRT-LM only after local file import, SHA-256 and cleanup are proven.",
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
        busy = true
        status = "Copying selected model into Mayra private storage…"
        details = displayName(uri)

        worker.execute {
            val result = runCatching {
                val target = importedModelFile()
                target.parentFile?.mkdirs()
                val digest = MessageDigest.getInstance("SHA-256")
                var bytes = 0L
                contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input) { "Unable to open selected model" }
                    target.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            bytes += read
                        }
                    }
                }
                check(bytes > 0L) { "Selected file is empty" }
                val sha = digest.digest().joinToString("") { "%02x".format(it) }
                "Imported ${formatBytes(bytes)} • SHA-256 $sha"
            }

            runOnUiThread {
                busy = false
                result.onSuccess { message ->
                    status = "Model import verified ✓"
                    details = message
                }.onFailure { error ->
                    importedModelFile().delete()
                    status = "Model import failed"
                    details = "${error.javaClass.simpleName}: ${error.message.orEmpty().take(220)}"
                }
            }
        }
    }

    private fun removeImportedModel() {
        val removed = runCatching { importedModelFile().delete() }.getOrDefault(false)
        status = if (removed) "Imported model removed ✓" else "No imported model to remove"
        details = "Private model path is clear"
    }

    private fun refreshExistingState() {
        val file = importedModelFile()
        if (file.exists() && file.length() > 0L) {
            status = "Imported model present ✓"
            details = "${formatBytes(file.length())} • ready for future runtime benchmark"
        }
    }

    private fun importedModelFile(): File = File(filesDir, "models/j4-model.litertlm")

    private fun displayName(uri: Uri): String {
        val name = runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
        return name ?: uri.lastPathSegment.orEmpty().ifBlank { "Selected model" }
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
}
