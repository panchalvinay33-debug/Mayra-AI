package ai.mayra.app.presence

import ai.mayra.app.voice.VoiceState
import ai.mayra.app.voice.VoiceTransportState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Human-readable state used by every Mayra surface, not only the chat screen. */
enum class MayraPresenceState(val label: String, val prompt: String) {
    IDLE("Ready", "Tap me or say what you need."),
    LISTENING("Listening", "I’m listening…"),
    UNDERSTANDING("Understanding", "Let me understand that."),
    THINKING("Thinking", "I’m working on it."),
    SPEAKING("Speaking", "You can interrupt me anytime."),
    NEEDS_ATTENTION("Needs attention", "I need your approval to continue."),
    OFFLINE("Offline-ready", "Phone actions still work locally.")
}

internal fun mayraPresenceState(
    assistantThinking: Boolean,
    voiceState: VoiceState,
    pendingAttention: Boolean = false,
    onlineProviderReady: Boolean = true
): MayraPresenceState = when {
    pendingAttention -> MayraPresenceState.NEEDS_ATTENTION
    voiceState.isSpeaking -> MayraPresenceState.SPEAKING
    voiceState.isListening -> MayraPresenceState.LISTENING
    voiceState.transportState == VoiceTransportState.PROCESSING -> MayraPresenceState.UNDERSTANDING
    assistantThinking -> MayraPresenceState.THINKING
    !onlineProviderReady -> MayraPresenceState.OFFLINE
    else -> MayraPresenceState.IDLE
}

internal fun proactiveGreeting(userName: String, hourOfDay: Int): String {
    val name = userName.trim().takeIf { it.isNotEmpty() }?.let { ", $it" }.orEmpty()
    val greeting = when (hourOfDay) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }
    return "$greeting$name. I’m awake and ready."
}

@Composable
fun MayraPresenceOrb(
    state: MayraPresenceState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mayra-presence")
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = state.pulseDurationMillis()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mayra-pulse"
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_800),
            repeatMode = RepeatMode.Restart
        ),
        label = "mayra-wave"
    )

    Box(modifier = modifier.size(168.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(168.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension * 0.25f
            val primary = state.primaryColor()

            drawCircle(
                color = primary.copy(alpha = 0.10f * (1f - wave)),
                radius = baseRadius * (1.05f + wave * 0.9f),
                center = center,
                style = Stroke(width = 4f)
            )
            drawCircle(
                color = primary.copy(alpha = 0.16f),
                radius = baseRadius * pulse * 1.32f,
                center = center
            )
            drawCircle(
                color = primary.copy(alpha = 0.28f),
                radius = baseRadius * pulse * 1.05f,
                center = center
            )
            drawCircle(
                color = primary,
                radius = baseRadius * pulse * 0.78f,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.86f),
                radius = baseRadius * 0.20f,
                center = center.copy(
                    x = center.x - baseRadius * 0.18f,
                    y = center.y - baseRadius * 0.18f
                )
            )
        }
    }
}

private fun MayraPresenceState.pulseDurationMillis(): Int = when (this) {
    MayraPresenceState.LISTENING -> 620
    MayraPresenceState.UNDERSTANDING,
    MayraPresenceState.THINKING -> 900
    MayraPresenceState.SPEAKING -> 520
    MayraPresenceState.NEEDS_ATTENTION -> 740
    MayraPresenceState.IDLE,
    MayraPresenceState.OFFLINE -> 1_800
}

private fun MayraPresenceState.primaryColor(): Color = when (this) {
    MayraPresenceState.IDLE -> Color(0xFF6554C0)
    MayraPresenceState.LISTENING -> Color(0xFF00897B)
    MayraPresenceState.UNDERSTANDING -> Color(0xFF1976D2)
    MayraPresenceState.THINKING -> Color(0xFF5E35B1)
    MayraPresenceState.SPEAKING -> Color(0xFFD81B60)
    MayraPresenceState.NEEDS_ATTENTION -> Color(0xFFF57C00)
    MayraPresenceState.OFFLINE -> Color(0xFF546E7A)
}
