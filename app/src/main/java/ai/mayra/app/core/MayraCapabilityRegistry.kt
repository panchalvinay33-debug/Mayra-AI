package ai.mayra.app.core

enum class MayraCapabilityStatus {
    DONE,
    DEVICE_VERIFY,
    IN_PROGRESS,
    PLANNED,
    DEFERRED
}

data class MayraCapability(
    val id: String,
    val module: String,
    val title: String,
    val status: MayraCapabilityStatus,
    val note: String
)

data class MayraCapabilitySnapshot(
    val total: Int,
    val done: Int,
    val deviceVerify: Int,
    val inProgress: Int,
    val planned: Int,
    val deferred: Int,
    val byModule: Map<String, List<MayraCapability>>
) {
    val implemented: Int get() = done + deviceVerify
    val implementedPercent: Int get() = if (total == 0) 0 else implemented * 100 / total
}

/**
 * Machine-testable product status registry.
 *
 * This is intentionally conservative: a capability is DONE only after automated validation, and
 * DEVICE_VERIFY means implementation/CI exist but owner-device evidence is still pending.
 */
object MayraCapabilityRegistry {
    val capabilities: List<MayraCapability> = listOf(
        MayraCapability("core.query-routing", "Core assistant", "Typed deterministic query routing", MayraCapabilityStatus.DONE, "ANSWER, RETRIEVE, ACT, CLARIFY and UNSUPPORTED outcomes include reason, confidence, capability and confirmation policy."),
        MayraCapability("core.provider-eligibility", "Core assistant", "Provider and tool eligibility rules", MayraCapabilityStatus.IN_PROGRESS, "Next milestone: capability availability, privacy and freshness gates before execution."),
        MayraCapability("core.capability-registry", "Core assistant", "Global capability and roadmap registry", MayraCapabilityStatus.DONE, "Single machine-testable status source for program reporting."),
        MayraCapability("documents.foundation", "Documents", "Private document intelligence foundation", MayraCapabilityStatus.DONE, "Sixteen of eighteen tracked foundation features are implemented."),
        MayraCapability("documents.device-validation", "Documents", "Latest PDF/DOCX/freshness maintenance phone validation", MayraCapabilityStatus.DEVICE_VERIFY, "CI is green; owner-device verification remains."),
        MayraCapability("documents.ocr", "Documents", "On-device OCR", MayraCapabilityStatus.DEFERRED, "Separate milestone for scanned PDFs and images."),
        MayraCapability("documents.legacy-doc", "Documents", "Legacy binary DOC parsing", MayraCapabilityStatus.DEFERRED, "Separate lower-priority parser milestone."),
        MayraCapability("memory.user-controlled", "Memory", "User-controlled personal memory", MayraCapabilityStatus.PLANNED, "Requires consent, provenance, edit/delete and expiry controls."),
        MayraCapability("search.provider-layer", "Search", "Provider-neutral fresh search and citations", MayraCapabilityStatus.PLANNED, "Requires privacy, freshness, citations and fallback contracts."),
        MayraCapability("actions.typed-execution", "Actions", "Typed confirmed action execution", MayraCapabilityStatus.PLANNED, "Requires confirmation, idempotency and result history."),
        MayraCapability("voice.hinglish", "Voice", "Hindi/Hinglish voice intelligence", MayraCapabilityStatus.PLANNED, "Controlled milestone; stable voice behavior must not be replaced casually."),
        MayraCapability("privacy.permission-audit", "Privacy", "Least-privilege binary permission/component audit", MayraCapabilityStatus.DONE, "Isolated document-test builds are audited in CI."),
        MayraCapability("release.android-ci", "Release", "Android compile/test/lint/R8 artifact pipeline", MayraCapabilityStatus.DONE, "Authoritative CI pipeline is active."),
        MayraCapability("governance.blueprint-backup", "Governance", "Blueprint, roadmap and backup discipline", MayraCapabilityStatus.DONE, "Every coding batch must update roadmap and latest snapshot.")
    )

    fun snapshot(): MayraCapabilitySnapshot = MayraCapabilitySnapshot(
        total = capabilities.size,
        done = capabilities.count { it.status == MayraCapabilityStatus.DONE },
        deviceVerify = capabilities.count { it.status == MayraCapabilityStatus.DEVICE_VERIFY },
        inProgress = capabilities.count { it.status == MayraCapabilityStatus.IN_PROGRESS },
        planned = capabilities.count { it.status == MayraCapabilityStatus.PLANNED },
        deferred = capabilities.count { it.status == MayraCapabilityStatus.DEFERRED },
        byModule = capabilities.groupBy { it.module }
    )

    fun requireValid() {
        check(capabilities.isNotEmpty()) { "Mayra capability registry cannot be empty." }
        check(capabilities.map { it.id }.distinct().size == capabilities.size) {
            "Mayra capability IDs must be unique."
        }
        check(capabilities.none { it.id.isBlank() || it.module.isBlank() || it.title.isBlank() }) {
            "Every Mayra capability requires an ID, module and title."
        }
    }
}