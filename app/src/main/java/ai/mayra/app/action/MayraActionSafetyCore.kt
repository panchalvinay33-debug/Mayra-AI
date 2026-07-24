package ai.mayra.app.action

import ai.mayra.app.core.actions.ActionRiskLevel
import ai.mayra.app.core.actions.ConfirmationTicket
import ai.mayra.app.core.actions.DeviceActionCoordinator
import ai.mayra.app.core.actions.DeviceActionExecutionResult
import ai.mayra.app.core.actions.DeviceActionRequest
import ai.mayra.app.core.actions.DeviceActionType
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.core.actions.PermissionSnapshot
import java.util.ArrayDeque

/** Capability state is intentionally explicit: unavailable actions must never fabricate success. */
enum class MayraCapabilityState { AVAILABLE, USER_SETUP_REQUIRED, DEVICE_UNSUPPORTED, POLICY_RESTRICTED }

data class MayraCapability(
    val actionType: DeviceActionType,
    val state: MayraCapabilityState,
    val reason: String? = null,
    val alternative: String? = null
)

class MayraCapabilityRegistry(
    capabilities: Collection<MayraCapability> = DeviceActionType.entries.map {
        MayraCapability(it, MayraCapabilityState.AVAILABLE)
    }
) {
    private val byType = capabilities.associateBy(MayraCapability::actionType)

    fun capabilityFor(type: DeviceActionType): MayraCapability = byType[type]
        ?: MayraCapability(type, MayraCapabilityState.DEVICE_UNSUPPORTED, "Capability was not registered.")
}

class MayraPermissionManager {
    fun missing(request: DeviceActionRequest, snapshot: PermissionSnapshot): Set<DevicePermission> =
        snapshot.missingFor(request)

    fun permanentlyDenied(request: DeviceActionRequest, snapshot: PermissionSnapshot): Set<DevicePermission> =
        snapshot.permanentlyDeniedFor(request)
}

enum class MayraActionRisk { LOW, MEDIUM, HIGH, CRITICAL }

data class MayraRiskDecision(
    val level: MayraActionRisk,
    val reasons: List<String>,
    val confirmationRequired: Boolean,
    val strongAuthenticationRecommended: Boolean,
    val doubleConfirmationRequired: Boolean
)

class MayraRiskClassifier {
    fun classify(request: DeviceActionRequest): MayraRiskDecision {
        val sensitive = request.metadata["sensitive"] == "true"
        val destructive = request.metadata["destructive"] == "true"
        val financial = request.metadata["financial"] == "true"
        val publicPost = request.metadata["publicPost"] == "true"
        val legalAcceptance = request.metadata["legalAcceptance"] == "true"

        val level = when {
            financial || legalAcceptance -> MayraActionRisk.CRITICAL
            destructive || publicPost || sensitive -> MayraActionRisk.HIGH
            request.riskLevel == ActionRiskLevel.HIGH -> MayraActionRisk.HIGH
            request.riskLevel == ActionRiskLevel.MEDIUM -> MayraActionRisk.MEDIUM
            else -> MayraActionRisk.LOW
        }
        val reasons = buildList {
            add("Base action risk: ${request.riskLevel.name.lowercase()}.")
            if (sensitive) add("Sensitive content is involved.")
            if (destructive) add("The action may destroy or remove data.")
            if (financial) add("A financial operation is involved.")
            if (publicPost) add("Content may be published publicly.")
            if (legalAcceptance) add("The action may create legal acceptance.")
        }
        return MayraRiskDecision(
            level = level,
            reasons = reasons,
            confirmationRequired = level >= MayraActionRisk.HIGH,
            strongAuthenticationRecommended = level >= MayraActionRisk.HIGH,
            doubleConfirmationRequired = destructive || level == MayraActionRisk.CRITICAL
        )
    }
}

enum class MayraVerificationStatus { VERIFIED, USER_VISIBLE_HANDOFF, UNVERIFIED, FAILED }

data class MayraActionVerification(
    val status: MayraVerificationStatus,
    val message: String
)

fun interface MayraActionVerifier {
    suspend fun verify(request: DeviceActionRequest, output: String?): MayraActionVerification
}

