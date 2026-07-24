package ai.mayra.app.runtime

import ai.mayra.app.MayraRuntime
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
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

internal fun notificationScanMessage(posted: Boolean): String =
    if (posted) "Runtime alert posted." else "No new runtime alert to post."

internal fun notificationScanBlockedMessage(readiness: RuntimeNotificationReadiness): String = when (readiness) {
    RuntimeNotificationReadiness.READY -> ""
    RuntimeNotificationReadiness.PERMISSION_REQUIRED -> "Grant notification permission before scanning."
    RuntimeNotificationReadiness.SYSTEM_BLOCKED -> "Enable notifications in system settings before scanning."
}

@Composable
internal fun RuntimeNotificationControlsCard() {
    val context = LocalContext.current
    val preferences = remember(context) { RuntimeAttentionPreferences(context) }
    var preferenceState by remember { mutableStateOf(preferences.read()) }
    var readiness by remember { mutableStateOf(notificationReadiness(context)) }
    var notice by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        readiness = notificationReadiness(context)
        notice = if (granted) {
            "Notification permission granted."
        } else {
            "Notification permission was not granted."
        }
    }

    fun reload(message: String? = null) {
        preferenceState = preferences.read()
        readiness = notificationReadiness(context)
        notice = message
    }

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Runtime alerts", fontWeight = FontWeight.SemiBold)
            Text(preferenceState.status(System.currentTimeMillis()))
            Text(notificationReadinessMessage(readiness))
            notice?.let { Text(it) }

            when (readiness) {
                RuntimeNotificationReadiness.PERMISSION_REQUIRED -> {
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                context.startActivity(notificationSettingsIntent(context))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Grant notification permission") }
                }
                RuntimeNotificationReadiness.SYSTEM_BLOCKED -> {
                    OutlinedButton(
                        onClick = { context.startActivity(notificationSettingsIntent(context)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open notification settings") }
                }
                RuntimeNotificationReadiness.READY -> Unit
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!preferenceState.enabled || !preferenceState.canNotify(System.currentTimeMillis())) {
                    OutlinedButton(
                        onClick = {
                            preferences.resume()
                            reload("Runtime alerts resumed.")
                        }
                    ) { Text("Resume") }
                } else {
                    OutlinedButton(
                        onClick = {
                            preferences.snooze(System.currentTimeMillis())
                            reload("Runtime alerts snoozed for 1 hour.")
                        }
                    ) { Text("Snooze 1h") }
                    OutlinedButton(
                        onClick = {
                            preferences.setEnabled(false)
                            reload("Runtime alerts turned off.")
                        }
                    ) { Text("Turn off") }
                }
            }

            TextButton(
                onClick = {
                    val currentReadiness = notificationReadiness(context)
                    val message = if (currentReadiness != RuntimeNotificationReadiness.READY) {
                        notificationScanBlockedMessage(currentReadiness)
                    } else {
                        runCatching {
                            check(MayraRuntime.installed) { "Mayra runtime is not installed yet." }
                            notificationScanMessage(
                                RuntimeAttentionNotifier.scanAndNotify(
                                    context,
                                    MayraRuntime.controlCenter.snapshot()
                                )
                            )
                        }.getOrElse { it.message ?: "Runtime notification scan failed." }
                    }
                    reload(message)
                }
            ) { Text("Scan now") }
        }
    }
}
