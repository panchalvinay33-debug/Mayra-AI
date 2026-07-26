package ai.mayra.app.document

import ai.mayra.app.ui.theme.MayraAITheme
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MayraDocumentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { DocumentLibraryScreen(onClose = ::finish) } }
    }
}

@Composable
private fun DocumentLibraryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MayraDocumentStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    val documents = remember(refresh, search) {
        store.list().filter {
            search.isBlank() ||
                it.name.contains(search, ignoreCase = true) ||
                it.mimeType.contains(search, ignoreCase = true)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                store.add(uri)
            }.onSuccess {
                refresh++
                notice = "Document added to Mayra Library."
            }.onFailure {
                notice = "Mayra could not keep access to this document."
            }
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Document Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Choose documents you want Mayra to remember as local library entries. " +
                    "This foundation stores persistent access and file metadata locally on your device."
            )

            Button(
                onClick = {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "text/*",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add PDF or document")
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search documents") },
                singleLine = true
            )
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            if (documents.isEmpty()) {
                Text("No matching documents yet.")
            } else {
                documents.forEach { document ->
                    DocumentCard(
                        document = document,
                        onOpen = {
                            runCatching {
                                val documentUri = Uri.parse(document.uri)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            documentUri,
                                            document.mimeType.ifBlank { "*/*" }
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                                store.markOpened(document.uri)
                                refresh++
                            }.onFailure {
                                notice = "No compatible app could open this document."
                            }
                        },
                        onRemove = {
                            store.remove(document.uri)
                            refresh++
                            notice = "Document removed from Mayra Library."
                        }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Document intelligence status", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Local library, persistent access, metadata search and document opening are ready. " +
                            "Full-text extraction, page search and AI summaries remain a later milestone.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: MayraDocument,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(document.name, fontWeight = FontWeight.SemiBold)
            Text(
                document.mimeType.ifBlank { "Unknown type" },
                style = MaterialTheme.typography.bodySmall
            )
            if (document.sizeBytes >= 0) {
                Text(formatFileSize(document.sizeBytes), style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRemove) { Text("Remove") }
                TextButton(onClick = onOpen) { Text("Open") }
            }
        }
    }
}

data class MayraDocument(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val addedAt: Long,
    val lastOpenedAt: Long = 0L
)

class MayraDocumentStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun add(uri: Uri): MayraDocument {
        val metadata = readMetadata(uri)
        val current = list().filterNot { it.uri == uri.toString() }
        val existing = list().firstOrNull { it.uri == uri.toString() }
        val document = MayraDocument(
            uri = uri.toString(),
            name = metadata.first,
            mimeType = appContext.contentResolver.getType(uri).orEmpty(),
            sizeBytes = metadata.second,
            addedAt = existing?.addedAt ?: System.currentTimeMillis(),
            lastOpenedAt = existing?.lastOpenedAt ?: 0L
        )
        write(current + document)
        return document
    }

    fun list(): List<MayraDocument> = preferences
        .getStringSet(KEY_DOCUMENTS, emptySet())
        .orEmpty()
        .mapNotNull(::decode)
        .sortedWith(
            compareByDescending<MayraDocument> { it.lastOpenedAt }
                .thenByDescending { it.addedAt }
        )

    @Synchronized
    fun markOpened(uri: String) {
        write(
            list().map {
                if (it.uri == uri) it.copy(lastOpenedAt = System.currentTimeMillis()) else it
            }
        )
    }

    @Synchronized
    fun remove(uri: String) {
        write(list().filterNot { it.uri == uri })
        runCatching {
            appContext.contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun readMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "Document"
        var size = -1L
        val cursor: Cursor? = appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = it.getString(nameIndex) ?: name
                if (sizeIndex >= 0 && !it.isNull(sizeIndex)) size = it.getLong(sizeIndex)
            }
        }
        return name.take(220) to size
    }

    private fun write(items: List<MayraDocument>) {
        preferences.edit().putStringSet(KEY_DOCUMENTS, items.map(::encode).toSet()).apply()
    }

    private fun encode(value: MayraDocument): String = listOf(
        value.uri,
        clean(value.name),
        clean(value.mimeType),
        value.sizeBytes,
        value.addedAt,
        value.lastOpenedAt
    ).joinToString(SEPARATOR)

    private fun decode(raw: String): MayraDocument? {
        val parts = raw.split(SEPARATOR)
        if (parts.size != 6) return null
        return runCatching {
            MayraDocument(
                uri = parts[0],
                name = parts[1],
                mimeType = parts[2],
                sizeBytes = parts[3].toLong(),
                addedAt = parts[4].toLong(),
                lastOpenedAt = parts[5].toLong()
            )
        }.getOrNull()
    }

    private fun clean(value: String): String = value.replace(SEPARATOR, " ").take(300)

    private companion object {
        const val FILE_NAME = "mayra_document_library"
        const val KEY_DOCUMENTS = "documents"
        const val SEPARATOR = "\u001E"
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "${bytes / 1_024} KB"
    else -> "${bytes / 1_048_576} MB"
}