class HonestAndroidActionVerifier : MayraActionVerifier {
    override suspend fun verify(
        request: DeviceActionRequest,
        output: String?
    ): MayraActionVerification = when (request.type) {
        DeviceActionType.OPEN_APP -> MayraActionVerification(
            MayraVerificationStatus.USER_VISIBLE_HANDOFF,
            "Android accepted the request to open ${request.target}; the destination app remains responsible for its final state."
        )
        DeviceActionType.CALL_CONTACT -> MayraActionVerification(
            MayraVerificationStatus.USER_VISIBLE_HANDOFF,
            "The call flow was opened. Call connection is not claimed until the phone system confirms it."
        )
        DeviceActionType.SEND_MESSAGE -> MayraActionVerification(
            MayraVerificationStatus.USER_VISIBLE_HANDOFF,
            "The message composer was opened. The message is not claimed as sent."
        )
        DeviceActionType.CREATE_REMINDER -> MayraActionVerification(
            MayraVerificationStatus.USER_VISIBLE_HANDOFF,
            "The reminder creation screen was opened. Saving remains visible to the user."
        )
    }
}

data class MayraFallback(
    val title: String,
    val instruction: String,
    val safeAlternative: Boolean = true
)

class MayraFallbackPlanner {
    fun forCapability(capability: MayraCapability): MayraFallback = MayraFallback(
        title = "Action unavailable",
        instruction = capability.alternative
            ?: capability.reason
            ?: "Open the relevant app or settings screen and complete the final step manually."
    )

    fun forFailure(request: DeviceActionRequest): MayraFallback = when (request.type) {
        DeviceActionType.OPEN_APP -> MayraFallback("Open manually", "Open ${request.target} from the launcher.")
        DeviceActionType.CALL_CONTACT -> MayraFallback("Use the dialer", "Open the phone app and call ${request.target} manually.")
        DeviceActionType.SEND_MESSAGE -> MayraFallback("Prepare a draft", "Open your preferred messaging app and review the message before sending.")
        DeviceActionType.CREATE_REMINDER -> MayraFallback("Create manually", "Open Calendar or Clock and add the reminder manually.")
    }
}

enum class MayraActionEventStatus {
    REQUESTED, BLOCKED, WAITING_PERMISSION, WAITING_CONFIRMATION, CONFIRMED,
    EXECUTED, VERIFIED, REJECTED, FAILED, CANCELLED
}

data class MayraActionAuditEvent(
    val requestId: String,
    val type: DeviceActionType,
    val status: MayraActionEventStatus,
    val timestamp: Long,
    val detail: String? = null
)

class MayraActionAuditLog(
    private val maxEntries: Int = 300,
    private val clock: () -> Long = System::currentTimeMillis
) {
    init { require(maxEntries in 20..5_000) }
    private val entries = ArrayDeque<MayraActionAuditEvent>()

    @Synchronized
    fun record(request: DeviceActionRequest, status: MayraActionEventStatus, detail: String? = null) {
        entries.addLast(MayraActionAuditEvent(request.id, request.type, status, clock(), detail?.take(500)))
        while (entries.size > maxEntries) entries.removeFirst()
    }

    @Synchronized fun snapshot(): List<MayraActionAuditEvent> = entries.toList()
    @Synchronized fun clear(): Int = entries.size.also { entries.clear() }
}

sealed interface MayraActionResult {
    data class Completed(
        val request: DeviceActionRequest,
        val output: String?,
        val verification: MayraActionVerification
    ) : MayraActionResult

    data class AwaitingPermission(
        val request: DeviceActionRequest,
        val missing: Set<DevicePermission>,
        val permanentlyDenied: Set<DevicePermission>
    ) : MayraActionResult

    data class AwaitingConfirmation(
        val request: DeviceActionRequest,
        val ticket: ConfirmationTicket,
        val prompt: String,
        val risk: MayraRiskDecision
    ) : MayraActionResult

    data class Blocked(
        val request: DeviceActionRequest,
        val capability: MayraCapability,
        val fallback: MayraFallback
    ) : MayraActionResult

    data class Rejected(val request: DeviceActionRequest, val reason: String) : MayraActionResult

    data class Failed(
        val request: DeviceActionRequest,
        val message: String,
        val fallback: MayraFallback
    ) : MayraActionResult
}

/**
 * Single policy boundary for Mayra phone actions. It reuses the existing coordinator rather than
 * creating a second executor, and adds capability, verification, fallback, audit and kill-switch layers.
 */
