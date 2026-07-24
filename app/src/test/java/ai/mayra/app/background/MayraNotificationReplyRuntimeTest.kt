package ai.mayra.app.background

import ai.mayra.app.TestMayraApplication
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestMayraApplication::class)
class MayraNotificationReplyRuntimeTest {
    @Test
    fun `privacy policy persists and can be reset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NotificationPrivacyStore(context)
        val packageName = "chat.example.policy"

        store.save(
            NotificationAppPolicy(
                packageName = packageName,
                mode = NotificationPrivacyMode.REDACT_CONTENT,
                allowReply = false,
                allowReadAloud = true
            )
        )

        assertEquals(NotificationPrivacyMode.REDACT_CONTENT, store.policyFor(packageName).mode)
        assertTrue(store.knownPolicies().any { it.packageName == packageName })

        store.reset(packageName)

        assertEquals(NotificationAppPolicy(packageName), store.policyFor(packageName))
        assertTrue(store.knownPolicies().none { it.packageName == packageName })
    }

    @Test
    fun `reply confirmation expires after one minute`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = "expiry-${System.nanoTime()}"
        registerReplyableNotification(context, id, now = 1_000L)
        val prepared = MayraNotificationReplyRuntime.prepare(
            notificationId = id,
            replyText = "Hello",
            policy = NotificationAppPolicy("chat.example"),
            now = 1_000L
        ) as NotificationReplyResult.AwaitingConfirmation

        val result = MayraNotificationReplyRuntime.confirm(
            context = context,
            token = prepared.pending.token,
            now = 61_001L
        )

        assertTrue(result is NotificationReplyResult.Failed)
        assertTrue(result.message.contains("expired", ignoreCase = true))
    }

    @Test
    fun `identical reply is blocked inside duplicate window`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = "duplicate-${System.nanoTime()}"
        registerReplyableNotification(context, id, now = 10_000L)
        val first = MayraNotificationReplyRuntime.prepare(
            notificationId = id,
            replyText = "On my way",
            policy = NotificationAppPolicy("chat.example"),
            now = 10_000L
        ) as NotificationReplyResult.AwaitingConfirmation

        val sent = MayraNotificationReplyRuntime.confirm(context, first.pending.token, now = 10_500L)
        val duplicate = MayraNotificationReplyRuntime.prepare(
            notificationId = id,
            replyText = "On my way",
            policy = NotificationAppPolicy("chat.example"),
            now = 11_000L
        )

        assertTrue(sent is NotificationReplyResult.Sent)
        assertTrue(duplicate is NotificationReplyResult.Blocked)
        assertTrue(duplicate.message.contains("already", ignoreCase = true))
        assertTrue(MayraNotificationReplyRuntime.auditSnapshot().any {
            it.notificationId == id && it.status == NotificationReplyAuditStatus.DUPLICATE_BLOCKED
        })
    }

    private fun registerReplyableNotification(context: Context, id: String, now: Long) {
        val remoteInput = RemoteInput.Builder("reply_text").setAllowFreeFormInput(true).build()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            Intent("ai.mayra.test.NOTIFICATION_REPLY").setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val action = Notification.Action.Builder(null, "Reply", pendingIntent)
            .addRemoteInput(remoteInput)
            .build()
        val notification = Notification.Builder(context, "test")
            .setContentTitle("Chat")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .addAction(action)
            .build()

        assertTrue(MayraNotificationReplyRuntime.register(id, "chat.example", notification, now))
    }
}