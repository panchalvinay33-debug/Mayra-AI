package ai.mayra.app.core.actions

import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID

enum class DeviceActionType {
    OPEN_APP,
    CALL_CONTACT,
    SEND_MESSAGE,
    CREATE_REMINDER
}

enum class DevicePermission {
    QUERY_APPS,
    CALL_PHONE,
    READ_CONTACTS,
    SEND_MESSAGES,
    POST_NOTIFICATIONS,
    SCHEDULE_EXACT_ALARM
}

enum class ActionRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class DeviceActionRequest(
    val id: String = UUID.randomUUID().toString(),
    val type: DeviceActionType,
    val target: String,
    val payload: String? = null,
    val createdAt: Long,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Request id cannot be blank." }
        require(id.length <= 160) { "Request id is too long." }
        require(target.isNotBlank()) { "Action target cannot be blank." }
        require(target.length <= 500) { "Action target is too long." }
        require(payload == null || payload.length <= 8_000) { "Action payload is too long." }
        require(createdAt >= 0L) { "createdAt cannot be negative." }
        require(metadata.size <= 40) { "Action metadata has too many entries." }
        require(metadata.all { it.key.length <= 80 && it.value.length <= 500 }) { "Action metadata is too large." }
    }

    val riskLevel: ActionRiskLevel
        get() = when (type) {
            DeviceActionType.OPEN_APP -> ActionRiskLevel.LOW
            DeviceActionType.CREATE_REMINDER -> ActionRiskLevel.MEDIUM
            DeviceActionType.CALL_CONTACT,
            DeviceActionType.SEND_MESSAGE -> ActionRiskLevel.HIGH
        }

    /**
     * Calls and messages are review-first Android handoffs. Mayra needs contact lookup access when
     * resolving an owner-selected contact, but never requests direct CALL_PHONE or SEND_SMS access.
     */
    val requiredPermissions: Set<DevicePermission>
        get() = when (type) {
            DeviceActionType.OPEN_APP -> setOf(DevicePermission.QUERY_APPS)
            DeviceActionType.CALL_CONTACT,
            DeviceActionType.SEND_MESSAGE -> setOf(DevicePermission.READ_CONTACTS)
            DeviceActionType.CREATE_REMINDER -> setOf(DevicePermission.POST_NOTIFICATIONS)
        }

    val requiresConfirmation: Boolean
        get() = riskLevel == ActionRiskLevel.HIGH

    fun safetyFingerprint(): String {
        val canonical = buildString {
            append(type.name)
            append('\u001F')
            append(normalizeActionField(target))
            append('\u001F')
            append(normalizeActionField(payload.orEmpty()))
            append('\u001F')
            metadata.toSortedMap().forEach { (key, value) ->
                if (key !in volatileMetadataKeys) {
                    append(normalizeActionField(key))
                    append('=')
                    append(normalizeActionField(value))
                    append(';')
                }
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val volatileMetadataKeys = setOf("timestamp", "createdAt", "requestId", "traceId")
    }
}

data class PermissionSnapshot(
    val granted: Set<DevicePermission> = emptySet(),
    val permanentlyDenied: Set<DevicePermission> = emptySet()
) {
    fun missingFor(request: DeviceActionRequest): Set<DevicePermission> =
        request.requiredPermissions - granted

    fun permanentlyDeniedFor(request: DeviceActionRequest): Set<DevicePermission> =
        request.requiredPermissions intersect permanentlyDenied
}

data class ConfirmationTicket(
    val token: String,
    val requestId: String,
    val issuedAt: Long,
    val expiresAt: Long,
    val requestFingerprint: String = ""
) {
    init {
        require(token.isNotBlank())
        require(requestId.isNotBlank())
        require(issuedAt >= 0L)
        require(expiresAt > issuedAt)
        require(requestFingerprint.length <= 64)
    }

    fun isExpired(now: Long): Boolean = now >= expiresAt
}

sealed interface ActionGateDecision {
    data class Ready(val request: DeviceActionRequest) : ActionGateDecision

    data class NeedsPermission(
        val request: DeviceActionRequest,
        val missing: Set<DevicePermission>,
        val permanentlyDenied: Set<DevicePermission>
    ) : ActionGateDecision

    data class NeedsConfirmation(
        val request: DeviceActionRequest,
        val ticket: ConfirmationTicket,
        val prompt: String
    ) : ActionGateDecision

    data class Rejected(
        val request: DeviceActionRequest,
        val reason: String
    ) : ActionGateDecision
}

enum class ActionAuditStatus {
    REQUESTED,
    WAITING_FOR_PERMISSION,
    WAITING_FOR_CONFIRMATION,
    CONFIRMED,
    REJECTED,
    EXECUTED,
    FAILED,
    EXPIRED,
    DUPLICATE_BLOCKED,
    STALE_BLOCKED
}

data class ActionAuditEntry(
    val requestId: String,
    val type: DeviceActionType,
    val status: ActionAuditStatus,
    val timestamp: Long,
    val detail: String? = null
)

data class ActionSafetySnapshot(
    val pendingConfirmations: Int,
    val auditEntries: List<ActionAuditEntry>
)

/** Pure validation and duplicate policy shared by the action safety gate and its tests. */
object DeviceActionRequestSafetyPolicy {
    private const val MAX_REQUEST_AGE_MILLIS = 10 * 60 * 1_000L
    private const val MAX_FUTURE_SKEW_MILLIS = 60_000L

    fun rejectionReason(request: DeviceActionRequest, now: Long): String? = when {
        request.target.any { it.code < 32 && it != '\n' && it != '\t' } -> "Action target contains unsupported control characters."
        request.payload.orEmpty().any { it.code == 0 } -> "Action payload contains unsupported control characters."
        request.createdAt > now + MAX_FUTURE_SKEW_MILLIS -> "Action request time is invalid."
        now - request.createdAt > MAX_REQUEST_AGE_MILLIS -> "Action request is stale. Please ask Mayra again."
        request.type == DeviceActionType.CALL_CONTACT && normalizeActionField(request.target).length < 2 ->
            "Call target is too ambiguous."
        request.type == DeviceActionType.SEND_MESSAGE && normalizeActionField(request.target).length < 2 ->
            "Message recipient is too ambiguous."
        else -> null
    }
}

class DeviceActionSafetyGate(
    private val confirmationTtlMillis: Long = DEFAULT_CONFIRMATION_TTL_MILLIS,
    private val duplicateWindowMillis: Long = DEFAULT_DUPLICATE_WINDOW_MILLIS,
    private val maxAuditEntries: Int = DEFAULT_MAX_AUDIT_ENTRIES,
    private val clock: () -> Long = System::currentTimeMillis,
    private val tokenFactory: () -> String = { UUID.randomUUID().toString() }
) {
    init {
        require(confirmationTtlMillis > 0L) { "Confirmation TTL must be positive." }
        require(duplicateWindowMillis > 0L) { "Duplicate window must be positive." }
        require(maxAuditEntries in 20..5_000) { "Audit size is out of range." }
    }

    private val confirmations = linkedMapOf<String, PendingConfirmation>()
    private val pendingByFingerprint = linkedMapOf<String, String>()
    private val recentlyExecuted = linkedMapOf<String, Long>()
    private val audit = ArrayDeque<ActionAuditEntry>()

    @Synchronized
    fun evaluate(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot
    ): ActionGateDecision {
        val now = clock()
        prune(now)
        record(request, ActionAuditStatus.REQUESTED)

        DeviceActionRequestSafetyPolicy.rejectionReason(request, now)?.let { reason ->
            record(request, ActionAuditStatus.STALE_BLOCKED, reason)
            return ActionGateDecision.Rejected(request, reason)
        }

        val fingerprint = request.safetyFingerprint()
        if (pendingByFingerprint.containsKey(fingerprint)) {
            val reason = "An identical action is already waiting for confirmation."
            record(request, ActionAuditStatus.DUPLICATE_BLOCKED, reason)
            return ActionGateDecision.Rejected(request, reason)
        }
        if (recentlyExecuted[fingerprint]?.let { elapsed(now, it) < duplicateWindowMillis } == true) {
            val reason = "An identical action was already handed to Android recently."
            record(request, ActionAuditStatus.DUPLICATE_BLOCKED, reason)
            return ActionGateDecision.Rejected(request, reason)
        }

        val missing = permissions.missingFor(request)
        if (missing.isNotEmpty()) {
            val permanent = permissions.permanentlyDeniedFor(request)
            record(
                request,
                ActionAuditStatus.WAITING_FOR_PERMISSION,
                missing.joinToString(prefix = "missing=", separator = ",")
            )
            return ActionGateDecision.NeedsPermission(request, missing, permanent)
        }

        if (!request.requiresConfirmation) {
            return ActionGateDecision.Ready(request)
        }

        val ticket = ConfirmationTicket(
            token = tokenFactory(),
            requestId = request.id,
            issuedAt = now,
            expiresAt = now + confirmationTtlMillis,
            requestFingerprint = fingerprint
        )
        confirmations[ticket.token] = PendingConfirmation(request, ticket, fingerprint)
        pendingByFingerprint[fingerprint] = ticket.token
        record(request, ActionAuditStatus.WAITING_FOR_CONFIRMATION)
        return ActionGateDecision.NeedsConfirmation(
            request = request,
            ticket = ticket,
            prompt = confirmationPrompt(request)
        )
    }

    @Synchronized
    fun confirm(token: String): ActionGateDecision {
        require(token.isNotBlank()) { "Confirmation token cannot be blank." }
        val now = clock()
        prune(now)
        val pending = removePending(token)
            ?: return rejectedUnknownToken(now)

        if (pending.ticket.isExpired(now)) {
            record(pending.request, ActionAuditStatus.EXPIRED)
            return ActionGateDecision.Rejected(pending.request, "Confirmation expired.")
        }
        if (pending.ticket.requestFingerprint != pending.request.safetyFingerprint()) {
            val reason = "Action details changed after confirmation was requested."
            record(pending.request, ActionAuditStatus.REJECTED, reason)
            return ActionGateDecision.Rejected(pending.request, reason)
        }
        if (recentlyExecuted[pending.fingerprint]?.let { elapsed(now, it) < duplicateWindowMillis } == true) {
            val reason = "This action was already handed to Android."
            record(pending.request, ActionAuditStatus.DUPLICATE_BLOCKED, reason)
            return ActionGateDecision.Rejected(pending.request, reason)
        }

        record(pending.request, ActionAuditStatus.CONFIRMED)
        return ActionGateDecision.Ready(pending.request)
    }

    @Synchronized
    fun reject(
        token: String,
        reason: String = "User cancelled the action."
    ): ActionGateDecision.Rejected {
        require(token.isNotBlank()) { "Confirmation token cannot be blank." }
        val pending = removePending(token)
            ?: return rejectedUnknownToken(clock())
        val cleanReason = sanitizeAuditDetail(reason).ifBlank { "User cancelled the action." }
        record(pending.request, ActionAuditStatus.REJECTED, cleanReason)
        return ActionGateDecision.Rejected(pending.request, cleanReason)
    }

    @Synchronized
    fun recordExecuted(request: DeviceActionRequest, detail: String? = null) {
        val now = clock()
        recentlyExecuted[request.safetyFingerprint()] = now
        prune(now)
        record(request, ActionAuditStatus.EXECUTED, detail)
    }

    @Synchronized
    fun recordFailed(request: DeviceActionRequest, safeError: String) {
        val clean = sanitizeAuditDetail(safeError).ifBlank { "Action failed." }
        record(request, ActionAuditStatus.FAILED, clean)
    }

    @Synchronized
    fun snapshot(): ActionSafetySnapshot {
        prune(clock())
        return ActionSafetySnapshot(
            pendingConfirmations = confirmations.size,
            auditEntries = audit.toList()
        )
    }

    @Synchronized
    fun clearAudit(): Int = audit.size.also { audit.clear() }

    private fun rejectedUnknownToken(now: Long): ActionGateDecision.Rejected {
        val placeholder = DeviceActionRequest(
            id = "unknown-confirmation",
            type = DeviceActionType.OPEN_APP,
            target = "unknown",
            createdAt = now
        )
        return ActionGateDecision.Rejected(placeholder, "Confirmation is invalid or expired.")
    }

    private fun removePending(token: String): PendingConfirmation? {
        val pending = confirmations.remove(token) ?: return null
        if (pendingByFingerprint[pending.fingerprint] == token) {
            pendingByFingerprint.remove(pending.fingerprint)
        }
        return pending
    }

    private fun prune(now: Long) {
        val expiredTokens = confirmations.values
            .filter { it.ticket.isExpired(now) }
            .map { it.ticket.token }
        expiredTokens.forEach { token ->
            removePending(token)?.let { record(it.request, ActionAuditStatus.EXPIRED) }
        }
        recentlyExecuted.entries.removeAll { elapsed(now, it.value) >= duplicateWindowMillis }
    }

    private fun confirmationPrompt(request: DeviceActionRequest): String = when (request.type) {
        DeviceActionType.CALL_CONTACT -> "Confirm opening the dialer for ${safePromptTarget(request.target)}."
        DeviceActionType.SEND_MESSAGE -> if (request.payload.isNullOrBlank()) {
            "Confirm opening a message to ${safePromptTarget(request.target)}."
        } else {
            "Confirm preparing this message for ${safePromptTarget(request.target)}. Review it before sending."
        }
        DeviceActionType.OPEN_APP -> "Confirm opening ${safePromptTarget(request.target)}."
        DeviceActionType.CREATE_REMINDER -> "Confirm reminder: ${safePromptTarget(request.target)}."
    }

    private fun record(
        request: DeviceActionRequest,
        status: ActionAuditStatus,
        detail: String? = null
    ) {
        audit.addLast(
            ActionAuditEntry(
                requestId = request.id.take(160),
                type = request.type,
                status = status,
                timestamp = clock(),
                detail = detail?.let(::sanitizeAuditDetail)?.take(300)
            )
        )
        while (audit.size > maxAuditEntries) audit.removeFirst()
    }

    private data class PendingConfirmation(
        val request: DeviceActionRequest,
        val ticket: ConfirmationTicket,
        val fingerprint: String
    )

    private companion object {
        const val DEFAULT_CONFIRMATION_TTL_MILLIS = 60_000L
        const val DEFAULT_DUPLICATE_WINDOW_MILLIS = 30_000L
        const val DEFAULT_MAX_AUDIT_ENTRIES = 500
    }
}

internal fun normalizeActionField(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("[\\r\\n\\t]+"), " ")
    .replace(Regex("\\s+"), " ")
    .take(8_000)

private fun safePromptTarget(value: String): String = value
    .replace(Regex("[\\r\\n\\t]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .take(120)

private fun sanitizeAuditDetail(value: String): String = value
    .replace(Regex("(?i)bearer\\s+[a-z0-9._~-]+"), "Bearer [redacted]")
    .replace(Regex("(?i)\\bsk-[a-z0-9_-]{8,}"), "[redacted-key]")
    .replace(Regex("[\\r\\n\\t]+"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()

private fun elapsed(now: Long, then: Long): Long = if (now >= then) now - then else 0L