class MayraActionEngine(
    private val coordinator: DeviceActionCoordinator,
    private val capabilityRegistry: MayraCapabilityRegistry = MayraCapabilityRegistry(),
    private val permissionManager: MayraPermissionManager = MayraPermissionManager(),
    private val riskClassifier: MayraRiskClassifier = MayraRiskClassifier(),
    private val verifier: MayraActionVerifier = HonestAndroidActionVerifier(),
    private val fallbackPlanner: MayraFallbackPlanner = MayraFallbackPlanner(),
    private val auditLog: MayraActionAuditLog = MayraActionAuditLog()
) {
    @Volatile private var stopped = false

    suspend fun submit(
        request: DeviceActionRequest,
        permissions: PermissionSnapshot
    ): MayraActionResult {
        auditLog.record(request, MayraActionEventStatus.REQUESTED)
        if (stopped) return blockedByStop(request)

        val capability = capabilityRegistry.capabilityFor(request.type)
        if (capability.state != MayraCapabilityState.AVAILABLE) {
            auditLog.record(request, MayraActionEventStatus.BLOCKED, capability.reason)
            return MayraActionResult.Blocked(request, capability, fallbackPlanner.forCapability(capability))
        }

        val missing = permissionManager.missing(request, permissions)
        if (missing.isNotEmpty()) {
            auditLog.record(request, MayraActionEventStatus.WAITING_PERMISSION, missing.joinToString())
        }
        return mapCoordinatorResult(request, coordinator.submit(request, permissions))
    }

    suspend fun confirm(token: String): MayraActionResult {
        if (stopped) {
            val placeholder = DeviceActionRequest(
                id = "stopped-confirmation",
                type = DeviceActionType.OPEN_APP,
                target = "cancelled action",
                createdAt = System.currentTimeMillis()
            )
            return blockedByStop(placeholder)
        }
        return mapCoordinatorResult(null, coordinator.confirm(token))
    }

    fun reject(token: String, reason: String = "User cancelled the action."): MayraActionResult {
        val result = coordinator.reject(token, reason)
        return when (result) {
            is DeviceActionExecutionResult.Rejected -> {
                auditLog.record(result.request, MayraActionEventStatus.REJECTED, result.reason)
                MayraActionResult.Rejected(result.request, result.reason)
            }
            else -> error("Reject must produce a rejected result.")
        }
    }

    fun stopAll() { stopped = true }
    fun resume() { stopped = false }
    fun isStopped(): Boolean = stopped
    fun auditSnapshot(): List<MayraActionAuditEvent> = auditLog.snapshot()
    fun clearAudit(): Int = auditLog.clear()

    private suspend fun mapCoordinatorResult(
        originalRequest: DeviceActionRequest?,
        result: DeviceActionExecutionResult
    ): MayraActionResult = when (result) {
        is DeviceActionExecutionResult.Completed -> {
            auditLog.record(result.request, MayraActionEventStatus.EXECUTED, result.output)
            val verification = runCatching { verifier.verify(result.request, result.output) }
                .getOrElse {
                    MayraActionVerification(
                        MayraVerificationStatus.UNVERIFIED,
                        "The action was handed to Android, but final completion could not be verified."
                    )
                }
            auditLog.record(result.request, MayraActionEventStatus.VERIFIED, verification.message)
            MayraActionResult.Completed(result.request, result.output, verification)
        }
        is DeviceActionExecutionResult.AwaitingPermission -> MayraActionResult.AwaitingPermission(
            result.request, result.missing, result.permanentlyDenied
        )
        is DeviceActionExecutionResult.AwaitingConfirmation -> {
            val risk = riskClassifier.classify(result.request)
            auditLog.record(result.request, MayraActionEventStatus.WAITING_CONFIRMATION, risk.level.name)
            MayraActionResult.AwaitingConfirmation(
                result.request,
                result.ticket,
                result.prompt,
                risk
            )
        }
        is DeviceActionExecutionResult.Rejected -> {
            auditLog.record(result.request, MayraActionEventStatus.REJECTED, result.reason)
            MayraActionResult.Rejected(result.request, result.reason)
        }
        is DeviceActionExecutionResult.Failed -> {
            val request = result.request
            auditLog.record(request, MayraActionEventStatus.FAILED, result.message)
            MayraActionResult.Failed(request, result.message, fallbackPlanner.forFailure(request))
        }
    }

    private fun blockedByStop(request: DeviceActionRequest): MayraActionResult.Blocked {
        val capability = MayraCapability(
            request.type,
            MayraCapabilityState.POLICY_RESTRICTED,
            "Mayra action execution is stopped by the user.",
            "Resume actions from Privacy or Action controls when you are ready."
        )
        auditLog.record(request, MayraActionEventStatus.CANCELLED, capability.reason)
        return MayraActionResult.Blocked(request, capability, fallbackPlanner.forCapability(capability))
    }
}
