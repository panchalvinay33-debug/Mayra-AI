package ai.mayra.app.vision

import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max

/** Framework-neutral image descriptor. Raw bytes stay outside the long-lived model boundary. */
data class VisionAsset(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val mimeType: String,
    val displayName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null,
    val capturedAt: Long = System.currentTimeMillis(),
    val source: VisionAssetSource,
    val fingerprint: String? = null,
    val sensitive: Boolean = false,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(uri.isNotBlank())
        require(mimeType.startsWith("image/"))
        width?.let { require(it > 0) }
        height?.let { require(it > 0) }
        sizeBytes?.let { require(it >= 0) }
        require(metadata.size <= 40)
    }
}

enum class VisionAssetSource { CAMERA, GALLERY, FILE_PICKER, SHARE, CLIPBOARD, GENERATED, UNKNOWN }
enum class VisionTask {
    DESCRIBE,
    EXTRACT_TEXT,
    CLASSIFY_DOCUMENT,
    READ_RECEIPT,
    DETECT_BARCODE,
    DETECT_OBJECTS,
    ANSWER_QUESTION,
    SUMMARIZE_DOCUMENT,
    IDENTIFY_PLANT,
    IDENTIFY_MEDICINE
}

enum class VisionProcessingMode { ON_DEVICE_ONLY, PREFER_ON_DEVICE, ALLOW_REMOTE }
enum class VisionProviderKind { ON_DEVICE, REMOTE, HYBRID, TEST }
enum class VisionSensitivity { PUBLIC, PERSONAL, SENSITIVE, HIGHLY_SENSITIVE }

data class VisionRequest(
    val id: String = UUID.randomUUID().toString(),
    val asset: VisionAsset,
    val tasks: Set<VisionTask>,
    val question: String? = null,
    val locale: String = "hi-IN",
    val mode: VisionProcessingMode = VisionProcessingMode.PREFER_ON_DEVICE,
    val sensitivity: VisionSensitivity = if (asset.sensitive) VisionSensitivity.SENSITIVE else VisionSensitivity.PERSONAL,
    val allowMemory: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = createdAt + DEFAULT_TTL_MS,
    val attributes: Map<String, String> = emptyMap()
) {
    init {
        require(tasks.isNotEmpty())
        require(question == null || question.length <= 2_000)
        require(attributes.size <= 30)
        require(expiresAt > createdAt)
        if (VisionTask.ANSWER_QUESTION in tasks) require(!question.isNullOrBlank())
    }

    fun expired(now: Long = System.currentTimeMillis()): Boolean = now >= expiresAt

    companion object { const val DEFAULT_TTL_MS = 5 * 60 * 1000L }
}

data class VisionTextBlock(
    val text: String,
    val confidence: Double,
    val languageTag: String? = null,
    val boundingBox: NormalizedBox? = null
) {
    init {
        require(text.isNotBlank())
        require(confidence in 0.0..1.0)
    }
}

data class VisionLabel(
    val label: String,
    val confidence: Double,
    val boundingBox: NormalizedBox? = null,
    val attributes: Map<String, String> = emptyMap()
) {
    init {
        require(label.isNotBlank())
        require(confidence in 0.0..1.0)
    }
}

data class VisionBarcode(
    val rawValue: String,
    val format: String,
    val confidence: Double = 1.0
) {
    init {
        require(rawValue.isNotBlank())
        require(format.isNotBlank())
        require(confidence in 0.0..1.0)
    }
}

data class NormalizedBox(val left: Double, val top: Double, val right: Double, val bottom: Double) {
    init {
        require(left in 0.0..1.0 && top in 0.0..1.0 && right in 0.0..1.0 && bottom in 0.0..1.0)
        require(right >= left && bottom >= top)
    }
}

data class ReceiptField(val name: String, val value: String, val confidence: Double) {
    init {
        require(name.isNotBlank() && value.isNotBlank())
        require(confidence in 0.0..1.0)
    }
}

data class VisionEvidence(
    val providerId: String,
    val task: VisionTask,
    val confidence: Double,
    val explanation: String? = null
) {
    init { require(confidence in 0.0..1.0) }
}

