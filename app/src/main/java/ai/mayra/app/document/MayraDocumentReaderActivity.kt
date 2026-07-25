package ai.mayra.app.document

import ai.mayra.app.knowledge.MayraMemoryPrivacyGuard
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
import ai.mayra.app.ui.theme.MayraAITheme
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MayraDocumentReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.getStringExtra(EXTRA_URI).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty().ifBlank { "Document" }
        val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE).orEmpty()
        setContent {
            MayraAITheme {
                DocumentReaderScreen(uri = uri, name = name, mimeType = mimeType, onClose = ::finish)
            }
        }
    }

    companion object {
        const val EXTRA_URI = "document_uri"
        const val EXTRA_NAME = "document_name"
        const val EXTRA_MIME_TYPE = "document_mime_type"
    }
}

@Composable
private fun DocumentReaderScreen(uri: String, name: String, mimeType: String, onClose: () -> Unit) {
    val context = LocalContext.current
    val reader = remember(context) { MayraDocumentTextReader(context) }
    val memory = remember(context) { MayraPersonalMemory(context) }
    val preview = remember(uri, mimeType) {
        if (uri.isBlank()) DocumentTextPreview.Error("Document reference is missing.")
        else reader.preview(Uri.parse(uri), mimeType)
    }
    var query by remember { mutableStateOf("") }
    var selectedText by remember { mutableStateOf("") }
    var noteTitle by remember { mutableStateOf(name.substringBeforeLast('.').take(80)) }
    var notice by remember { mutableStateOf<String?>(null) }

    val ready = preview as? DocumentTextPreview.Ready
    val matches = remember(ready, query) {
        if (ready == null) emptyList() else reader.search(ready, query)
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Local bounded text reader", style = MaterialTheme.typography.labelMedium)

            when (preview) {
                is DocumentTextPreview.Ready -> {
                    OutlinedTextField(
                        query,
                        { query = it.take(200) },
                        Modifier.fillMaxWidth(),
                        label = { Text("Search inside preview") },
                        singleLine = true
                    )
                    if (query.length >= 2) {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${matches.size} matches", fontWeight = FontWeight.SemiBold)
                                matches.forEach { match ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            "Line ${match.lineNumber}: ${match.preview}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = {
                                            selectedText = appendExcerpt(selectedText, match.preview)
                                            notice = "Line ${match.lineNumber} added to the reviewed excerpt."
                                        }) { Text("Use") }
                                    }
                                }
                                if (matches.isEmpty()) Text("No match in the bounded preview.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Preview", fontWeight = FontWeight.SemiBold)
                            Text(preview.text, style = MaterialTheme.typography.bodySmall)
                            if (preview.truncated) Text("Preview truncated for safety.", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Save an excerpt to Mayra Memory", fontWeight = FontWeight.SemiBold)
                            Text("Select a search result with Use, or paste and edit an excerpt. Nothing is saved automatically.", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                noteTitle,
                                { noteTitle = it.take(180) },
                                Modifier.fillMaxWidth(),
                                label = { Text("Memory title") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                selectedText,
                                { selectedText = it.take(MAX_EXCERPT_CHARACTERS) },
                                Modifier.fillMaxWidth(),
                                label = { Text("Reviewed useful excerpt") },
                                minLines = 4,
                                supportingText = { Text("${selectedText.length}/$MAX_EXCERPT_CHARACTERS characters") }
                            )
                            Button(
                                enabled = noteTitle.isNotBlank() && selectedText.isNotBlank(),
                                onClick = {
                                    val cleanTitle = noteTitle.trim()
                                    val cleanExcerpt = selectedText.trim()
                                    if (MayraMemoryPrivacyGuard.looksSensitive("$cleanTitle $cleanExcerpt")) {
                                        notice = "Not saved: credential-like or financial-secret content is blocked."
                                    } else {
                                        memory.saveNote(
                                            PersonalNote(
                                                type = PersonalNoteType.PROJECT_NOTE,
                                                title = cleanTitle,
                                                body = cleanExcerpt,
                                                tags = setOf("document", name.take(80)),
                                                sensitive = false
                                            )
                                        )
                                        selectedText = ""
                                        notice = "Document excerpt saved to Mayra Memory."
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save reviewed excerpt") }
                        }
                    }
                }
                is DocumentTextPreview.Unsupported -> Text(preview.reason)
                is DocumentTextPreview.Error -> Text(preview.reason)
                DocumentTextPreview.Empty -> Text("This document has no readable text in the supported preview format.")
            }

            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            Text("PDF and Word parsing are still not claimed. Only supported text-like files are read with strict size limits.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

internal fun appendExcerpt(current: String, line: String, maxCharacters: Int = MAX_EXCERPT_CHARACTERS): String {
    require(maxCharacters > 0)
    val cleanLine = line.trim()
    if (cleanLine.isBlank()) return current.take(maxCharacters)
    val combined = if (current.isBlank()) cleanLine else "${current.trimEnd()}\n$cleanLine"
    return combined.take(maxCharacters)
}

internal const val MAX_EXCERPT_CHARACTERS = 8_000
