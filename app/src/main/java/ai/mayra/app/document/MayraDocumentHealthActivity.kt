package ai.mayra.app.document

import ai.mayra.app.ui.theme.MayraAITheme
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MayraDocumentHealthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                DocumentHealthScreen(onClose = ::finish)
            }
        }
    }
}

@Composable
private fun DocumentHealthScreen(onClose: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val documentStore = remember(context) { MayraDocumentStore(context) }
    val contentStore = remember(context) { MayraDocumentContentStore(context) }
    val maintenance = remember(context) {
        MayraDocumentMaintenance(
            documentStore = documentStore,
            contentStore = contentStore,
            extractor = MayraDocumentTextExtractor(context)
        )
    }
    var inventoryRefresh by remember { mutableIntStateOf(0) }
    val inventory = remember(inventoryRefresh) {
        val documents = documentStore.list()
        MayraDocumentInventory.build(
            documents = documents,
            indexedUris = documents.mapNotNullTo(mutableSetOf()) { document ->
                document.uri.takeIf { contentStore.get(it) != null }
            }
        )
    }
    var report by remember { mutableStateOf<DocumentMaintenanceReport?>(null) }
    var running by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

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
                "Mayra Library Health",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Review parser readiness and rebuild the private on-device document index. " +
                    "This operation does not upload document content."
            )

            LibraryInventoryCard(inventory)

            Button(
                onClick = {
                    if (running) return@Button
                    running = true
                    error = null
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { maintenance.rebuildAll() }
                        }.onSuccess {
                            report = it
                            inventoryRefresh++
                        }.onFailure {
                            error = it.message ?: "Library maintenance failed."
                        }
                        running = false
                    }
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (running) "Checking…" else "Re-index complete library")
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            report?.let { MaintenanceReportCard(it) }

            Text("Parser capabilities", fontWeight = FontWeight.SemiBold)
            MayraDocumentParserCatalog.capabilities.forEach { capability ->
                ParserCapabilityCard(capability)
            }

            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun LibraryInventoryCard(inventory: DocumentLibraryInventory) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Current library", fontWeight = FontWeight.SemiBold)
            Text(inventory.userMessage())
            if (inventory.totalDocuments == 0) {
                Text("Add a document from Mayra Library to begin local indexing.")
            } else {
                val labels = MayraDocumentParserCatalog.capabilities.associate { it.id to it.label }
                inventory.formatCounts.forEach { (id, count) ->
                    Text(
                        "${labels[id] ?: "Unknown format"}: $count",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MaintenanceReportCard(report: DocumentMaintenanceReport) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                if (report.healthy) "Library check complete ✓" else "Library needs attention",
                fontWeight = FontWeight.SemiBold
            )
            Text(report.userMessage())
            if (report.removedOrphanedIndexes > 0) {
                Text(
                    "Removed ${report.removedOrphanedIndexes} stale local index" +
                        if (report.removedOrphanedIndexes == 1) "." else "es.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (report.messages.isNotEmpty()) {
                Text("Diagnostics", fontWeight = FontWeight.Medium)
                report.messages.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
private fun ParserCapabilityCard(capability: DocumentParserCapability) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(capability.label, fontWeight = FontWeight.SemiBold)
                Text(
                    when (capability.state) {
                        ParserCapabilityState.READY -> "Ready"
                        ParserCapabilityState.FOUNDATION_ONLY -> "Foundation"
                        ParserCapabilityState.PLANNED -> "Planned"
                    },
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(capability.note, style = MaterialTheme.typography.bodySmall)
            if (capability.extensions.isNotEmpty()) {
                Text(
                    capability.extensions.sorted().joinToString(prefix = "Formats: ", separator = ", "),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
