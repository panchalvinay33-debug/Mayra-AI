package ai.mayra.app.floating

import ai.mayra.app.MainActivity
import ai.mayra.app.presence.MayraPresenceActivity
import ai.mayra.app.ui.theme.MayraAITheme
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button as ComposeButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlin.math.abs

class FloatingMayraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MayraAITheme {
                FloatingMayraScreen(
                    hasOverlayAccess = Settings.canDrawOverlays(this),
                    isEnabled = FloatingMayraPreferences(this).enabled,
                    onGrantAccess = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    },
                    onStart = {
                        FloatingMayraPreferences(this).enabled = true
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, FloatingMayraService::class.java).setAction(FloatingMayraService.ACTION_START)
                        )
                    },
                    onStop = {
                        FloatingMayraPreferences(this).enabled = false
                        startService(Intent(this, FloatingMayraService::class.java).setAction(FloatingMayraService.ACTION_STOP))
                    },
                    onClose = ::finish
                )
            }
        }
    }
}

@Composable
private fun FloatingMayraScreen(
    hasOverlayAccess: Boolean,
    isEnabled: Boolean,
    onGrantAccess: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit
) {
    var notice by remember { mutableStateOf<String?>(null) }
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Floating Mayra", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Keep Mayra available as a draggable companion over other apps. Tap her face to open a compact action panel.")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Companion readiness", fontWeight = FontWeight.SemiBold)
                    Text(if (hasOverlayAccess) "Overlay access allowed" else "Overlay access required")
                    Text(if (isEnabled) "Floating preference is on" else "Floating preference is off")
                    Text(
                        "The bubble can be dragged, docks to a screen edge and remembers its position. Android shows a persistent notification while it is active.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (!hasOverlayAccess) {
                ComposeButton(onClick = onGrantAccess, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow display over other apps")
                }
            } else {
                ComposeButton(
                    onClick = {
                        onStart()
                        notice = "Floating Mayra started. Tap the bubble for Talk, Type, Home and Stop."
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isEnabled) "Restart floating Mayra" else "Start floating Mayra") }
            }
            OutlinedButton(
                onClick = {
                    onStop()
                    notice = "Floating Mayra stopped."
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Stop floating Mayra") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

class FloatingMayraService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var preferences: FloatingMayraPreferences
    private var bubble: MayraBubbleView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panel: LinearLayout? = null
    private var panelParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        preferences = FloatingMayraPreferences(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                preferences.enabled = false
                stopFloating()
                return START_NOT_STICKY
            }
            ACTION_COLLAPSE -> {
                hidePanel()
                return START_STICKY
            }
        }
        if (!Settings.canDrawOverlays(this)) {
            preferences.enabled = false
            stopSelf()
            return START_NOT_STICKY
        }
        preferences.enabled = true
        startForeground(NOTIFICATION_ID, buildNotification())
        showBubble()
        return START_STICKY
    }

    override fun onDestroy() {
        removeAllOverlays()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBubble() {
        if (bubble != null) return
        val view = MayraBubbleView(this)
        val layoutParams = WindowManager.LayoutParams(
            dp(BUBBLE_SIZE_DP),
            dp(BUBBLE_SIZE_DP),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = preferences.x
            y = preferences.y
        }
        bubble = view
        bubbleParams = layoutParams
        attachDragAndTap(view, layoutParams)
        runCatching { windowManager.addView(view, layoutParams) }
            .onFailure { stopFloating() }
    }

    private fun attachDragAndTap(view: View, layoutParams: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = layoutParams.x
                    startY = layoutParams.y
                    touchX = event.rawX
                    touchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    moved = moved || abs(dx) > dp(4) || abs(dy) > dp(4)
                    layoutParams.x = (startX + dx).coerceAtLeast(0)
                    layoutParams.y = (startY + dy).coerceAtLeast(0)
                    hidePanel()
                    runCatching { windowManager.updateViewLayout(view, layoutParams) }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (moved) dockAndSave(layoutParams) else togglePanel()
                    true
                }
                else -> false
            }
        }
    }

    private fun dockAndSave(layoutParams: WindowManager.LayoutParams) {
        val screenWidth = resources.displayMetrics.widthPixels
        val bubbleWidth = dp(BUBBLE_SIZE_DP)
        val centerX = layoutParams.x + bubbleWidth / 2
        layoutParams.x = if (centerX < screenWidth / 2) dp(8) else (screenWidth - bubbleWidth - dp(8)).coerceAtLeast(0)
        layoutParams.y = layoutParams.y.coerceIn(dp(48), (resources.displayMetrics.heightPixels - bubbleWidth - dp(80)).coerceAtLeast(dp(48)))
        preferences.x = layoutParams.x
        preferences.y = layoutParams.y
        bubble?.let { runCatching { windowManager.updateViewLayout(it, layoutParams) } }
    }

    private fun togglePanel() {
        if (panel == null) showPanel() else hidePanel()
    }

    private fun showPanel() {
        val currentBubbleParams = bubbleParams ?: return
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBackground(Color.rgb(248, 245, 255), 22f)
            elevation = dp(10).toFloat()
        }
        root.addView(TextView(this).apply {
            text = "Mayra"
            textSize = 18f
            setTextColor(Color.rgb(38, 28, 64))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "What should I do?"
            textSize = 13f
            setTextColor(Color.rgb(90, 80, 112))
            setPadding(0, dp(2), 0, dp(10))
        })
        root.addView(panelButton("🎤  Talk to Mayra") { openChat(startVoice = true) })
        root.addView(panelButton("⌨  Type to Mayra") { openChat(startVoice = false) })
        root.addView(panelButton("⌂  Open Living Home") { openMayraHome() })
        root.addView(panelButton("—  Minimize") { hidePanel() })
        root.addView(panelButton("✕  Stop floating Mayra") {
            preferences.enabled = false
            stopFloating()
        })

        val width = dp(244)
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val bubbleOnLeft = currentBubbleParams.x < resources.displayMetrics.widthPixels / 2
            x = if (bubbleOnLeft) currentBubbleParams.x + dp(BUBBLE_SIZE_DP + 8)
            else (currentBubbleParams.x - width - dp(8)).coerceAtLeast(dp(8))
            y = currentBubbleParams.y.coerceAtLeast(dp(48))
        }
        panel = root
        panelParams = params
        runCatching { windowManager.addView(root, params) }
            .onFailure { panel = null; panelParams = null }
    }

    private fun panelButton(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 14f
        isAllCaps = false
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setTextColor(Color.rgb(53, 40, 83))
        background = roundedBackground(Color.TRANSPARENT, 14f)
        setPadding(dp(10), dp(8), dp(10), dp(8))
        setOnClickListener { action() }
    }

    private fun hidePanel() {
        panel?.let { runCatching { windowManager.removeView(it) } }
        panel = null
        panelParams = null
    }

    private fun openChat(startVoice: Boolean) {
        hidePanel()
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(EXTRA_START_VOICE, startVoice)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun openMayraHome() {
        hidePanel()
        startActivity(
            Intent(this, MayraPresenceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun stopFloating() {
        removeAllOverlays()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeAllOverlays() {
        hidePanel()
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
        bubbleParams = null
    }

    private fun buildNotification(): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MayraPresenceActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val collapseIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingMayraService::class.java).setAction(ACTION_COLLAPSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, FloatingMayraService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mayra is available")
            .setContentText("Tap the floating Mayra for Talk, Type and Home.")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Minimize", collapseIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setCategory(android.app.Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Floating Mayra", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the user-enabled floating Mayra companion available."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun roundedBackground(color: Int, radiusDp: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp.toInt()).toFloat()
        if (color == Color.TRANSPARENT) setStroke(dp(1), Color.argb(32, 82, 63, 125))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_START = "ai.mayra.app.floating.START"
        const val ACTION_STOP = "ai.mayra.app.floating.STOP"
        const val ACTION_COLLAPSE = "ai.mayra.app.floating.COLLAPSE"
        const val EXTRA_START_VOICE = "ai.mayra.app.extra.START_VOICE"
        private const val CHANNEL_ID = "mayra_floating_companion"
        private const val NOTIFICATION_ID = 7_410
        private const val BUBBLE_SIZE_DP = 68
    }
}

internal class FloatingMayraPreferences(context: Context) {
    private val values = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = values.getBoolean(KEY_ENABLED, false)
        set(value) { values.edit().putBoolean(KEY_ENABLED, value).apply() }
    var x: Int
        get() = values.getInt(KEY_X, dp(context, 8))
        set(value) { values.edit().putInt(KEY_X, value).apply() }
    var y: Int
        get() = values.getInt(KEY_Y, dp(context, 180))
        set(value) { values.edit().putInt(KEY_Y, value).apply() }

    private companion object {
        const val PREFS = "mayra_floating_companion"
        const val KEY_ENABLED = "enabled"
        const val KEY_X = "x"
        const val KEY_Y = "y"
        fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    }
}

private class MayraBubbleView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val radius = width.coerceAtMost(height) * 0.46f
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(54, 101, 84, 192)
        canvas.drawCircle(cx, cy, radius, paint)
        paint.color = Color.rgb(42, 23, 21)
        canvas.drawCircle(cx, cy - radius * 0.08f, radius * 0.78f, paint)
        paint.color = Color.rgb(243, 183, 143)
        canvas.drawCircle(cx, cy, radius * 0.61f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx - radius * 0.22f, cy - radius * 0.10f, radius * 0.16f, paint)
        canvas.drawCircle(cx + radius * 0.22f, cy - radius * 0.10f, radius * 0.16f, paint)
        paint.color = Color.rgb(55, 35, 31)
        canvas.drawCircle(cx - radius * 0.22f, cy - radius * 0.08f, radius * 0.09f, paint)
        canvas.drawCircle(cx + radius * 0.22f, cy - radius * 0.08f, radius * 0.09f, paint)
        paint.color = Color.rgb(183, 58, 78)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = radius * 0.08f
        canvas.drawArc(cx - radius * 0.18f, cy + radius * 0.02f, cx + radius * 0.18f, cy + radius * 0.32f, 18f, 144f, false, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(207, 23, 56)
        canvas.drawCircle(cx, cy + radius * 0.66f, radius * 0.18f, paint)
    }
}
