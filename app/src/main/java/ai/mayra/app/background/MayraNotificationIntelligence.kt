package ai.mayra.app.background

import ai.mayra.app.action.MayraActionRuntime
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import java.util.ArrayDeque
import java.util.UUID

/** User-facing privacy choices for a source app. */
enum class NotificationPrivacyMode { FULL, REDACT_CONTENT, IGNORE }

data class NotificationAppPolicy(
    val packageName: String,
    val mode: NotificationPrivacyMode = NotificationPrivacyMode.FULL,
    val allowReply: Boolean = true,
    val allowReadAloud: Boolean = false
)

class NotificationPrivacyStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "mayra_notification_privacy",
        Context.MODE_PRIVATE
    )

    fun policyFor(packageName: String): NotificationAppPolicy {
        val prefix = packageName.trim()
        val mode = runCatching {
            NotificationPrivacyMode.valueOf(
                preferences.getString("$prefix.mode", NotificationPrivacyMode.FULL.name).orEmpty()
            )
        }.getOrDefault(NotificationPrivacyMode.FULL)
        return NotificationAppPolicy(
            packageName = prefix,
            mode = mode,
            allowReply = preferences.getBoolean("$prefix.reply", true),
            allowReadAloud = preferences.getBoolean("$prefix.read_aloud", false)
        )
    }

    fun save(policy: NotificationAppPolicy) {
        val prefix = policy.packageName.trim()
        require(prefix.isNotBlank())
        preferences.edit()
            .putString("$prefix.mode", policy.mode.name)
            .putBoolean("$prefix.reply", policy.allowReply)
            .putBoolean("$prefix.read_aloud", policy.allowReadAloud)
            .apply()
    }
}

enum class NotificationSensitivity { NORMAL, SENSITIVE, OTP }

data class MayraNotificationRecord(
    val id: String,
    val sourcePackage: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val postedAt: Long,
    val conversationKey: String? = null,
    val groupKey: String? = null,
    val sensitivity: NotificationSensitivity = NotificationSensitivity.NORMAL,
    val replyAvailable: Boolean = false,
    val clearable: Boolean = false,
    val ongoing: Boolean = false
)

object NotificationContentGuard {
    private val otpPatterns = listOf(
        Regex("(?i)\\b(?:otp|one[- ]time password|verification code|security code)\\b"),
        Regex("\\b\\d{4,8}\\b")
    )
    private val sensitivePatterns = listOf(
        Regex("(?i)\\b(?:password|passcode|pin|bank|account|card|cvv|payment|transaction)\\b"),
        Regex("(?i)\\b(?:medical|diagnosis|prescription|legal notice)\\b")
    )

    fun classify(title: String, text: String, secretVisibility: Boolean): NotificationSensitivity {
        val combined = "$title $text"
        val otpWord = otpPatterns.first().containsMatchIn(combined)
        val code = otpPatterns.last().find(combined)?.value
        if (otpWord && code != null) return NotificationSensitivity.OTP
        if (secretVisibility || sensitivePatterns.any { it.containsMatchIn(combined) }) {
            return NotificationSensitivity.SENSITIVE
        }
        return NotificationSensitivity.NORMAL
    }

    fun sanitize(
        title: String,
        text: String,
        sensitivity: NotificationSensitivity,
        mode: NotificationPrivacyMode
    ): Pair<String, String> = when {
        mode == NotificationPrivacyMode.REDACT_CONTENT ->
            title.take(120) to "Content hidden by notification privacy settings."
        sensitivity == NotificationSensitivity.OTP ->
            title.take(120) to "OTP or verification code hidden."
        sensitivity == NotificationSensitivity.SENSITIVE ->
            title.take(120) to redactSensitiveNumbers(text).take(500)
        else -> title.take(200) to text.take(1_000)
    }

    private fun redactSensitiveNumbers(value: String): String = value
        .replace(Regex("\\b\\d{4,16}\\b"), "••••")
}

class MayraNotificationStore(
    private val maxEntries: Int = 250
) {
    init { require(maxEntries in 20..2_000) }
    private val records = ArrayDeque<MayraNotificationRecord>()

    @Synchronized
    fun upsert(record: MayraNotificationRecord) {
        records.removeAll { it.id == record.id }
        records.addLast(record)
        while (records.size > maxEntries) records.removeFirst()
    }

    @Synchronized
    fun remove(id: String) {
        records.removeAll { it.id == id }
    }

    @Synchronized
    fun snapshot(): List<MayraNotificationRecord> = records.toList().sortedByDescending { it.postedAt }

    @Synchronized
    fun clear(): Int = records.size.also { records.clear() }

    fun summary(limit: Int = 8): NotificationBrief {
        val items = snapshot()
        val grouped = items.groupBy { it.conversationKey?.takeIf(String::isNotBlank) ?: it.sourcePackage }
        val lines = grouped.entries
            .sortedByDescending { entry -> entry.value.maxOfOrNull(MayraNotificationRecord::postedAt) ?: 0L }
            .take(limit.coerceIn(1, 20))
            .map { (_, notifications) ->
                val latest = notifications.maxBy(MayraNotificationRecord::postedAt)
                val source = latest.conversationKey?.takeIf(String::isNotBlank) ?: latest.appLabel
                if (notifications.size == 1) "$source: ${latest.text}" else "$source: ${notifications.size} notifications · ${latest.text}"
            }
        return NotificationBrief(
            total = items.size,
            sensitiveCount = items.count { it.sensitivity != NotificationSensitivity.NORMAL },
            replyableCount = items.count(MayraNotificationRecord::replyAvailable),
            lines = lines
        )
    }
}

