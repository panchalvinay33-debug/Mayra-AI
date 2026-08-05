package ai.mayra.app.launcher

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.mayra.app.MainActivity
import ai.mayra.app.ui.theme.MayraAITheme

/**
 * J5 launcher/Home foundation.
 *
 * This activity deliberately remains independent of local/cloud model startup. Basic Home and app
 * access must continue working if Mayra's heavy AI runtime is unavailable or crashes.
 */
class MayraLauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                MayraLauncherHome()
            }
        }
    }
}

internal data class LaunchableApp(
    val label: String,
    val packageName: String,
    val activityName: String
)

internal fun filterLaunchableApps(apps: List<LaunchableApp>, rawQuery: String): List<LaunchableApp> {
    val query = rawQuery.trim()
    if (query.isEmpty()) return apps
    return apps.filter {
        it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }
}

@Composable
private fun MayraLauncherHome() {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("Mayra Home ready") }
    val apps = remember(refreshKey) { loadLaunchableApps(context) }
    val filtered = remember(apps, query) { filterLaunchableApps(apps, query) }
    val roleManager = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(RoleManager::class.java) else null
    }
    val roleAvailable = remember(roleManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager?.isRoleAvailable(RoleManager.ROLE_HOME) == true else false
    }
    var roleHeld by remember(refreshKey, roleManager) {
        mutableStateOf(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true else false)
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshKey++
        roleHeld = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) roleManager?.isRoleHeld(RoleManager.ROLE_HOME) == true else false
        status = if (roleHeld) "Mayra is the default Home ✓" else "Home role not granted"
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, tonalElevation = 8.dp, modifier = Modifier.size(64.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("M", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Mayra Home", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(if (roleHeld) "Default Home ✓" else status, style = MaterialTheme.typography.bodySmall)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(
                    onClick = { context.startActivity(Intent(context, MainActivity::class.java)) },
                    label = { Text("Ask Mayra") }
                )
                AssistChip(onClick = { refreshKey++ }, label = { Text("Refresh apps") })
            }

            if (!roleHeld) {
                Button(
                    enabled = roleAvailable,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager != null) {
                            roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (roleAvailable) "Make Mayra default Home" else "Home role unavailable")
                }
            }

            OutlinedButton(
                onClick = {
                    runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS)) }
                        .onFailure { status = "Home settings are unavailable on this device" }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Switch / restore Home app")
            }

            Spacer(Modifier.height(2.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search apps…") },
                label = { Text("Apps") }
            )
            Text("${filtered.size} of ${apps.size} launchable apps", style = MaterialTheme.typography.bodySmall)

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { "${it.packageName}/${it.activityName}" }) { app ->
                    Card(
                        onClick = {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_LAUNCHER)
                                component = ComponentName(app.packageName, app.activityName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                            }
                            runCatching { context.startActivity(intent) }
                                .onSuccess { status = "Opened ${app.label}" }
                                .onFailure { status = "Could not open ${app.label}" }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                            Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Text(
                "Safe J5 foundation: Home/app access does not initialize the local model, cloud provider, memory or privileged action engine.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun loadLaunchableApps(context: Context): List<LaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return context.packageManager.queryIntentActivities(intent, 0)
        .asSequence()
        .mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            LaunchableApp(
                label = info.loadLabel(context.packageManager)?.toString()?.trim().orEmpty().ifBlank { activity.packageName },
                packageName = activity.packageName,
                activityName = activity.name
            )
        }
        .distinctBy { it.packageName to it.activityName }
        .sortedBy { it.label.lowercase() }
        .toList()
}