data class VisionAnalysis(
    val requestId: String,
    val assetId: String,
    val summary: String,
    val textBlocks: List<VisionTextBlock> = emptyList(),
    val labels: List<VisionLabel> = emptyList(),
    val barcodes: List<VisionBarcode> = emptyList(),
    val receiptFields: List<ReceiptField> = emptyList(),
    val documentType: String? = null,
    val answer: String? = null,
    val warnings: List<String> = emptyList(),
    val evidence: List<VisionEvidence> = emptyList(),
    val processedAt: Long = System.currentTimeMillis(),
    val processingMillis: Long,
    val usedRemoteProcessing: Boolean,
    val confidence: Double
) {
    init {
        require(summary.isNotBlank())
        require(processingMillis >= 0)
        require(confidence in 0.0..1.0)
        require(warnings.size <= 20)
    }
}

data class VisionProviderDescriptor(
    val id: String,
    val displayName: String,
    val kind: VisionProviderKind,
    val supportedTasks: Set<VisionTask>,
    val requiresNetwork: Boolean,
    val sendsImageOffDevice: Boolean,
    val maxImageBytes: Long,
    val priority: Int = 0
) {
    init {
        require(id.isNotBlank() && displayName.isNotBlank())
        require(supportedTasks.isNotEmpty())
        require(maxImageBytes > 0)
    }
}

sealed interface VisionProviderResult {
    data class Success(val analysis: VisionAnalysis) : VisionProviderResult
    data class Unsupported(val reason: String) : VisionProviderResult
    data class Failure(val message: String, val retryable: Boolean) : VisionProviderResult
}

interface MayraVisionProvider {
    val descriptor: VisionProviderDescriptor
    suspend fun analyze(request: VisionRequest): VisionProviderResult
}

data class VisionRuntimePolicy(
    val maximumImageBytes: Long = 20L * 1024 * 1024,
    val remoteAllowedForPersonal: Boolean = true,
    val remoteAllowedForSensitive: Boolean = false,
    val remoteAllowedForHighlySensitive: Boolean = false,
    val duplicateWindowMs: Long = 30_000,
    val maxProviderAttempts: Int = 3
) {
    init {
        require(maximumImageBytes > 0)
        require(duplicateWindowMs >= 0)
        require(maxProviderAttempts in 1..10)
    }
}

sealed interface VisionRuntimeResult {
    data class Completed(val analysis: VisionAnalysis) : VisionRuntimeResult
    data class Blocked(val reason: String) : VisionRuntimeResult
    data class Failed(val reason: String, val attempts: Int) : VisionRuntimeResult
    data class DuplicateSuppressed(val previousRequestId: String) : VisionRuntimeResult
}

data class VisionRuntimeDiagnostics(
    val requests: Long,
    val completed: Long,
    val blocked: Long,
    val failed: Long,
    val duplicateSuppressed: Long,
    val remoteRuns: Long,
    val averageLatencyMillis: Double,
    val providerFailures: Map<String, Long>
)

