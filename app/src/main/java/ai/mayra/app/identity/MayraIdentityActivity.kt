package ai.mayra.app.identity

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

class MayraIdentityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MayraAITheme { MayraIdentityScreen(onClose = ::finish) } }
    }
}

@Composable
private fun MayraIdentityScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MayraContactIdentityStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val identities = remember(refresh) { store.all() }
    var canonicalName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var aliases by remember { mutableStateOf("") }
    var trust by remember { mutableStateOf(MayraContactTrust.STANDARD) }
    var channel by remember { mutableStateOf(MayraCommunicationChannel.ASK_EVERY_TIME) }
    var notice by remember { mutableStateOf<String?>(null) }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Mayra People & Relationships", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Map natural names like Mummy, Papa, Boss or Doctor to the exact contact name already saved in your phone.")

            notice?.let { Card(Modifier.fillMaxWidth()) { Text(it, Modifier.padding(12.dp)) } }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Add identity", fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = canonicalName,
                        onValueChange = { canonicalName = it.take(120) },
                        label = { Text("Exact Android contact name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it.take(80) },
                        label = { Text("Relationship, e.g. Mummy") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = aliases,
                        onValueChange = { aliases = it.take(300) },
                        label = { Text("Other names, comma separated") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Trust level", fontWeight = FontWeight.Medium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MayraContactTrust.entries.forEach { value ->
                            OutlinedButton(onClick = { trust = value }, modifier = Modifier.weight(1f)) {
                                Text(if (trust == value) "✓ ${value.name.lowercase()}" else value.name.lowercase())
                            }
                        }
                    }
                    Text("Preferred channel", fontWeight = FontWeight.Medium)
                    MayraCommunicationChannel.entries.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { value ->
                                OutlinedButton(onClick = { channel = value }, modifier = Modifier.weight(1f)) {
                                    Text(if (channel == value) "✓ ${value.name.replace('_', ' ').lowercase()}" else value.name.replace('_', ' ').lowercase())
                                }
                            }
                            if (row.size == 1) Column(Modifier.weight(1f)) {}
                        }
                    }
                    Button(
                        onClick = {
                            val cleanName = canonicalName.trim()
                            if (cleanName.isBlank()) {
                                notice = "Enter the exact contact name saved in Android Contacts."
                            } else {
                                store.upsert(
                                    MayraContactIdentity(
                                        canonicalContactName = cleanName,
                                        relationship = relationship.trim().takeIf(String::isNotBlank),
                                        aliases = aliases.split(',').map(String::trim).filter(String::isNotBlank).toSet(),
                                        preferredChannel = channel,
                                        trust = trust
                                    )
                                )
                                canonicalName = ""
                                relationship = ""
                                aliases = ""
                                trust = MayraContactTrust.STANDARD
                                channel = MayraCommunicationChannel.ASK_EVERY_TIME
                                notice = "Identity saved. Mayra will still verify the real contact through Android Contacts."
                                refresh++
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save identity") }
                }
            }

            Text("Saved people", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (identities.isEmpty()) Text("No relationship identities saved yet.")
            identities.forEach { identity ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(identity.relationship ?: identity.canonicalContactName, fontWeight = FontWeight.SemiBold)
                        Text("Android contact: ${identity.canonicalContactName}")
                        if (identity.aliases.isNotEmpty()) Text("Aliases: ${identity.aliases.joinToString()}")
                        Text(identitySafetySummary(identity), style = MaterialTheme.typography.bodySmall)
                        if (identity.trust == MayraContactTrust.SENSITIVE) {
                            Text("Sensitive identity: direct-owner handoff must not bypass confirmation.", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = {
                                store.remove(identity.id)
                                notice = "Identity removed. Android contact was not changed."
                                refresh++
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Remove identity") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Wrong-contact protection", fontWeight = FontWeight.SemiBold)
                    Text("• Multiple matching identities are never guessed.")
                    Text("• The alias maps to a contact name, not a privately copied phone number.")
                    Text("• Android Contacts remains the source of the actual number.")
                    Text("• Sensitive identities always preserve confirmation.")
                }
            }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}
