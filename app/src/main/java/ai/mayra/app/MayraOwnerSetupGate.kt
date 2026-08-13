package ai.mayra.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat

private const val SETUP_PREFS = "mayra_owner_setup"
private const val SETUP_COMPLETE = "complete_v1"

/** One-time owner setup: required runtime permissions, then Android Assistant role. */
@Composable
fun MayraOwnerSetupGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE) }
    var setupComplete by remember { mutableStateOf(prefs.getBoolean(SETUP_COMPLETE, false)) }
    var refresh by remember { mutableIntStateOf(0) }

    if (setupComplete) {
        content()
        return
    }

    val permissions = remember {
        buildList {
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
    }
    val permissionsReady = remember(refresh) {
        permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }

    val roleManager = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) context.getSystemService(RoleManager::class.java) else null
    }
    val assistantRoleAvailable = remember(refresh, roleManager) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
    }
    val assistantRoleHeld = remember(refresh, roleManager) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refresh++
    }

    fun finishSetup() {
        prefs.edit().putBoolean(SETUP_COMPLETE, true).apply()
        setupComplete = true
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Mayra setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Bas do simple steps. Mayra sirf voice, contacts aur reminders ke liye zaroori permissions maangegi.")
        Spacer(Modifier.height(20.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Required permissions", fontWeight = FontWeight.Bold)
                Text(if (permissionsReady) "Ready ✓" else "Voice, contacts aur reminder notifications allow karein.")
                if (!permissionsReady) {
                    Button(onClick = { permissionLauncher.launch(permissions) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Allow required permissions")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2. Activate Mayra", fontWeight = FontWeight.Bold)
                Text(
                    when {
                        assistantRoleHeld -> "Mayra is your Android Assistant ✓"
                        assistantRoleAvailable -> "Android Assistant list kholkar Mayra select karein."
                        else -> "Assistant role is device par available nahi dikh raha; Mayra app phir bhi chalegi."
                    }
                )
                if (assistantRoleAvailable && !assistantRoleHeld) {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                roleManager?.let {
                                    roleLauncher.launch(it.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Activate Mayra") }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = ::finishSetup, modifier = Modifier.fillMaxWidth()) { Text("Start Mayra") }
        if (!permissionsReady || (assistantRoleAvailable && !assistantRoleHeld)) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = ::finishSetup, modifier = Modifier.fillMaxWidth()) { Text("Continue for now") }
        }
    }
}
