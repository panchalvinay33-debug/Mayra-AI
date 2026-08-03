package ai.mayra.app.j1

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
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
    private var activationMessage by mutableStateOf("Ready to open Android Assistant setup")

    private val roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        refreshAssistantStatus()
        activationMessage = if (assistantSelected) {
            "Mayra is selected. Now use the phone assistant gesture/button."
        } else {
            "Mayra is still not selected. Tap Activate Mayra to open Assistant settings again."
        }
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
                        Spacer(Modifier.height(8.dp))
                        Text(activationMessage, style = MaterialTheme.typography.bodySmall)
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
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true &&
                !roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
            ) {
                val request = roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
                if (request.resolveActivity(packageManager) != null) {
                    activationMessage = "Opening Android Assistant selection…"
                    roleLauncher.launch(request)
                    return
                }
                activationMessage = "Assistant role screen is not exposed directly on this Motorola. Opening system settings…"
            } else if (roleManager?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true) {
                activationMessage = "Mayra is already selected. Opening system Assistant settings…"
            } else {
                activationMessage = "Android reports that the Assistant role is unavailable. Opening system settings…"
            }
        }

        openFirstAvailableSettings()
    }

    private fun openFirstAvailableSettings() {
        val candidates = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        for (intent in candidates) {
            if (intent.resolveActivity(packageManager) != null) {
                try {
                    startActivity(intent)
                    activationMessage = when (intent.action) {
                        Settings.ACTION_VOICE_INPUT_SETTINGS -> "Opened Voice input settings. Select Mayra as the Assistant."
                        Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS -> "Opened Default apps. Choose Digital assistant app, then Mayra."
                        else -> "Opened Android Settings. Search for ‘Digital assistant app’ and select Mayra."
                    }
                    return
                } catch (_: ActivityNotFoundException) {
                    // Try the next official Settings screen.
                }
            }
        }

        activationMessage = "Motorola did not expose any Assistant settings screen. This result is now visible for diagnosis."
    }
}
