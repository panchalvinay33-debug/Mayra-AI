package ai.mayra.app.context

import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

/** Normalized notification data. Raw Android objects never enter the context engine. */
data class ContextNotification(
    val id: String = UUID.randomUUID().toString(),
    val sourcePackage: String,
    val appLabel: String = sourcePackage,
    val title: String,
    val text: String,
    val postedAt: Long,
    val conversationKey: String? = null,
    val categoryHint: String? = null,
    val ongoing: Boolean = false,
    val silent: Boolean = false,
    val clearable: Boolean = true,
    val sensitiveHint: Boolean = false,
    val groupKey: String? = null
) {
    init {
        require(sourcePackage.isNotBlank())
        require(postedAt >= 0)
        require(title.length <= 1_000)
        require(text.length <= 4_000)
    }
}

enum class NotificationCategory {
    MESSAGE, CALL, CALENDAR, REMINDER, PAYMENT, DELIVERY, TRAVEL, SECURITY,
    HEALTH, SOCIAL, PROMOTION, SYSTEM, MEDIA, OTHER
}

enum class NotificationSensitivity { PUBLIC, PERSONAL, SENSITIVE, HIGHLY_SENSITIVE }
enum class AttentionAction { IGNORE, STORE_ONLY, SUMMARIZE, DEFER, ASK, INTERRUPT }

data class AppNotificationPolicy(
    val packageName: String,
    val enabled: Boolean = true,
    val allowInterruptions: Boolean = true,
    val allowOnLockScreen: Boolean = false,
    val storeHistory: Boolean = true,
    val forceSensitivity: NotificationSensitivity? = null,
    val mutedCategories: Set<NotificationCategory> = emptySet(),
    val priorityBoost: Double = 0.0
) {
    init { require(priorityBoost in -0.5..0.5) }
}

data class AttentionContext(
    val now: Long = System.currentTimeMillis(),
    val deviceLocked: Boolean = false,
    val quietHours: Boolean = false,
    val userBusy: Boolean = false,
    val activeCall: Boolean = false,
    val driving: Boolean = false,
    val currentAppPackage: String? = null,
    val recentInterruptions: Int = 0,
    val pendingCriticalCount: Int = 0
) {
    init {
        require(recentInterruptions >= 0)
        require(pendingCriticalCount >= 0)
    }
}

data class NotificationInsight(
    val notificationId: String,
    val category: NotificationCategory,
    val sensitivity: NotificationSensitivity,
    val urgency: Double,
    val importance: Double,
    val attentionScore: Double,
    val action: AttentionAction,
    val maskedTitle: String,
    val maskedText: String,
    val summary: String,
    val fingerprint: String,
    val duplicateOf: String? = null,
    val reasons: List<String> = emptyList(),
    val expiresAt: Long
) {
    init {
        require(urgency in 0.0..1.0)
        require(importance in 0.0..1.0)
        require(attentionScore in 0.0..1.0)
    }
}

class NotificationPrivacyMasker {
    fun mask(
        notification: ContextNotification,
        sensitivity: NotificationSensitivity,
        deviceLocked: Boolean,
        allowOnLockScreen: Boolean
    ): Pair<String, String> {
        if (!deviceLocked || allowOnLockScreen || sensitivity == NotificationSensitivity.PUBLIC) {
            return notification.title to redactSecrets(notification.text)
        }
        return when (sensitivity) {
            NotificationSensitivity.PERSONAL -> notification.appLabel to "Naya personal notification"
            NotificationSensitivity.SENSITIVE -> notification.appLabel to "Sensitive notification hidden"
            NotificationSensitivity.HIGHLY_SENSITIVE -> "Mayra" to "Private content hidden"
            NotificationSensitivity.PUBLIC -> notification.title to redactSecrets(notification.text)
        }
    }

