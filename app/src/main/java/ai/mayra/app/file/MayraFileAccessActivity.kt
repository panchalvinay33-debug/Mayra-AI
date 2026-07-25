package ai.mayra.app.file

import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MayraFileAccessActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraFileAccessScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MayraFileAccessScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { MayraEncryptedFileIndexStore(context) }
    val registry = remember { MayraFileGrantRegistry(context) }
    var snapshot by remember { mutableStateOf(store.read()) }
    var message by remember { mutableStateOf<String?>(null) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                registry.register(uri, uri.lastPathSegment.orEmpty())
                MayraFileInventoryWorker.enqueue(context)
                snapshot = store.read()
                message = "Folder access saved. Background inventory queued."
            }.onFailure { message = it.message ?: "Could not save folder access." }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Mayra File Intelligence", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Authorized folders only · encrypted metadata index", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onClose) { Text("Close") }
            }

            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Index status", fontWeight = FontWeight.SemiBold)
                    Text("${snapshot.files.size} files · ${snapshot.grants.count { it.enabled }} folders · generation ${snapshot.generation}")
                    Text("PDF text and OCR are not active yet; this stage indexes permitted metadata honestly.", style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { treeLauncher.launch(null) }, modifier = Modifier.weight(1f)) { Text("Add folder") }
                OutlinedButton(onClick = {
                    MayraFileInventoryWorker.enqueue(context)
                    message = "Inventory queued."
                }, modifier = Modifier.weight(1f)) { Text("Scan now") }
                OutlinedButton(onClick = { snapshot = store.read() }, modifier = Modifier.weight(1f)) { Text("Refresh") }
            }

            message?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (snapshot.grants.isNotEmpty()) {
                    item { Text("Authorized folders", fontWeight = FontWeight.Bold) }
                    items(snapshot.grants, key = { it.treeUri }) { grant ->
                        Card(shape = RoundedCornerShape(14.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(grant.label, fontWeight = FontWeight.SemiBold)
                                Text(if (registry.hasPersistedReadAccess(grant.treeUri)) "Access active" else "Access lost", style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = {
                                    registry.remove(grant.treeUri)
                                    snapshot = store.read()
                                }) { Text("Remove") }
                            }
                        }
                    }
                }
                item { Text("Recent indexed files", fontWeight = FontWeight.Bold) }
                items(snapshot.files.sortedByDescending { it.modifiedAt }.take(50), key = { it.uri }) { file ->
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(file.displayName, fontWeight = FontWeight.SemiBold)
                            Text(file.mimeType ?: "Unknown type", style = MaterialTheme.typography.bodySmall)
                            Text(file.relativeLocation ?: file.sourceKind.name, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
