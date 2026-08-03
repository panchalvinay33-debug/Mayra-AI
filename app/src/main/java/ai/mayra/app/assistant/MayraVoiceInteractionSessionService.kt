package ai.mayra.app.assistant

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import ai.mayra.app.BuildConfig

class MayraVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = MayraVoiceInteractionSession(this)
}

/**
 * Lightweight animated assistant surface. J1 uses it only for lifecycle/orb proof. J2 can enable a
 * bounded invocation-time on-device speech recognizer through BuildConfig without turning the
 * always-running VoiceInteractionService into a heavy or continuous microphone process.
 */
class MayraVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private var pulse: AnimatorSet? = null
    private var orbView: View? = null
    private var stateLabel: TextView? = null
    private var recognizer: MayraOnDeviceSpeechRecognizer? = null
    private var voiceState: MayraVoiceSessionState = MayraVoiceSessionState.Idle

    override fun onCreate() {
        super.onCreate()
        setKeepAwake(false)
    }

    override fun onCreateContentView(): View {
        val density = context.resources.displayMetrics.density
        val root = FrameLayout(context).apply {
            setPadding(24.dp(density), 24.dp(density), 24.dp(density), 56.dp(density))
            isClickable = true
            isFocusable = true
            contentDescription = "Mayra assistant surface. Tap to close."
            setOnClickListener { hide() }
        }

        val orb = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(76, 90, 220))
                setStroke(3.dp(density), Color.argb(180, 220, 225, 255))
            }
            contentDescription = "Mayra assistant. Tap to close."
            isClickable = true
            setOnClickListener { hide() }
        }
        orbView = orb
        val orbSize = 104.dp(density)
        root.addView(
            orb,
            FrameLayout.LayoutParams(orbSize, orbSize).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 78.dp(density)
            }
        )

        val label = TextView(context).apply {
            text = "Mayra"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { hide() }
        }
        root.addView(
            label,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 42.dp(density)
            }
        )

        stateLabel = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.argb(220, 238, 240, 255))
            gravity = Gravity.CENTER
            maxLines = 2
            isClickable = true
            setOnClickListener { hide() }
        }
        root.addView(
            stateLabel,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 14.dp(density)
                marginStart = 20.dp(density)
                marginEnd = 20.dp(density)
            }
        )

        startPulse(orb)
        renderVoiceState()
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        setKeepAwake(true)
        orbView?.let { if (pulse == null) startPulse(it) }
        if (BuildConfig.VOICE_SESSION_RECOGNITION_ENABLED) {
            startInvocationRecognition()
        } else {
            setVoiceState(MayraVoiceSessionState.Idle)
        }
    }

    override fun onBackPressed() {
        hide()
    }

    override fun onHide() {
        stopRecognition()
        setKeepAwake(false)
        pulse?.cancel()
        pulse = null
        super.onHide()
    }

    override fun onDestroy() {
        stopRecognition()
        pulse?.cancel()
        pulse = null
        orbView = null
        stateLabel = null
        super.onDestroy()
    }

    private fun startInvocationRecognition() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            setVoiceState(MayraVoiceSessionState.PermissionRequired)
            return
        }

        stopRecognition()
        val next = MayraOnDeviceSpeechRecognizer(context) { state -> setVoiceState(state) }
        recognizer = next
        next.start()
    }

    private fun stopRecognition() {
        recognizer?.stop()
        recognizer = null
    }

    private fun setVoiceState(state: MayraVoiceSessionState) {
        voiceState = state
        renderVoiceState()
    }

    private fun renderVoiceState() {
        stateLabel?.text = if (BuildConfig.VOICE_SESSION_RECOGNITION_ENABLED) {
            voiceState.primaryText()
        } else {
            "Tap or Back to close"
        }
    }

    private fun startPulse(orb: View) {
        pulse?.cancel()
        val scaleX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 0.92f, 1.08f, 0.92f).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 0.92f, 1.08f, 0.92f).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        val alpha = ObjectAnimator.ofFloat(orb, View.ALPHA, 0.72f, 1f, 0.72f).apply {
            repeatCount = ObjectAnimator.INFINITE
        }
        pulse = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1400L
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun Int.dp(density: Float): Int = (this * density).toInt()
}
