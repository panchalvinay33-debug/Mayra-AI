package ai.mayra.app.core.actions

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
        require(target.isNotBlank()) { "Action target cannot be blank." }
        require(createdAt >= 0L) { "createdAt cannot be negative." }
    }

    val riskLevel: ActionRiskLevel
        get() = when (type) {
            DeviceActionType.OPEN_APP -> ActionRiskLevel.LOW
            DeviceActionType.CREATE_REMINDER -> ActionRiskLevel.MEDIUM
            DeviceActionType.CALL_CONTACT,
            DeviceActionType.SEND_MESSAGE -> ActionRiskLevel.HIGH
        }

    /**
     * Call/message handoff is review-first: Mayra only resolves the contact and opens Android's
     * dialer/composer. Direct CALL_PHONE and SEND_SMS permissions are intentionally not required.
     */
    val requiredPermissions: Set<DevicePermission>
        get() = when (type) {
            DeviceActionType.OPEN_APP -> setOf(DevicePermission.QUERY_APPS)
            DeviceActionType.CALL_CONTACT -> setOf(DevicePermission.READ_CONTACTS)
            DeviceActionType.SEND_MESSAGE -> setOf(DevicePermission.READ_CONTACTS)
            DeviceActionType.CREATE_REMINDER -> setOf(DevicePermission.POST_NOTIFICATIONS)
        }

    val requiresConfirmation: Boolean
        get() = riskLevel == ActionRiskLevel.HIGH
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
    val expiresAt: Long
) {
    init {
        require(token.isNotBlank())
        require(requestId.isNotBlank())
        require(issuedAt >= 0L)
        require(expiresAt > issuedAt)
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
    EXPIRED
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

class DeviceActionSafetyGate(
    private val confirmationTtlMillis: Long = DEFAULT_CONFIRMATION_TTL_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
    private val tokenFactory: () -> String = { UUID.randomUUID().toString() }
) {
    init {
        require(confirmationTtlMillis > 0L) { "Confirmation TTL must be positive." }
    }

    private val confirmations = linkedMapOf<String, PendingConfirmation>()
    private val audit = mutableListOf<ActionAuditEntry>()

    @Synchronized
    fun evaluate(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot
    ): ActionGateDecision {
        expireTickets(clock())
        record(request, ActionAuditStatus.REQUESTED)

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

        val now = clock()
        val ticket = ConfirmationTicket(
            token = tokenFactory(),
            requestId = request.id,
            issuedAt = now,
            expiresAt = now + confirmationTtlMillis
        )
        confirmations[ticket.token] = PendingConfirmation(request, ticket)
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
        expireTickets(now)
        val pending = confirmations.remove(token)
            ?: return rejectedUnknownToken(token, now)

        if (pending.ticket.isExpired(now)) {
            record(pending.request, ActionAuditStatus.EXPIRED)
            return ActionGateDecision.Rejected(pending.request, "Confirmation expired.")
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
        val pending = confirmations.remove(token)
            ?: return rejectedUnknownToken(token, clock())
        val cleanReason = reason.trim().ifBlank { "User cancelled the action." }
        record(pending.request, ActionAuditStatus.REJECTED, cleanReason)
        return ActionGateDecision.Rejected(pending.request, cleanReason)
    }

    @Synchronized
    fun recordExecuted(request: DeviceActionRequest, detail: String? = null) {
        record(request, ActionAuditStatus.EXECUTED, detail)
    }

    @Synchronized
    fun recordFailed(request: DeviceActionRequest, safeError: String) {
        val clean = safeError.trim().ifBlank { "Action failed." }
        record(request, ActionAuditStatus.FAILED, clean)
    }

    @Synchronized
    fun snapshot(): ActionSafetySnapshot {
        expireTickets(clock())
        return ActionSafetySnapshot(
            pendingConfirmations = confirmations.size,
            auditEntries = audit.toList()
        )
    }

    @Synchronized
    fun clearAudit(): Int {
        val count = audit.size
        audit.clear()
        return count
    }

    private fun rejectedUnknownToken(token: String, now: Long): ActionGateDecision.Rejected {
        val placeholder = DeviceActionRequest(
            id = "unknown:$token",
            type = DeviceActionType.OPEN_APP,
            target = "unknown",
            createdAt = now
        )
        return ActionGateDecision.Rejected(placeholder, "Confirmation is invalid or expired.")
    }

    private fun expireTickets(now: Long) {
        val expired = confirmations.values.filter { it.ticket.isExpired(now) }
        expired.forEach {
            confirmations.remove(it.ticket.token)
            record(it.request, ActionAuditStatus.EXPIRED)
        }
    }

    private fun confirmationPrompt(request: DeviceActionRequest): String = when (request.type) {
        DeviceActionType.CALL_CONTACT -> "Confirm opening the dialer for ${request.target}."
        DeviceActionType.SEND_MESSAGE -> if (request.payload.isNullOrBlank()) {
            "Confirm opening a message to ${request.target}."
        } else {
            "Confirm preparing this message to ${request.target}."
        }
        DeviceActionType.OPEN_APP -> "Confirm opening ${request.target}."
        DeviceActionType.CREATE_REMINDER -> "Confirm reminder: ${request.target}."
    }

    private fun record(
        request: DeviceActionRequest,
        status: ActionAuditStatus,
        detail: String? = null
    ) {
        audit += ActionAuditEntry(
            requestId = request.id,
            type = request.type,
            status = status,
            timestamp = clock(),
            detail = detail
        )
    }

    private data class PendingConfirmation(
        val request: DeviceActionRequest,
        val ticket: ConfirmationTicket
    )

    private companion object {
        const val DEFAULT_CONFIRMATION_TTL_MILLIS = 60_000L
    }
}
