package ai.mayra.app.platform.device

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import ai.mayra.app.core.device.DeviceNotificationGateway
import java.util.concurrent.atomic.AtomicInteger

class AndroidNotificationGateway(
    context: Context
) : DeviceNotificationGateway {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val nextId = AtomicInteger(1)

    override fun show(channelId: String, title: String, message: String): Boolean {
        if (channelId.isBlank() || title.isBlank() || message.isBlank()) return false

        return runCatching {
            ensureChannel(channelId)
            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(appContext.applicationInfo.icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            notificationManager.notify(nextId.getAndIncrement(), notification)
            true
        }.getOrDefault(false)
    }

    private fun ensureChannel(channelId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (notificationManager.getNotificationChannel(channelId) != null) return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                channelId,
                DEFAULT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = DEFAULT_CHANNEL_DESCRIPTION
            }
        )
    }

    private companion object {
        const val DEFAULT_CHANNEL_NAME = "Mayra notifications"
        const val DEFAULT_CHANNEL_DESCRIPTION = "General notifications from Mayra"
    }
}
