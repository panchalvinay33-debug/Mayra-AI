package ai.mayra.app.j1

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    private var activationMessage by mutableStateOf(
        "Tap Activate Mayra. Then open Digital assistant and choose Mayra."
    )

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
                        Text(
                            "Mayra J1 Assistant Test",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "This small build asks for no runtime permissions. It only checks whether this Motorola can select Mayra as the Android Assistant and show the animated assistant session."
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            if (assistantSelected) "Status: Mayra is selected ✓"
                            else "Status: Mayra is not selected"
                        )
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
                        Text(
                            "Motorola path: Settings → Apps → Default apps → Digital assistant → Mayra. After selecting Mayra, use the phone's normal assistant gesture/button. The expected result is the Mayra orb.",
                            style = MaterialTheme.typography.bodySmall
                        )
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

        activationMessage = if (assistantSelected) {
            "Mayra is selected. Use the phone assistant gesture/button to invoke her."
        } else {
            "Tap Activate Mayra. In Default apps, tap Digital assistant and choose Mayra."
        }
    }

    private fun openAssistantSetup() {
        val defaultAppsIntent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        try {
            startActivity(defaultAppsIntent)
            activationMessage = "Opened Default apps. Tap Digital assistant, then choose Mayra."
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to the general Settings screen.
        }

        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            activationMessage = "Opened Settings. Go to Apps → Default apps → Digital assistant → Mayra."
        } catch (_: ActivityNotFoundException) {
            activationMessage = "Could not open Android Settings. Open Settings manually: Apps → Default apps → Digital assistant → Mayra."
        }
    }
}