class MayraVisionRuntime(
    providers: Collection<MayraVisionProvider>,
    private val policy: VisionRuntimePolicy = VisionRuntimePolicy(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val providers = providers.associateBy { it.descriptor.id }
    private val recent = LinkedHashMap<String, Pair<String, Long>>()
    private val failures = mutableMapOf<String, Long>()
    private var requests = 0L
    private var completed = 0L
    private var blocked = 0L
    private var failed = 0L
    private var duplicates = 0L
    private var remoteRuns = 0L
    private var latencyTotal = 0L

    init { require(this.providers.size == providers.size) { "Duplicate vision provider ids" } }

    suspend fun analyze(request: VisionRequest): VisionRuntimeResult {
        requests++
        validate(request)?.let {
            blocked++
            return VisionRuntimeResult.Blocked(it)
        }

        val fingerprint = requestFingerprint(request)
        synchronized(this) {
            pruneRecent()
            recent[fingerprint]?.let { (previousId, timestamp) ->
                if (now() - timestamp <= policy.duplicateWindowMs) {
                    duplicates++
                    return VisionRuntimeResult.DuplicateSuppressed(previousId)
                }
            }
            recent[fingerprint] = request.id to now()
            while (recent.size > 100) recent.remove(recent.keys.first())
        }

        val eligible = providers.values
            .filter { it.descriptor.supportedTasks.containsAll(request.tasks) }
            .filter { providerAllowed(it.descriptor, request) }
            .filter { request.asset.sizeBytes == null || request.asset.sizeBytes <= it.descriptor.maxImageBytes }
            .sortedWith(compareByDescending<MayraVisionProvider> { providerScore(it.descriptor, request) }
                .thenByDescending { it.descriptor.priority })
            .take(policy.maxProviderAttempts)

        if (eligible.isEmpty()) {
            blocked++
            return VisionRuntimeResult.Blocked("No allowed vision provider supports this request")
        }

        var attempts = 0
        val reasons = mutableListOf<String>()
        for (provider in eligible) {
            attempts++
            when (val result = runCatching { provider.analyze(request) }
                .getOrElse { VisionProviderResult.Failure(it.message ?: "Provider crashed", true) }) {
                is VisionProviderResult.Success -> {
                    completed++
                    latencyTotal += result.analysis.processingMillis
                    if (result.analysis.usedRemoteProcessing) remoteRuns++
                    return VisionRuntimeResult.Completed(result.analysis)
                }
                is VisionProviderResult.Unsupported -> reasons += "${provider.descriptor.id}: ${result.reason}"
                is VisionProviderResult.Failure -> {
                    failures[provider.descriptor.id] = failures.getOrDefault(provider.descriptor.id, 0) + 1
                    reasons += "${provider.descriptor.id}: ${result.message}"
                    if (!result.retryable) break
                }
            }
        }
        failed++
        return VisionRuntimeResult.Failed(reasons.joinToString("; ").ifBlank { "Vision analysis failed" }, attempts)
    }

    @Synchronized
    fun diagnostics(): VisionRuntimeDiagnostics = VisionRuntimeDiagnostics(
        requests = requests,
        completed = completed,
        blocked = blocked,
        failed = failed,
        duplicateSuppressed = duplicates,
        remoteRuns = remoteRuns,
        averageLatencyMillis = if (completed == 0L) 0.0 else latencyTotal.toDouble() / completed,
        providerFailures = failures.toMap()
    )

    private fun validate(request: VisionRequest): String? = when {
        request.expired(now()) -> "Vision request expired"
        request.asset.sizeBytes != null && request.asset.sizeBytes > policy.maximumImageBytes -> "Image is too large"
        request.asset.mimeType !in SUPPORTED_MIME_TYPES -> "Unsupported image type"
        request.sensitivity == VisionSensitivity.HIGHLY_SENSITIVE && request.mode == VisionProcessingMode.ALLOW_REMOTE && !policy.remoteAllowedForHighlySensitive -> "Remote processing is blocked for highly sensitive images"
        else -> null
    }

    private fun providerAllowed(descriptor: VisionProviderDescriptor, request: VisionRequest): Boolean {
        if (request.mode == VisionProcessingMode.ON_DEVICE_ONLY && descriptor.sendsImageOffDevice) return false
        if (!descriptor.sendsImageOffDevice) return true
        return when (request.sensitivity) {
            VisionSensitivity.PUBLIC -> true
            VisionSensitivity.PERSONAL -> policy.remoteAllowedForPersonal
            VisionSensitivity.SENSITIVE -> policy.remoteAllowedForSensitive
            VisionSensitivity.HIGHLY_SENSITIVE -> policy.remoteAllowedForHighlySensitive
        }
    }

    private fun providerScore(descriptor: VisionProviderDescriptor, request: VisionRequest): Int {
        val locality = when {
            !descriptor.sendsImageOffDevice -> 100
            request.mode == VisionProcessingMode.ALLOW_REMOTE -> 70
            else -> 40
        }
        return locality + descriptor.priority + if (descriptor.supportedTasks == request.tasks) 10 else 0
    }

    private fun requestFingerprint(request: VisionRequest): String {
        val raw = listOf(request.asset.fingerprint ?: request.asset.uri, request.tasks.sortedBy(Enum<*>::name), request.question.orEmpty()).joinToString("|")
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    private fun pruneRecent() {
        val cutoff = now() - max(policy.duplicateWindowMs, 1)
        recent.entries.removeAll { it.value.second < cutoff }
    }

    companion object {
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp", "image/heic", "image/heif")
    }
}
