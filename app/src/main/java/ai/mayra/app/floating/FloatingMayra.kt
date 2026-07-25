package ai.mayra.app.floating

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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
                    onGrantAccess = {
                        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                    },
                    onStart = {
                        ContextCompat.startForegroundService(
                            this,
                            Intent(this, FloatingMayraService::class.java).setAction(FloatingMayraService.ACTION_START)
                        )
                    },
                    onStop = {
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
            Text("Keep Mayra as a draggable assistive ball over other apps. Tap the ball to open the full Living Home.")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Overlay access", fontWeight = FontWeight.SemiBold)
                    Text(if (hasOverlayAccess) "Allowed" else "Not allowed")
                    Text(
                        "Android shows a persistent notification while the floating companion is active. Mayra does not read screen content in this V1 mode.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (!hasOverlayAccess) {
                Button(onClick = onGrantAccess, modifier = Modifier.fillMaxWidth()) { Text("Allow display over other apps") }
            } else {
                Button(
                    onClick = { onStart(); notice = "Floating Mayra started. You can drag her to any screen edge." },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Start floating Mayra") }
            }
            OutlinedButton(
                onClick = { onStop(); notice = "Floating Mayra stopped." },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Stop floating Mayra") }
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

class FloatingMayraService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: MayraBubbleView? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopFloating()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        if (Settings.canDrawOverlays(this)) showBubble()
        return START_STICKY
    }

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBubble() {
        if (bubble != null) return
        val view = MayraBubbleView(this)
        val layoutParams = WindowManager.LayoutParams(
            dp(68),
            dp(68),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(180)
        }
        bubble = view
        attachDragAndTap(view, layoutParams)
        windowManager.addView(view, layoutParams)
    }

    private fun attachDragAndTap(view: View, layoutParams: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        var moved = false
        view.setOnTouchListener { _, event ->
            when (event.action) {
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
                    layoutParams.x = startX + dx
                    layoutParams.y = startY + dy
                    runCatching { windowManager.updateViewLayout(view, layoutParams) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openMayraHome()
                    true
                }
                else -> false
            }
        }
    }

    private fun openMayraHome() {
        startActivity(
            Intent(this, MayraPresenceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }

    private fun stopFloating() {
        removeBubble()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeBubble() {
        bubble?.let { runCatching { windowManager.removeView(it) } }
        bubble = null
    }

    private fun buildNotification(): android.app.Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MayraPresenceActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingMayraService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return android.app.Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mayra is floating")
            .setContentText("Tap the Mayra ball from any app, or stop it here.")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Floating Mayra", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the user-enabled floating Mayra companion active."
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val ACTION_START = "ai.mayra.app.floating.START"
        const val ACTION_STOP = "ai.mayra.app.floating.STOP"
        private const val CHANNEL_ID = "mayra_floating_companion"
        private const val NOTIFICATION_ID = 7_410
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
        paint.color = Color.argb(44, 101, 84, 192)
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