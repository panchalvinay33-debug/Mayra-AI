package ai.mayra.app.background

/** Pure safety boundary used before notification content enters Mayra stores or automation queues. */
data class NotificationCaptureDecision(
    val capture: Boolean,
    val registerReply: Boolean,
    val allowProactiveTask: Boolean,
    val safeConversationKey: String?,
    val reason: String
)

object MayraNotificationSafetyPolicy {
    fun decide(
        privacyMode: NotificationPrivacyMode,
        allowReply: Boolean,
        sensitivity: NotificationSensitivity,
        rawConversationKey: String?,
        globalStopActive: Boolean,
        proactiveSuggestionsEnabled: Boolean
    ): NotificationCaptureDecision {
        if (privacyMode == NotificationPrivacyMode.IGNORE) {
            return NotificationCaptureDecision(false, false, false, null, "source_ignored")
        }

        val safeConversation = sanitizeConversationKey(rawConversationKey, sensitivity, privacyMode)
        val replyAllowed = !globalStopActive &&
            allowReply &&
            sensitivity != NotificationSensitivity.OTP
        val proactiveAllowed = !globalStopActive &&
            proactiveSuggestionsEnabled &&
            sensitivity == NotificationSensitivity.NORMAL &&
            privacyMode == NotificationPrivacyMode.FULL

        return NotificationCaptureDecision(
            capture = true,
            registerReply = replyAllowed,
            allowProactiveTask = proactiveAllowed,
            safeConversationKey = safeConversation,
            reason = when {
                globalStopActive -> "global_stop"
                sensitivity == NotificationSensitivity.OTP -> "otp_protected"
                sensitivity == NotificationSensitivity.SENSITIVE -> "sensitive_store_only"
                privacyMode == NotificationPrivacyMode.REDACT_CONTENT -> "redacted_store_only"
                else -> "normal"
            }
        )
    }

    fun sanitizeConversationKey(
        raw: String?,
        sensitivity: NotificationSensitivity,
        privacyMode: NotificationPrivacyMode
    ): String? {
        val clean = raw.orEmpty().replace(Regex("[\\r\\n\\t]+"), " ").trim().take(120)
        if (clean.isBlank()) return null
        if (privacyMode == NotificationPrivacyMode.REDACT_CONTENT) return "Private conversation"
        if (sensitivity != NotificationSensitivity.NORMAL) return "Protected conversation"
        return NotificationContentGuard.redactSecrets(clean).take(120)
    }
}
