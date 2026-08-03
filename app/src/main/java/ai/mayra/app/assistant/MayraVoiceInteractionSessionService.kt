package ai.mayra.app.assistant

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
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

class MayraVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = MayraVoiceInteractionSession(this)
}

/**
 * Lightweight animated assistant surface. The orb is intentionally framework-native so it can be
 * shown by Android's voice-interaction layer over the current app without a general overlay
 * permission. Conversational audio/state will be wired into this session in the next voice batch.
 */
class MayraVoiceInteractionSession(context: Context) : VoiceInteractionSession(context) {
    private var pulse: AnimatorSet? = null

    override fun onCreate() {
        super.onCreate()
        setKeepAwake(true)
    }

    override fun onCreateContentView(): View {
        val density = context.resources.displayMetrics.density
        val root = FrameLayout(context).apply {
            setPadding(24.dp(density), 24.dp(density), 24.dp(density), 48.dp(density))
        }

        val orb = View(context).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(76, 90, 220))
                setStroke(3.dp(density), Color.argb(180, 220, 225, 255))
            }
            contentDescription = "Mayra assistant"
        }
        val orbSize = 104.dp(density)
        root.addView(
            orb,
            FrameLayout.LayoutParams(orbSize, orbSize).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 58.dp(density)
            }
        )

        val label = TextView(context).apply {
            text = "Mayra"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        root.addView(
            label,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                bottomMargin = 20.dp(density)
            }
        )

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
        return root
    }

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        setKeepAwake(true)
    }

    override fun onHide() {
        setKeepAwake(false)
        super.onHide()
    }

    override fun onDestroy() {
        pulse?.cancel()
        pulse = null
        super.onDestroy()
    }

    private fun Int.dp(density: Float): Int = (this * density).toInt()
}
