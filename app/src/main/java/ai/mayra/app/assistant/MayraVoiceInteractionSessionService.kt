package ai.mayra.app.assistant

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.KeyguardManager
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

class MayraVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private var pulse: AnimatorSet? = null
    private var orbView: View? = null
    private var nameLabel: TextView? = null
    private var stateLabel: TextView? = null
    private var recognizer: MayraOnDeviceSpeechRecognizer? = null
    private var ttsSpeaker: MayraOfflineTtsSpeaker? = null
    private var voiceState: MayraVoiceSessionState = MayraVoiceSessionState.Idle

    override fun onCreate() {
        super.onCreate()
        setKeepAwake(false)
        if (BuildConfig.VOICE_SESSION_RECOGNITION_ENABLED) {
            ttsSpeaker = MayraOfflineTtsSpeaker(context)
        }
    }

    override fun onCreateContentView(): View {
        val density = context.resources.displayMetrics.density
        val root = FrameLayout(context).apply {
            setPadding(24.dp(density), 24.dp(density), 24.dp(density), 72.dp(density))
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
        root.addView(orb, FrameLayout.LayoutParams(orbSize, orbSize).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 102.dp(density)
        })

        nameLabel = TextView(context).apply {
            text = "Mayra"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isClickable = true
            setOnClickListener { hide() }
        }
        root.addView(nameLabel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 66.dp(density)
        })

        stateLabel = TextView(context).apply {
            textSize = 13f
            setTextColor(Color.argb(220, 238, 240, 255))
            gravity = Gravity.CENTER
            maxLines = 2
            isClickable = true
            setOnClickListener { hide() }
        }
        root.addView(stateLabel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 18.dp(density)
            marginStart = 28.dp(density)
            marginEnd = 28.dp(density)
        })

        startPulse(orb)
        renderVoiceState()
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        setKeepAwake(true)
        orbView?.let { if (pulse == null) startPulse(it) }
        if (BuildConfig.VOICE_SESSION_RECOGNITION_ENABLED) startInvocationRecognition()
        else setVoiceState(MayraVoiceSessionState.Idle)
    }

    override fun onBackPressed() = hide()

    override fun onHide() {
        stopRecognition()
        ttsSpeaker?.stop()
        setKeepAwake(false)
        pulse?.cancel()
        pulse = null
        super.onHide()
    }

    override fun onDestroy() {
        stopRecognition()
        ttsSpeaker?.shutdown()
        ttsSpeaker = null
        pulse?.cancel()
        pulse = null
        orbView = null
        nameLabel = null
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
        if (state is MayraVoiceSessionState.Heard) handleHeard(state.text)
    }

    private fun handleHeard(transcript: String) {
        val reply = MayraVoiceReplyPolicy.replyFor(transcript)
        if (isDeviceLocked()) {
            // Never expose or speak transcript-derived/private content before unlock.
            stateLabel?.text = "Heard you. Unlock to continue."
            ttsSpeaker?.speak("Maine suna. Phone unlock karke continue karein.")
        } else {
            stateLabel?.text = reply.text
            ttsSpeaker?.speak(reply.text)
        }
    }

    private fun renderVoiceState() {
        val label = stateLabel ?: return
        label.text = when {
            !BuildConfig.VOICE_SESSION_RECOGNITION_ENABLED -> "Tap or Back to close"
            isDeviceLocked() -> when (voiceState) {
                is MayraVoiceSessionState.Listening,
                is MayraVoiceSessionState.Partial,
                is MayraVoiceSessionState.Processing,
                is MayraVoiceSessionState.Preparing -> "Listening…"
                is MayraVoiceSessionState.Heard -> "Heard you. Unlock to continue."
                else -> voiceState.primaryText().takeUnless { it.startsWith("Heard:") } ?: "Mayra is ready"
            }
            else -> voiceState.primaryText()
        }
    }

    private fun isDeviceLocked(): Boolean =
        (context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked

    private fun startPulse(orb: View) {
        pulse?.cancel()
        val scaleX = ObjectAnimator.ofFloat(orb, View.SCALE_X, 0.92f, 1.08f, 0.92f).apply { repeatCount = ObjectAnimator.INFINITE }
        val scaleY = ObjectAnimator.ofFloat(orb, View.SCALE_Y, 0.92f, 1.08f, 0.92f).apply { repeatCount = ObjectAnimator.INFINITE }
        val alpha = ObjectAnimator.ofFloat(orb, View.ALPHA, 0.72f, 1f, 0.72f).apply { repeatCount = ObjectAnimator.INFINITE }
        pulse = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1400L
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun Int.dp(density: Float): Int = (this * density).toInt()
}
