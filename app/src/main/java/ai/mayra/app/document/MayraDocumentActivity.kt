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
    val reader = remember(context) { MayraDocumentTextReader(context) }
    var refresh by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var previewTitle by remember { mutableStateOf<String?>(null) }
    var previewText by remember { mutableStateOf<String?>(null) }
    val documents = remember(refresh, search) {
        store.list().filter { search.isBlank() || it.name.contains(search, ignoreCase = true) || it.mimeType.contains(search, ignoreCase = true) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Document Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Choose documents for a local metadata library. Supported text-like files can be previewed with strict size limits.")

            Button(
                onClick = { picker.launch(arrayOf("application/pdf", "text/*", "application/json", "application/xml", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add PDF or document") }

            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search documents") }, singleLine = true)
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            previewTitle?.let { title ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("Preview · $title", fontWeight = FontWeight.SemiBold)
                        Text(previewText.orEmpty(), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { previewTitle = null; previewText = null }) { Text("Close preview") }
                    }
                }
            }

            if (documents.isEmpty()) {
                Text("No matching documents yet.")
            } else {
                documents.forEach { document ->
                    DocumentCard(
                        document = document,
                        previewSupported = MayraDocumentTextReader.isSupportedTextType(document.mimeType, document.name),
                        onPreview = {
                            when (val result = reader.preview(Uri.parse(document.uri), document.mimeType)) {
                                is DocumentTextPreview.Ready -> {
                                    previewTitle = document.name
                                    previewText = result.text + if (result.truncated) "\n\n[Preview truncated for safety]" else ""
                                    store.markOpened(document.uri)
                                    refresh++
                                }
                                is DocumentTextPreview.Unsupported -> notice = result.reason
                                is DocumentTextPreview.Error -> notice = result.reason
                                DocumentTextPreview.Empty -> notice = "This document has no readable text."
                            }
                        },
                        onOpen = {
                            runCatching {
                                store.markOpened(document.uri)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(document.uri)).apply {
                                        setDataAndType(Uri.parse(document.uri), document.mimeType.ifBlank { "*/*" })
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                            }.onFailure { notice = "No compatible app could open this document." }
                        },
                        onRemove = {
                            store.remove(document.uri)
                            if (previewTitle == document.name) { previewTitle = null; previewText = null }
                            refresh++
                        }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Document intelligence status", fontWeight = FontWeight.SemiBold)
                    Text("Metadata library, persistent access, search, system open and bounded text preview are implemented. PDF page extraction, DOC parsing and AI summaries are not claimed yet.", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

@Composable
private fun DocumentCard(
    document: MayraDocument,
    previewSupported: Boolean,
    onPreview: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(document.name, fontWeight = FontWeight.SemiBold)
            Text(document.mimeType.ifBlank { "Unknown type" }, style = MaterialTheme.typography.bodySmall)
            if (document.sizeBytes >= 0) Text("${document.sizeBytes} bytes", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRemove) { Text("Remove") }
                if (previewSupported) TextButton(onClick = onPreview) { Text("Preview") }
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
        val document = MayraDocument(
            uri = uri.toString(),
            name = metadata.first,
            mimeType = appContext.contentResolver.getType(uri).orEmpty(),
            sizeBytes = metadata.second,
            addedAt = System.currentTimeMillis()
        )
        write(current + document)
        return document
    }

    fun list(): List<MayraDocument> = preferences.getStringSet(KEY_DOCUMENTS, emptySet()).orEmpty()
        .mapNotNull(::decode)
        .sortedWith(compareByDescending<MayraDocument> { it.lastOpenedAt }.thenByDescending { it.addedAt })

    @Synchronized
    fun markOpened(uri: String) {
        write(list().map { if (it.uri == uri) it.copy(lastOpenedAt = System.currentTimeMillis()) else it })
    }

    @Synchronized
    fun remove(uri: String) {
        write(list().filterNot { it.uri == uri })
        runCatching { appContext.contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION) }
    }

    private fun readMetadata(uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment ?: "Document"
        var size = -1L
        val cursor: Cursor? = appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
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
        value.uri, clean(value.name), clean(value.mimeType), value.sizeBytes, value.addedAt, value.lastOpenedAt
    ).joinToString(SEPARATOR)

    private fun decode(raw: String): MayraDocument? {
        val parts = raw.split(SEPARATOR)
        if (parts.size != 6) return null
        return runCatching {
            MayraDocument(parts[0], parts[1], parts[2], parts[3].toLong(), parts[4].toLong(), parts[5].toLong())
        }.getOrNull()
    }

    private fun clean(value: String): String = value.replace(SEPARATOR, " ").take(300)

    private companion object {
        const val FILE_NAME = "mayra_document_library"
        const val KEY_DOCUMENTS = "documents"
        const val SEPARATOR = "\u001E"
    }
}
