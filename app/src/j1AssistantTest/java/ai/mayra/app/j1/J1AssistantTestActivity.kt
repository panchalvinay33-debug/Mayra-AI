package ai.mayra.app.j1

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

class J1AssistantTestActivity : ComponentActivity() {
    private var assistantSelected by mutableStateOf(false)

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshAssistantStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshAssistantStatus()
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Mayra J1 Assistant Test", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("This small build asks for no runtime permissions. It only checks whether this Motorola can select Mayra as the Android Assistant and show the animated assistant session.")
                        Spacer(Modifier.height(20.dp))
                        Text(if (assistantSelected) "Status: Mayra is selected ✓" else "Status: Mayra is not selected")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = ::openAssistantSetup, modifier = Modifier.fillMaxWidth()) {
                            Text(if (assistantSelected) "Open Assistant settings" else "Activate Mayra")
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = ::refreshAssistantStatus, modifier = Modifier.fillMaxWidth()) {
                            Text("Refresh status")
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("After selecting Mayra, use the phone's normal assistant gesture/button. The expected result is the Mayra orb. This build does not contain chat, contacts, reminders, provider, notification listener or background recovery.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAssistantStatus()
    }

    private fun refreshAssistantStatus() {
        assistantSelected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true &&
                roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        } else {
            false
        }
    }

    private fun openAssistantSetup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true && !roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)) {
                roleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))
                return
            }
        }
        runCatching { startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
            .recoverCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
}
