package ai.mayra.app.background

import ai.mayra.app.action.MayraActionRuntime
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import java.security.MessageDigest
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
        val packages = preferences.getStringSet(KEY_PACKAGES, emptySet()).orEmpty().toMutableSet()
        packages += prefix
        preferences.edit()
            .putString("$prefix.mode", policy.mode.name)
            .putBoolean("$prefix.reply", policy.allowReply)
            .putBoolean("$prefix.read_aloud", policy.allowReadAloud)
            .putStringSet(KEY_PACKAGES, packages)
            .apply()
    }

    fun knownPolicies(): List<NotificationAppPolicy> = preferences
        .getStringSet(KEY_PACKAGES, emptySet())
        .orEmpty()
        .map(::policyFor)
        .sortedBy(NotificationAppPolicy::packageName)

    fun reset(packageName: String) {
        val prefix = packageName.trim()
        val packages = preferences.getStringSet(KEY_PACKAGES, emptySet()).orEmpty().toMutableSet()
        packages -= prefix
        preferences.edit()
            .remove("$prefix.mode")
            .remove("$prefix.reply")
            .remove("$prefix.read_aloud")
            .putStringSet(KEY_PACKAGES, packages)
            .apply()
    }

    private companion object {
        const val KEY_PACKAGES = "configured_packages"
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
    private val otpWordPattern = Regex("(?i)\\b(?:otp|one[- ]time password|verification code|security code)\\b")
    private val numericCodePattern = Regex("\\b\\d{4,8}\\b")
    private val sensitivePatterns = listOf(
        Regex("(?i)\\b(?:password|passcode|pin|bank|account|card|cvv|payment|transaction)\\b"),
        Regex("(?i)\\b(?:medical|diagnosis|prescription|legal notice)\\b")
    )

    fun classify(title: String, text: String, secretVisibility: Boolean): NotificationSensitivity {
        val combined = "$title $text"
        if (otpWordPattern.containsMatchIn(combined) && numericCodePattern.containsMatchIn(combined)) {
            return NotificationSensitivity.OTP
        }
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

    fun safeReplyPreview(text: String, sensitivity: NotificationSensitivity): String = when (sensitivity) {
        NotificationSensitivity.NORMAL -> text.take(160)
        NotificationSensitivity.SENSITIVE -> "Sensitive reply content hidden."
        NotificationSensitivity.OTP -> "OTP-related replies are blocked."
    }

    private fun redactSensitiveNumbers(value: String): String = value
        .replace(Regex("\\b\\d{4,16}\\b"), "••••")
}

class MayraNotificationStore(private val maxEntries: Int = 250) {
    init { require(maxEntries in 20..2_000) }
    private val records = ArrayDeque<MayraNotificationRecord>()

    @Synchronized
    fun upsert(record: MayraNotificationRecord) {
        records.removeAll { it.id == record.id }
        records.addLast(record)
        while (records.size > maxEntries) records.removeFirst()
    }

    @Synchronized fun remove(id: String) { records.removeAll { it.id == id } }
    @Synchronized fun snapshot(): List<MayraNotificationRecord> = records.toList().sortedByDescending { it.postedAt }
    @Synchronized fun clear(): Int = records.size.also { records.clear() }

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

private data class PendingReplyState(
    val handle: NotificationReplyHandle,
    val text: String,
    val createdAt: Long,
    val expiresAt: Long,
    val fingerprint: String
)

data class PendingNotificationReply(
    val token: String,
    val notificationId: String,
    val sourcePackage: String,
    val preview: String,
    val expiresAt: Long
)

enum class NotificationReplyAuditStatus { PREPARED, CONFIRMED, SENT, BLOCKED, FAILED, CANCELLED, DUPLICATE_BLOCKED }

data class NotificationReplyAuditEvent(
    val notificationId: String,
    val sourcePackage: String,
    val status: NotificationReplyAuditStatus,
    val timestamp: Long,
    val detail: String
)

sealed interface NotificationReplyResult {
    data class AwaitingConfirmation(val pending: PendingNotificationReply) : NotificationReplyResult
    data class Sent(val message: String) : NotificationReplyResult
    data class Blocked(val message: String) : NotificationReplyResult
    data class Failed(val message: String) : NotificationReplyResult
}

/**
 * Reply PendingIntents stay in memory only. Every send requires a short-lived explicit confirmation.
 * The global Mayra action kill switch always takes priority. Duplicate sends are suppressed.
 */
object MayraNotificationReplyRuntime {
    private const val TTL_MILLIS = 60_000L
    private const val HANDLE_TTL_MILLIS = 6 * 60 * 60 * 1_000L
    private const val DUPLICATE_WINDOW_MILLIS = 2 * 60 * 1_000L
    private const val MAX_AUDIT_EVENTS = 200
    private val handles = linkedMapOf<String, NotificationReplyHandle>()
    private val pending = linkedMapOf<String, PendingReplyState>()
    private val sentFingerprints = linkedMapOf<String, Long>()
    private val audit = ArrayDeque<NotificationReplyAuditEvent>()

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
        pending.entries.removeAll { it.value.handle.notificationId == notificationId }
    }

    @Synchronized
    fun prepare(
        notificationId: String,
        replyText: String,
        policy: NotificationAppPolicy,
        sensitivity: NotificationSensitivity = NotificationSensitivity.NORMAL,
        now: Long = System.currentTimeMillis()
    ): NotificationReplyResult {
        prune(now)
        val clean = replyText.trim().take(1_000)
        if (clean.isBlank()) return failed(notificationId, policy.packageName, "Reply cannot be empty.", now)
        if (policy.mode == NotificationPrivacyMode.IGNORE) {
            return blocked(notificationId, policy.packageName, "This app is ignored by notification privacy settings.", now)
        }
        if (!policy.allowReply) return blocked(notificationId, policy.packageName, "Replies are disabled for this app.", now)
        if (sensitivity == NotificationSensitivity.OTP) {
            return blocked(notificationId, policy.packageName, "Replies to OTP notifications are blocked for safety.", now)
        }
        if (MayraActionRuntime.installed && MayraActionRuntime.requireEngine().isStopped()) {
            return blocked(notificationId, policy.packageName, "Mayra actions are stopped. Resume them from Action controls first.", now)
        }
        val handle = handles[notificationId]
            ?: return failed(notificationId, policy.packageName, "This notification no longer exposes a supported reply action.", now)
        val fingerprint = replyFingerprint(notificationId, clean)
        if (sentFingerprints[fingerprint]?.let { now - it < DUPLICATE_WINDOW_MILLIS } == true) {
            record(notificationId, handle.sourcePackage, NotificationReplyAuditStatus.DUPLICATE_BLOCKED, now, "Duplicate reply suppressed.")
            return NotificationReplyResult.Blocked("An identical reply was already handed to this notification recently.")
        }
        val token = UUID.randomUUID().toString()
        val expiresAt = now + TTL_MILLIS
        pending[token] = PendingReplyState(handle, clean, now, expiresAt, fingerprint)
        record(notificationId, handle.sourcePackage, NotificationReplyAuditStatus.PREPARED, now, "Reply prepared for confirmation.")
        return NotificationReplyResult.AwaitingConfirmation(
            PendingNotificationReply(
                token = token,
                notificationId = notificationId,
                sourcePackage = handle.sourcePackage,
                preview = NotificationContentGuard.safeReplyPreview(clean, sensitivity),
                expiresAt = expiresAt
            )
        )
    }

    @Synchronized
    fun confirm(context: Context, token: String, now: Long = System.currentTimeMillis()): NotificationReplyResult {
        prune(now)
        if (MayraActionRuntime.installed && MayraActionRuntime.requireEngine().isStopped()) {
            val removed = pending.remove(token)
            val id = removed?.handle?.notificationId.orEmpty()
            val source = removed?.handle?.sourcePackage.orEmpty()
            return blocked(id, source, "Mayra actions are stopped. Reply was not sent.", now)
        }
        val state = pending.remove(token)
            ?: return NotificationReplyResult.Failed("Reply confirmation is invalid or expired.")
        if (now > state.expiresAt) {
            return failed(state.handle.notificationId, state.handle.sourcePackage, "Reply confirmation expired. Review it again.", now)
        }
        if (sentFingerprints[state.fingerprint]?.let { now - it < DUPLICATE_WINDOW_MILLIS } == true) {
            record(state.handle.notificationId, state.handle.sourcePackage, NotificationReplyAuditStatus.DUPLICATE_BLOCKED, now, "Duplicate confirmation suppressed.")
            return NotificationReplyResult.Blocked("This reply was already handed to the app.")
        }
        if (now - state.handle.createdAt > HANDLE_TTL_MILLIS) {
            handles.remove(state.handle.notificationId)
            return failed(state.handle.notificationId, state.handle.sourcePackage, "The notification reply action expired.", now)
        }
        record(state.handle.notificationId, state.handle.sourcePackage, NotificationReplyAuditStatus.CONFIRMED, now, "User confirmed reply.")
        return runCatching {
            val fillInIntent = Intent()
            val bundle = android.os.Bundle().apply {
                putCharSequence(state.handle.remoteInput.resultKey, state.text)
            }
            RemoteInput.addResultsToIntent(arrayOf(state.handle.remoteInput), fillInIntent, bundle)
            state.handle.action.actionIntent.send(context.applicationContext, 0, fillInIntent)
            sentFingerprints[state.fingerprint] = now
            record(state.handle.notificationId, state.handle.sourcePackage, NotificationReplyAuditStatus.SENT, now, "Reply action handed to source app.")
            NotificationReplyResult.Sent("Reply action handed to ${state.handle.sourcePackage}. Delivery is controlled by that app.")
        }.getOrElse { error ->
            val message = when (error) {
                is PendingIntent.CanceledException -> "The app cancelled this notification reply action."
                else -> "The notification reply could not be completed."
            }
            failed(state.handle.notificationId, state.handle.sourcePackage, message, now)
        }
    }

    @Synchronized
    fun reject(token: String, now: Long = System.currentTimeMillis()): NotificationReplyResult {
        val state = pending.remove(token) ?: return NotificationReplyResult.Failed("Reply confirmation is invalid or expired.")
        record(state.handle.notificationId, state.handle.sourcePackage, NotificationReplyAuditStatus.CANCELLED, now, "Reply cancelled by user.")
        return NotificationReplyResult.Blocked("Reply cancelled.")
    }

    @Synchronized fun auditSnapshot(): List<NotificationReplyAuditEvent> = audit.toList().reversed()
    @Synchronized fun clearAudit(): Int = audit.size.also { audit.clear() }

    @Synchronized
    private fun prune(now: Long) {
        handles.entries.removeAll { now - it.value.createdAt > HANDLE_TTL_MILLIS }
        pending.entries.removeAll { now > it.value.expiresAt || now - it.value.handle.createdAt > HANDLE_TTL_MILLIS }
        sentFingerprints.entries.removeAll { now - it.value >= DUPLICATE_WINDOW_MILLIS }
    }

    private fun replyFingerprint(notificationId: String, text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest("$notificationId\u001F$text".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun blocked(id: String, source: String, message: String, now: Long): NotificationReplyResult.Blocked {
        record(id, source, NotificationReplyAuditStatus.BLOCKED, now, message)
        return NotificationReplyResult.Blocked(message)
    }

    private fun failed(id: String, source: String, message: String, now: Long): NotificationReplyResult.Failed {
        record(id, source, NotificationReplyAuditStatus.FAILED, now, message)
        return NotificationReplyResult.Failed(message)
    }

    private fun record(id: String, source: String, status: NotificationReplyAuditStatus, now: Long, detail: String) {
        audit.addLast(NotificationReplyAuditEvent(id, source, status, now, detail.take(240)))
        while (audit.size > MAX_AUDIT_EVENTS) audit.removeFirst()
    }
}

object MayraNotificationIntelligenceRuntime {
    val store = MayraNotificationStore()
}