data class NotificationBrief(
    val total: Int,
    val sensitiveCount: Int,
    val replyableCount: Int,
    val lines: List<String>
) {
    fun spokenText(): String = when {
        total == 0 -> "There are no captured notifications."
        lines.isEmpty() -> "$total notifications are available."
        else -> buildString {
            append("You have $total notifications. ")
            append(lines.joinToString(" "))
            if (sensitiveCount > 0) append(" $sensitiveCount sensitive notifications were protected.")
        }
    }
}

private data class NotificationReplyHandle(
    val notificationId: String,
    val action: Notification.Action,
    val remoteInput: RemoteInput,
    val sourcePackage: String,
    val createdAt: Long
)

data class PendingNotificationReply(
    val token: String,
    val notificationId: String,
    val sourcePackage: String,
    val preview: String,
    val expiresAt: Long
)

sealed interface NotificationReplyResult {
    data class AwaitingConfirmation(val pending: PendingNotificationReply) : NotificationReplyResult
    data class Sent(val message: String) : NotificationReplyResult
    data class Blocked(val message: String) : NotificationReplyResult
    data class Failed(val message: String) : NotificationReplyResult
}

/**
 * Reply PendingIntents stay in memory only. Every send requires a short-lived explicit confirmation.
 * The global Mayra action kill switch always takes priority.
 */
object MayraNotificationReplyRuntime {
    private const val TTL_MILLIS = 60_000L
    private const val HANDLE_TTL_MILLIS = 6 * 60 * 60 * 1_000L
    private val handles = linkedMapOf<String, NotificationReplyHandle>()
    private val pending = linkedMapOf<String, Pair<NotificationReplyHandle, String>>()

    @Synchronized
    fun register(
        notificationId: String,
        sourcePackage: String,
        notification: Notification,
        now: Long = System.currentTimeMillis()
    ): Boolean {
        val candidate = notification.actions.orEmpty().asSequence()
            .mapNotNull { action ->
                val input = action.remoteInputs?.firstOrNull { it.allowFreeFormInput } ?: return@mapNotNull null
                NotificationReplyHandle(notificationId, action, input, sourcePackage, now)
            }
            .firstOrNull() ?: return false
        handles[notificationId] = candidate
        prune(now)
        return true
    }

    @Synchronized
    fun remove(notificationId: String) {
        handles.remove(notificationId)
        pending.entries.removeAll { it.value.first.notificationId == notificationId }
    }

    @Synchronized
    fun prepare(
        notificationId: String,
        replyText: String,
        policy: NotificationAppPolicy,
        now: Long = System.currentTimeMillis()
    ): NotificationReplyResult {
        val clean = replyText.trim().take(1_000)
        if (clean.isBlank()) return NotificationReplyResult.Failed("Reply cannot be empty.")
        if (!policy.allowReply) return NotificationReplyResult.Blocked("Replies are disabled for this app.")
        if (MayraActionRuntime.installed && MayraActionRuntime.requireEngine().isStopped()) {
            return NotificationReplyResult.Blocked("Mayra actions are stopped. Resume them from Action controls first.")
        }
        val handle = handles[notificationId]
            ?: return NotificationReplyResult.Failed("This notification no longer exposes a supported reply action.")
        val token = UUID.randomUUID().toString()
        pending[token] = handle to clean
        return NotificationReplyResult.AwaitingConfirmation(
            PendingNotificationReply(
                token = token,
                notificationId = notificationId,
                sourcePackage = handle.sourcePackage,
                preview = clean,
                expiresAt = now + TTL_MILLIS
            )
        )
    }

    @Synchronized
    fun confirm(
        context: Context,
        token: String,
        now: Long = System.currentTimeMillis()
    ): NotificationReplyResult {
        if (MayraActionRuntime.installed && MayraActionRuntime.requireEngine().isStopped()) {
            pending.remove(token)
            return NotificationReplyResult.Blocked("Mayra actions are stopped. Reply was not sent.")
        }
        val pair = pending.remove(token)
            ?: return NotificationReplyResult.Failed("Reply confirmation is invalid or expired.")
        val (handle, text) = pair
        if (now - handle.createdAt > HANDLE_TTL_MILLIS) {
            handles.remove(handle.notificationId)
            return NotificationReplyResult.Failed("The notification reply action expired.")
        }
        return runCatching {
            val fillInIntent = Intent()
            val bundle = android.os.Bundle().apply {
                putCharSequence(handle.remoteInput.resultKey, text)
            }
            RemoteInput.addResultsToIntent(arrayOf(handle.remoteInput), fillInIntent, bundle)
            handle.action.actionIntent.send(context.applicationContext, 0, fillInIntent)
            NotificationReplyResult.Sent("Reply handed to ${handle.sourcePackage}.")
        }.getOrElse { error ->
            val message = when (error) {
                is PendingIntent.CanceledException -> "The app cancelled this notification reply action."
                else -> "The notification reply could not be completed."
            }
            NotificationReplyResult.Failed(message)
        }
    }

    @Synchronized
    fun reject(token: String): NotificationReplyResult = if (pending.remove(token) != null) {
        NotificationReplyResult.Blocked("Reply cancelled.")
    } else {
        NotificationReplyResult.Failed("Reply confirmation is invalid or expired.")
    }

    @Synchronized
    private fun prune(now: Long) {
        handles.entries.removeAll { now - it.value.createdAt > HANDLE_TTL_MILLIS }
        pending.entries.removeAll { now - it.value.first.createdAt > HANDLE_TTL_MILLIS }
    }
}

object MayraNotificationIntelligenceRuntime {
    val store = MayraNotificationStore()
}
