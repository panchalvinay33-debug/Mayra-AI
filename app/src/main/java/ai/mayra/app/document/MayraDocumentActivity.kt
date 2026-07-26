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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MayraDocumentActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { DocumentLibraryScreen(onClose = ::finish) } }
    }
}

@Composable
private fun DocumentLibraryScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { MayraDocumentStore(context) }
    val contentStore = remember(context) { MayraDocumentContentStore(context) }
    val extractor = remember(context) { MayraDocumentTextExtractor(context) }
    val searchEngine = remember(context) { MayraDocumentSearch(store, contentStore) }
    var refresh by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var addingDocument by remember { mutableStateOf(false) }
    var indexingUris by remember { mutableStateOf(emptySet<String>()) }

    val documents = remember(refresh, search) {
        if (search.isBlank()) {
            store.list().map { document ->
                DocumentSearchHit(
                    document = document,
                    score = 0,
                    snippet = contentStore.get(document.uri)?.text.orEmpty().take(220),
                    matchedContent = contentStore.get(document.uri) != null
                )
            }
        } else {
            searchEngine.search(search, limit = 20)
        }
    }

    suspend fun index(document: MayraDocument): String = withContext(Dispatchers.IO) {
        when (val result = extractor.extract(document)) {
            is DocumentExtractionResult.Success -> {
                if (result.text.isBlank()) {
                    contentStore.remove(document.uri)
                    "No readable text was found. This may be a scanned PDF that needs OCR."
                } else {
                    contentStore.put(document.uri, result.text, result.truncated)
                    val suffix = if (result.truncated) {
                        " The index was safely limited to 500,000 characters or 100 PDF pages."
                    } else {
                        ""
                    }
                    "Indexed ${result.text.length} characters locally.$suffix"
                }
            }
            is DocumentExtractionResult.Unsupported -> result.reason
            is DocumentExtractionResult.Failure -> result.reason
        }
    }

    fun startIndex(document: MayraDocument, prefix: String = "") {
        if (document.uri in indexingUris) return
        indexingUris = indexingUris + document.uri
        notice = "Indexing ${document.name}…"
        scope.launch {
            val result = runCatching { index(document) }
            indexingUris = indexingUris - document.uri
            refresh++
            notice = result.fold(
                onSuccess = { prefix + it },
                onFailure = { "Mayra could not index this document: ${it.message.orEmpty()}" }
            )
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !addingDocument) {
            addingDocument = true
            notice = "Adding and indexing document…"
            scope.launch {
                val result = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        store.add(uri)
                    }
                }
                result.onSuccess { document ->
                    addingDocument = false
                    refresh++
                    startIndex(document, prefix = "Document added. ")
                }.onFailure {
                    addingDocument = false
                    notice = "Mayra could not keep access to this document: ${it.message.orEmpty()}"
                }
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
                "Keep selected documents available to Mayra on this device. Plain-text and text-based PDF files are indexed locally for search and chat; nothing is uploaded by this feature."
            )

            Button(
                onClick = {
                    picker.launch(
                        arrayOf(
                            "application/pdf",
                            "text/*",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/json",
                            "application/xml"
                        )
                    )
                },
                enabled = !addingDocument,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (addingDocument) "Adding and indexing…" else "Add PDF or document")
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search title or indexed text") },
                supportingText = {
                    Text("Try a name, topic, invoice number, person or phrase.")
                },
                singleLine = true
            )
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Text(
                if (search.isBlank()) "${documents.size} saved document${if (documents.size == 1) "" else "s"}"
                else "${documents.size} search result${if (documents.size == 1) "" else "s"}",
                style = MaterialTheme.typography.labelLarge
            )

            if (documents.isEmpty()) {
                Text(
                    if (search.isBlank()) "No documents yet."
                    else "No matching title or indexed text found."
                )
            } else {
                documents.forEach { hit ->
                    val indexed = contentStore.get(hit.document.uri)
                    val isIndexing = hit.document.uri in indexingUris
                    DocumentCard(
                        document = hit.document,
                        snippet = if (search.isNotBlank()) hit.snippet else indexed?.text.orEmpty().take(220),
                        indexedContent = indexed,
                        isIndexing = isIndexing,
                        onIndex = { startIndex(hit.document) },
                        onOpen = {
                            runCatching {
                                val documentUri = Uri.parse(hit.document.uri)
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            documentUri,
                                            hit.document.mimeType.ifBlank { "*/*" }
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                )
                                store.markOpened(hit.document.uri)
                                refresh++
                            }.onFailure {
                                notice = "No compatible app could open this document."
                            }
                        },
                        onRemove = {
                            contentStore.remove(hit.document.uri)
                            store.remove(hit.document.uri)
                            refresh++
                            notice = "Document and its local text index were removed."
                        }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Document intelligence status", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Ready: persistent library access, metadata, full-text search, plain-text and text-based PDF extraction, snippets, summaries and grounded document Q&A. Pending: DOC/DOCX parsing and OCR for scanned PDFs.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "In chat, try: ‘Search my documents for payment terms’ or ‘मेरी files में invoice search करो’.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(context, MayraDocumentHealthActivity::class.java))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Library Health")
                    }
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
    snippet: String,
    indexedContent: IndexedDocumentContent?,
    isIndexing: Boolean,
    onIndex: () -> Unit,
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
            Text(
                when {
                    isIndexing -> "Indexing locally…"
                    indexedContent == null -> "Text not indexed"
                    indexedContent.truncated -> "Indexed locally (${indexedContent.text.length} characters, limited)"
                    else -> "Indexed locally (${indexedContent.text.length} characters)"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (indexedContent == null && !isIndexing) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            if (snippet.isNotBlank()) {
                Text(
                    snippet.replace(Regex("\\s+"), " ").trim().let {
                        if (it.length > 220) it.take(220) + "…" else it
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onIndex, enabled = !isIndexing) {
                    Text(
                        when {
                            isIndexing -> "Indexing…"
                            indexedContent == null -> "Index text"
                            else -> "Re-index"
                        }
                    )
                }
                TextButton(onClick = onRemove, enabled = !isIndexing) { Text("Remove") }
                TextButton(onClick = onOpen, enabled = !isIndexing) { Text("Open") }
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
        val existingDocuments = list()
        val existing = existingDocuments.firstOrNull { it.uri == uri.toString() }
        val document = MayraDocument(
            uri = uri.toString(),
            name = metadata.first,
            mimeType = appContext.contentResolver.getType(uri).orEmpty(),
            sizeBytes = metadata.second,
            addedAt = existing?.addedAt ?: System.currentTimeMillis(),
            lastOpenedAt = existing?.lastOpenedAt ?: 0L
        )
        write(existingDocuments.filterNot { it.uri == uri.toString() } + document)
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