    private fun redactSecrets(text: String): String {
        var value = text
        value = value.replace(Regex("(?i)(otp|verification code|passcode|password)\\s*[:=-]?\\s*\\d{4,8}"), "$1 [REDACTED]")
        value = value.replace(Regex("(?<!\\d)\\d{12,19}(?!\\d)"), "[REDACTED_NUMBER]")
        value = value.replace(Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"), "[REDACTED_EMAIL]")
        return value.take(1_000)
    }
}

class NotificationAttentionEngine(
    private val duplicateWindowMillis: Long = 10 * 60 * 1000L,
    private val maxRecent: Int = 500,
    private val masker: NotificationPrivacyMasker = NotificationPrivacyMasker(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private data class Seen(val id: String, val fingerprint: String, val timestamp: Long)
    private val recent = ArrayDeque<Seen>()

    @Synchronized
    fun analyze(
        notification: ContextNotification,
        context: AttentionContext,
        policy: AppNotificationPolicy = AppNotificationPolicy(notification.sourcePackage)
    ): NotificationInsight {
        prune(context.now)
        val category = classify(notification)
        val sensitivity = policy.forceSensitivity ?: sensitivity(notification, category)
        val fingerprint = fingerprint(notification, category)
        val duplicate = recent.lastOrNull { it.fingerprint == fingerprint && context.now - it.timestamp <= duplicateWindowMillis }
        val urgency = urgency(notification, category)
        val importance = importance(notification, category, policy)
        val fatiguePenalty = min(0.35, context.recentInterruptions * 0.04)
        val busyPenalty = if (context.userBusy || context.activeCall) 0.15 else 0.0
        val rawScore = (urgency * 0.58 + importance * 0.42 - fatiguePenalty - busyPenalty).coerceIn(0.0, 1.0)
        val reasons = mutableListOf<String>()
        if (duplicate != null) reasons += "duplicate"
        if (context.quietHours) reasons += "quiet_hours"
        if (context.deviceLocked) reasons += "device_locked"
        if (context.userBusy) reasons += "user_busy"
        if (category in setOf(NotificationCategory.SECURITY, NotificationCategory.CALL)) reasons += "time_sensitive_category"
        if (!policy.enabled) reasons += "app_policy_disabled"
        if (category in policy.mutedCategories) reasons += "category_muted"

        val action = decide(
            score = rawScore,
            urgency = urgency,
            sensitivity = sensitivity,
            category = category,
            duplicate = duplicate != null,
            context = context,
            policy = policy
        )
        val masked = masker.mask(notification, sensitivity, context.deviceLocked, policy.allowOnLockScreen)
        val insight = NotificationInsight(
            notificationId = notification.id,
            category = category,
            sensitivity = sensitivity,
            urgency = urgency,
            importance = importance,
            attentionScore = rawScore,
            action = action,
            maskedTitle = masked.first,
            maskedText = masked.second,
            summary = summarize(notification, category, masked.second),
            fingerprint = fingerprint,
            duplicateOf = duplicate?.id,
            reasons = reasons,
            expiresAt = context.now + retention(category, sensitivity)
        )
        recent += Seen(notification.id, fingerprint, notification.postedAt)
        while (recent.size > maxRecent) recent.removeFirst()
        return insight
    }

    @Synchronized
    fun clearDuplicates() = recent.clear()

    private fun decide(
        score: Double,
        urgency: Double,
        sensitivity: NotificationSensitivity,
        category: NotificationCategory,
        duplicate: Boolean,
        context: AttentionContext,
        policy: AppNotificationPolicy
    ): AttentionAction {
        if (!policy.enabled || category in policy.mutedCategories) return AttentionAction.IGNORE
        if (duplicate) return AttentionAction.STORE_ONLY
        if (context.deviceLocked && sensitivity >= NotificationSensitivity.SENSITIVE && !policy.allowOnLockScreen) {
            return if (urgency >= 0.92) AttentionAction.ASK else AttentionAction.DEFER
        }
        if (context.driving) {
            return if (category == NotificationCategory.CALL || urgency >= 0.95) AttentionAction.INTERRUPT else AttentionAction.DEFER
        }
        if (context.quietHours && urgency < 0.92) return AttentionAction.DEFER
        if ((context.userBusy || context.activeCall) && urgency < 0.95) return AttentionAction.DEFER
        if (!policy.allowInterruptions) return if (score >= 0.55) AttentionAction.SUMMARIZE else AttentionAction.STORE_ONLY
        return when {
            score >= 0.86 && urgency >= 0.80 -> AttentionAction.INTERRUPT
            score >= 0.68 -> AttentionAction.ASK
            score >= 0.46 -> AttentionAction.SUMMARIZE
            score >= 0.25 -> AttentionAction.STORE_ONLY
            else -> AttentionAction.IGNORE
        }
    }

    private fun classify(n: ContextNotification): NotificationCategory {
        val value = "${n.categoryHint.orEmpty()} ${n.title} ${n.text}".lowercase()
        return when {
            containsAny(value, "incoming call", "missed call", "calling", "video call") -> NotificationCategory.CALL
            containsAny(value, "otp", "verification", "security alert", "login", "signed in", "पासवर्ड", "ओटीपी") -> NotificationCategory.SECURITY
            containsAny(value, "credited", "debited", "payment", "transaction", "upi", "bank", "₹", "rs.") -> NotificationCategory.PAYMENT
            containsAny(value, "meeting", "calendar", "appointment", "event starts") -> NotificationCategory.CALENDAR
            containsAny(value, "reminder", "due", "task") -> NotificationCategory.REMINDER
            containsAny(value, "delivered", "out for delivery", "parcel", "order arriving") -> NotificationCategory.DELIVERY
            containsAny(value, "flight", "train", "boarding", "platform", "trip") -> NotificationCategory.TRAVEL
            containsAny(value, "medicine", "health", "doctor", "report", "hospital") -> NotificationCategory.HEALTH
            containsAny(value, "message", "replied", "sent you", "whatsapp", "telegram", "sms") || n.conversationKey != null -> NotificationCategory.MESSAGE
            containsAny(value, "sale", "offer", "discount", "coupon", "% off") -> NotificationCategory.PROMOTION
            n.ongoing && containsAny(value, "playing", "paused", "media") -> NotificationCategory.MEDIA
            containsAny(value, "system", "battery", "storage", "update available") -> NotificationCategory.SYSTEM
            containsAny(value, "liked", "commented", "followed", "social") -> NotificationCategory.SOCIAL
            else -> NotificationCategory.OTHER
        }
    }

    private fun sensitivity(n: ContextNotification, category: NotificationCategory): NotificationSensitivity {
        if (n.sensitiveHint) return NotificationSensitivity.SENSITIVE
        val value = "${n.title} ${n.text}".lowercase()
        return when {
            containsAny(value, "password", "passcode", "otp", "verification code", "card ending", "account number") -> NotificationSensitivity.HIGHLY_SENSITIVE
            category in setOf(NotificationCategory.PAYMENT, NotificationCategory.SECURITY, NotificationCategory.HEALTH) -> NotificationSensitivity.SENSITIVE
            category in setOf(NotificationCategory.MESSAGE, NotificationCategory.CALL, NotificationCategory.CALENDAR, NotificationCategory.TRAVEL) -> NotificationSensitivity.PERSONAL
            else -> NotificationSensitivity.PUBLIC
        }
    }

    private fun urgency(n: ContextNotification, category: NotificationCategory): Double {
        val value = "${n.title} ${n.text}".lowercase()
        var score = when (category) {
            NotificationCategory.CALL -> 0.96
            NotificationCategory.SECURITY -> 0.90
            NotificationCategory.CALENDAR, NotificationCategory.REMINDER, NotificationCategory.TRAVEL -> 0.72
            NotificationCategory.HEALTH -> 0.68
            NotificationCategory.PAYMENT -> 0.62
            NotificationCategory.MESSAGE -> 0.52
            NotificationCategory.DELIVERY -> 0.45
            NotificationCategory.SYSTEM -> 0.34
            NotificationCategory.OTHER -> 0.30
            NotificationCategory.SOCIAL -> 0.22
            NotificationCategory.MEDIA -> 0.12
            NotificationCategory.PROMOTION -> 0.05
        }
        if (containsAny(value, "urgent", "immediately", "now", "asap", "emergency", "cancelled", "failed", "अभी", "जरूरी")) score += 0.20
        if (containsAny(value, "in 5 min", "in 10 min", "starting now", "expires")) score += 0.15
        if (n.silent) score -= 0.08
        if (n.ongoing) score -= 0.08
        return score.coerceIn(0.0, 1.0)
    }

    private fun importance(n: ContextNotification, category: NotificationCategory, policy: AppNotificationPolicy): Double {
        var score = when (category) {
            NotificationCategory.SECURITY, NotificationCategory.CALL -> 0.90
            NotificationCategory.HEALTH, NotificationCategory.PAYMENT -> 0.78
            NotificationCategory.CALENDAR, NotificationCategory.REMINDER, NotificationCategory.TRAVEL -> 0.72
            NotificationCategory.MESSAGE -> 0.60
            NotificationCategory.DELIVERY -> 0.48
            NotificationCategory.SYSTEM -> 0.40
            NotificationCategory.OTHER -> 0.34
            NotificationCategory.SOCIAL -> 0.25
            NotificationCategory.MEDIA -> 0.12
            NotificationCategory.PROMOTION -> 0.05
        }
        if (!n.clearable) score += 0.04
        score += policy.priorityBoost
        return score.coerceIn(0.0, 1.0)
    }

    private fun summarize(n: ContextNotification, category: NotificationCategory, maskedText: String): String {
        val source = n.appLabel.ifBlank { n.sourcePackage }
        val body = maskedText.trim().replace(Regex("\\s+"), " ").take(240)
        return if (body.isBlank()) "$source se ${category.name.lowercase()} notification" else "$source: $body"
    }

    private fun fingerprint(n: ContextNotification, category: NotificationCategory): String {
        val normalized = listOf(n.sourcePackage, category.name, n.conversationKey.orEmpty(), n.title, n.text)
            .joinToString("|")
            .lowercase()
            .replace(Regex("\\d+"), "#")
            .replace(Regex("\\s+"), " ")
            .take(2_000)
        return MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }
    }

    private fun retention(category: NotificationCategory, sensitivity: NotificationSensitivity): Long = when {
        sensitivity == NotificationSensitivity.HIGHLY_SENSITIVE -> 30 * 60 * 1000L
        sensitivity == NotificationSensitivity.SENSITIVE -> 6 * 60 * 60 * 1000L
        category == NotificationCategory.PROMOTION -> 2 * 60 * 60 * 1000L
        else -> 3 * 24 * 60 * 60 * 1000L
    }

    private fun prune(timestamp: Long) {
        while (recent.firstOrNull()?.let { timestamp - it.timestamp > duplicateWindowMillis } == true) recent.removeFirst()
    }

    private fun containsAny(value: String, vararg terms: String): Boolean = terms.any(value::contains)
}
