package ai.mayra.app.background

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Receives notification events only after the user explicitly enables Notification Access.
 * Raw notification data stays on-device in the ambient inbox; no network transmission occurs here.
 */
class MayraNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        if (sbn.packageName == packageName) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return

        AmbientEventStore(applicationContext).append(
            AmbientEvent(
                sourcePackage = sbn.packageName.orEmpty(),
                title = title.take(MAX_FIELD_LENGTH),
                text = text.take(MAX_FIELD_LENGTH),
                timestamp = sbn.postTime
            )
        )
    }

    private companion object {
        const val MAX_FIELD_LENGTH = 500
    }
}
