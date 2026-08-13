package ai.mayra.app.context

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import ai.mayra.app.background.MayraNotificationListener
import java.time.LocalDateTime

/**
 * Single lightweight J6 read boundary for normalized context.
 *
 * The repository does not initialize any AI/model/provider runtime. Each source keeps its own
 * permission/failure semantics and raw sensitive content is reduced before entering this bundle.
 */
data class MayraContextBundle(
    val capturedAt: LocalDateTime,
    val device: MayraContextSnapshot,
    val calendar: CalendarContextSnapshot,
    val reminders: ReminderContextSnapshot,
    val notifications: NotificationContextSnapshot,
    val contacts: ContactsContextSnapshot,
    val session: SessionContextSnapshot,
    val knowledge: KnowledgeContextSnapshot = KnowledgeContextSnapshot(capturedAt)
)

class MayraContextRepository(private val context: Context) {
    private val appContext = context.applicationContext

    fun snapshot(now: LocalDateTime = LocalDateTime.now()): MayraContextBundle {
        val notificationAccess = isNotificationAccessGranted(appContext)
        return MayraContextBundle(
            capturedAt = now,
            device = collectMayraContext(appContext, now),
            calendar = collectCalendarContext(appContext, now),
            reminders = collectReminderContext(appContext, now),
            notifications = NotificationContextStore(appContext).read(notificationAccess, now),
            contacts = collectContactsContext(appContext, now),
            session = MayraSessionContextStore(appContext).read(now),
            knowledge = collectKnowledgeContext(appContext, now)
        )
    }

    private fun isNotificationAccessGranted(context: Context): Boolean = runCatching {
        val expected = ComponentName(context, MayraNotificationListener::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS_SETTING
        ).orEmpty()
        enabled.split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
    }.getOrDefault(false)

    private companion object {
        const val ENABLED_NOTIFICATION_LISTENERS_SETTING = "enabled_notification_listeners"
    }
}
