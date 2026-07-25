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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

/**
 * Main living Mayra character. The face is intentionally vector-based so it can animate smoothly,
 * remain sharp on every device, and later accept a final generated avatar texture without changing
 * the surrounding state/animation contract.
 */
@Composable
fun MayraCharacterPresence(
    state: MayraPresenceState,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mayra-character")
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(state.pulseDurationMillis()),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mayra-breathe"
    )
    val halo by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mayra-halo"
    )
    val eyePulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (state == MayraPresenceState.LISTENING) 520 else 1_500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mayra-eye-pulse"
    )

    Box(modifier = modifier.size(244.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(244.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val primary = state.primaryColor()
            val skin = Color(0xFFF3B78F)
            val skinShade = Color(0xFFE49B73)
            val hair = Color(0xFF2A1715)
            val hairSoft = Color(0xFF422421)
            val red = Color(0xFFCF1738)

            drawCircle(primary.copy(alpha = 0.08f + halo * 0.08f), size.minDimension * 0.45f, center)
            drawCircle(
                primary.copy(alpha = 0.18f + halo * 0.08f),
                size.minDimension * 0.39f * breathe,
                center,
                style = Stroke(width = 5f)
            )
            drawCircle(primary.copy(alpha = 0.10f), size.minDimension * 0.33f * breathe, center)

            // Hair bun and ears.
            drawCircle(hairSoft, size.minDimension * 0.115f, Offset(center.x, center.y - size.minDimension * 0.30f))
            drawCircle(hair, size.minDimension * 0.085f, Offset(center.x, center.y - size.minDimension * 0.32f))
            drawCircle(skinShade, size.minDimension * 0.070f, Offset(center.x - size.minDimension * 0.25f, center.y - size.minDimension * 0.015f))
            drawCircle(skinShade, size.minDimension * 0.070f, Offset(center.x + size.minDimension * 0.25f, center.y - size.minDimension * 0.015f))

            // Hair silhouette and face.
            drawOval(
                hair,
                topLeft = Offset(center.x - size.minDimension * 0.285f, center.y - size.minDimension * 0.29f),
                size = Size(size.minDimension * 0.57f, size.minDimension * 0.58f)
            )
            drawOval(
                skin,
                topLeft = Offset(center.x - size.minDimension * 0.235f, center.y - size.minDimension * 0.22f),
                size = Size(size.minDimension * 0.47f, size.minDimension * 0.48f)
            )

            // Side hair curves.
            drawArc(
                hairSoft,
                startAngle = 110f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x - size.minDimension * 0.28f, center.y - size.minDimension * 0.23f),
                size = Size(size.minDimension * 0.23f, size.minDimension * 0.42f),
                style = Stroke(width = size.minDimension * 0.055f)
            )
            drawArc(
                hairSoft,
                startAngle = -60f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(center.x + size.minDimension * 0.05f, center.y - size.minDimension * 0.23f),
                size = Size(size.minDimension * 0.23f, size.minDimension * 0.42f),
                style = Stroke(width = size.minDimension * 0.055f)
            )

            // Eyes inspired by the supplied Mayra reference face.
            val eyeY = center.y - size.minDimension * 0.045f
            val eyeOffset = size.minDimension * 0.105f
            val eyeRadius = size.minDimension * 0.074f * eyePulse
            listOf(center.x - eyeOffset, center.x + eyeOffset).forEach { eyeX ->
                drawCircle(Color.White, eyeRadius * 1.08f, Offset(eyeX, eyeY))
                drawCircle(Color(0xFF49302A), eyeRadius * 0.72f, Offset(eyeX, eyeY + eyeRadius * 0.08f))
                drawCircle(Color(0xFF121010), eyeRadius * 0.43f, Offset(eyeX, eyeY + eyeRadius * 0.10f))
                drawCircle(Color.White, eyeRadius * 0.17f, Offset(eyeX - eyeRadius * 0.16f, eyeY - eyeRadius * 0.19f))
            }

            // Brows react subtly to attention/thinking states.
            val browLift = if (state == MayraPresenceState.NEEDS_ATTENTION) -size.minDimension * 0.018f else 0f
            drawArc(
                hair,
                205f,
                120f,
                false,
                Offset(center.x - size.minDimension * 0.18f, eyeY - size.minDimension * 0.105f + browLift),
                Size(size.minDimension * 0.14f, size.minDimension * 0.08f),
                style = Stroke(width = 6f)
            )
            drawArc(
                hair,
                215f,
                120f,
                false,
                Offset(center.x + size.minDimension * 0.04f, eyeY - size.minDimension * 0.105f + browLift),
                Size(size.minDimension * 0.14f, size.minDimension * 0.08f),
                style = Stroke(width = 6f)
            )

            // Nose and cheeks.
            drawCircle(Color(0xFFE9957B).copy(alpha = 0.42f), size.minDimension * 0.036f, Offset(center.x - size.minDimension * 0.15f, center.y + size.minDimension * 0.075f))
            drawCircle(Color(0xFFE9957B).copy(alpha = 0.42f), size.minDimension * 0.036f, Offset(center.x + size.minDimension * 0.15f, center.y + size.minDimension * 0.075f))
            drawCircle(skinShade.copy(alpha = 0.65f), size.minDimension * 0.015f, Offset(center.x, center.y + size.minDimension * 0.035f))

            // Mouth changes between speaking and calm states.
            if (state == MayraPresenceState.SPEAKING) {
                drawOval(
                    Color(0xFF7A2532),
                    Offset(center.x - size.minDimension * 0.047f, center.y + size.minDimension * 0.105f),
                    Size(size.minDimension * 0.094f, size.minDimension * 0.075f)
                )
            } else {
                drawArc(
                    Color(0xFFB73A4E),
                    18f,
                    144f,
                    false,
                    Offset(center.x - size.minDimension * 0.065f, center.y + size.minDimension * 0.075f),
                    Size(size.minDimension * 0.13f, size.minDimension * 0.095f),
                    style = Stroke(width = 6f)
                )
            }

            // Neck and red outfit reference.
            drawRoundRect(
                skinShade,
                topLeft = Offset(center.x - size.minDimension * 0.045f, center.y + size.minDimension * 0.19f),
                size = Size(size.minDimension * 0.09f, size.minDimension * 0.095f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.minDimension * 0.025f)
            )
            val dress = Path().apply {
                moveTo(center.x - size.minDimension * 0.20f, center.y + size.minDimension * 0.24f)
                lineTo(center.x + size.minDimension * 0.20f, center.y + size.minDimension * 0.24f)
                lineTo(center.x + size.minDimension * 0.27f, center.y + size.minDimension * 0.43f)
                lineTo(center.x - size.minDimension * 0.27f, center.y + size.minDimension * 0.43f)
                close()
            }
            drawPath(dress, red)
            drawCircle(Color(0xFFFFD54F), size.minDimension * 0.017f, Offset(center.x, center.y + size.minDimension * 0.255f))

            if (state == MayraPresenceState.LISTENING || state == MayraPresenceState.SPEAKING) {
                drawArc(
                    primary.copy(alpha = 0.72f),
                    -55f,
                    110f,
                    false,
                    Offset(center.x + size.minDimension * 0.27f, center.y - size.minDimension * 0.10f),
                    Size(size.minDimension * 0.14f, size.minDimension * 0.20f),
                    style = Stroke(width = 5f)
                )
                drawArc(
                    primary.copy(alpha = 0.38f),
                    -55f,
                    110f,
                    false,
                    Offset(center.x + size.minDimension * 0.30f, center.y - size.minDimension * 0.15f),
                    Size(size.minDimension * 0.21f, size.minDimension * 0.30f),
                    style = Stroke(width = 4f)
                )
            }
        }
    }
}

/** Kept for compact surfaces such as the future floating assistive ball. */
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
            drawCircle(primary.copy(alpha = 0.10f * (1f - wave)), baseRadius * (1.05f + wave * 0.9f), center, style = Stroke(width = 4f))
            drawCircle(primary.copy(alpha = 0.16f), baseRadius * pulse * 1.32f, center)
            drawCircle(primary.copy(alpha = 0.28f), baseRadius * pulse * 1.05f, center)
            drawCircle(primary, baseRadius * pulse * 0.78f, center)
            drawCircle(Color.White.copy(alpha = 0.86f), baseRadius * 0.20f, center.copy(x = center.x - baseRadius * 0.18f, y = center.y - baseRadius * 0.18f))
        }
    }
}

private fun MayraPresenceState.pulseDurationMillis(): Int = when (this) {
    MayraPresenceState.LISTENING -> 620
    MayraPresenceState.UNDERSTANDING, MayraPresenceState.THINKING -> 900
    MayraPresenceState.SPEAKING -> 520
    MayraPresenceState.NEEDS_ATTENTION -> 740
    MayraPresenceState.IDLE, MayraPresenceState.OFFLINE -> 1_800
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