package ai.mayra.app.j2

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.SpeechRecognizer
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
import androidx.core.content.ContextCompat
import ai.mayra.app.ui.theme.MayraAITheme

class J2VoiceTestActivity : ComponentActivity() {
    private var assistantSelected by mutableStateOf(false)
    private var microphoneAllowed by mutableStateOf(false)
    private var onDeviceSpeechAvailable by mutableStateOf(false)
    private var statusMessage by mutableStateOf("Checking Mayra voice readiness…")

    private val microphoneLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshStatus()
        if (granted) {
            if (assistantSelected) {
                statusMessage = readyMessage()
            } else {
                statusMessage = "Microphone ready. Select Mayra J2 as Digital assistant."
                openAssistantSettings()
            }
        } else {
            statusMessage = "Microphone permission is required only for real voice listening."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshStatus()
        setContent {
            MayraAITheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Mayra J2 Voice Test", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("One small test for real Mayra voice: microphone + Android Digital Assistant + on-device speech recognition.")
                        Spacer(Modifier.height(20.dp))
                        Text("Assistant: ${if (assistantSelected) "Mayra J2 selected ✓" else "not selected"}")
                        Text("Microphone: ${if (microphoneAllowed) "allowed ✓" else "not allowed"}")
                        Text("On-device speech: ${if (onDeviceSpeechAvailable) "available ✓" else "not available"}")
                        Spacer(Modifier.height(12.dp))
                        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = ::prepareMayraVoice, modifier = Modifier.fillMaxWidth()) {
                            Text(primaryButtonText())
                        }
                        if (!assistantSelected) {
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = ::openAssistantSettings, modifier = Modifier.fillMaxWidth()) {
                                Text("Open Digital assistant settings")
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "When all three checks are ready, return Home and use the configured Power-button Digital assistant trigger. Mayra should show Listening… and then the words she heard. Back, tap, or lock closes the session.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
        if (assistantSelected && microphoneAllowed) statusMessage = readyMessage()
    }

    private fun refreshStatus() {
        microphoneAllowed = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        assistantSelected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true && roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT)
        } else {
            false
        }
        onDeviceSpeechAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
    }

    private fun prepareMayraVoice() {
        refreshStatus()
        when {
            !microphoneAllowed -> microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
            !assistantSelected -> openAssistantSettings()
            else -> statusMessage = readyMessage()
        }
    }

    private fun openAssistantSettings() {
        statusMessage = "Opening Settings → Apps → Default apps. Choose Digital assistant app → Mayra J2 Voice Test."
        runCatching { startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)) }
            .recoverCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
            .onFailure { statusMessage = "Open Settings → Apps → Default apps → Digital assistant app → Mayra J2 Voice Test." }
    }

    private fun primaryButtonText(): String = when {
        !microphoneAllowed -> "Enable Mayra Voice"
        !assistantSelected -> "Activate Mayra J2"
        else -> "Mayra Voice Ready ✓"
    }

    private fun readyMessage(): String = if (onDeviceSpeechAvailable) {
        "Ready. Go Home and hold the configured Power-button assistant trigger, then speak."
    } else {
        "Assistant and microphone are ready, but Android reports no on-device speech recognizer on this phone."
    }
}
