package ai.mayra.app.vision

import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.Locale
import java.util.UUID

/** Stores only compact analysis metadata; raw image bytes are never retained here. */
data class VisionMemoryRecord(
    val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val fingerprint: String,
    val source: VisionAssetSource,
    val summary: String,
    val searchableText: String,
    val labels: Set<String>,
    val documentType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val sensitive: Boolean,
    val remoteProcessed: Boolean,
    val confidence: Double,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(assetId.isNotBlank() && fingerprint.isNotBlank())
        require(summary.isNotBlank())
        require(expiresAt > createdAt)
        require(confidence in 0.0..1.0)
        require(metadata.size <= 30)
    }

    fun expired(now: Long = System.currentTimeMillis()): Boolean = now >= expiresAt
}

data class VisionMemoryHit(
    val record: VisionMemoryRecord,
    val score: Double,
    val matchedTerms: Set<String>
) {
    init { require(score in 0.0..1.0) }
}

data class VisionMemoryPolicy(
    val enabled: Boolean = true,
    val publicRetentionMs: Long = 30L * 24 * 60 * 60 * 1000,
    val personalRetentionMs: Long = 14L * 24 * 60 * 60 * 1000,
    val sensitiveRetentionMs: Long = 24L * 60 * 60 * 1000,
    val highlySensitiveRetentionMs: Long = 0,
    val maximumRecords: Int = 200,
    val rememberRemoteResults: Boolean = true
) {
    init {
        require(publicRetentionMs >= 0 && personalRetentionMs >= 0 && sensitiveRetentionMs >= 0 && highlySensitiveRetentionMs >= 0)
        require(maximumRecords in 1..2_000)
    }

    fun retentionFor(sensitivity: VisionSensitivity): Long = when (sensitivity) {
        VisionSensitivity.PUBLIC -> publicRetentionMs
        VisionSensitivity.PERSONAL -> personalRetentionMs
        VisionSensitivity.SENSITIVE -> sensitiveRetentionMs
        VisionSensitivity.HIGHLY_SENSITIVE -> highlySensitiveRetentionMs
    }
}

data class VisionMemoryDiagnostics(
    val records: Int,
    val sensitiveRecords: Int,
    val remoteProcessedRecords: Int,
    val expiredPruned: Long,
    val duplicateMerges: Long,
    val deniedWrites: Long
)

class MayraVisionMemory(
    private val policy: VisionMemoryPolicy = VisionMemoryPolicy(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val records = ArrayDeque<VisionMemoryRecord>()
    private var expiredPruned = 0L
    private var duplicateMerges = 0L
    private var deniedWrites = 0L

    @Synchronized
    fun remember(request: VisionRequest, analysis: VisionAnalysis): VisionMemoryRecord? {
        val retention = policy.retentionFor(request.sensitivity)
        if (!policy.enabled || !request.allowMemory || retention <= 0 || (!policy.rememberRemoteResults && analysis.usedRemoteProcessing)) {
            deniedWrites++
            return null
        }
        prune()
        val fingerprint = request.asset.fingerprint ?: stableFingerprint(request.asset.uri, analysis.summary)
        val searchable = buildString {
            append(analysis.summary)
            append(' ')
            append(analysis.textBlocks.joinToString(" ") { it.text })
            append(' ')
            append(analysis.labels.joinToString(" ") { it.label })
            append(' ')
            append(analysis.receiptFields.joinToString(" ") { "${it.name} ${it.value}" })
        }.replace(Regex("\\s+"), " ").trim().take(8_000)

        val record = VisionMemoryRecord(
            assetId = request.asset.id,
            fingerprint = fingerprint,
            source = request.asset.source,
            summary = analysis.summary.take(1_500),
            searchableText = searchable,
            labels = analysis.labels.map { it.label.trim().lowercase(Locale.ROOT) }.filter(String::isNotBlank).take(50).toSet(),
            documentType = analysis.documentType,
            createdAt = now(),
            expiresAt = now() + retention,
            sensitive = request.sensitivity in setOf(VisionSensitivity.SENSITIVE, VisionSensitivity.HIGHLY_SENSITIVE),
            remoteProcessed = analysis.usedRemoteProcessing,
            confidence = analysis.confidence,
            metadata = mapOf(
                "tasks" to request.tasks.joinToString(",") { it.name },
                "mime" to request.asset.mimeType
            )
        )

        val existing = records.indexOfFirst { it.fingerprint == fingerprint }
        if (existing >= 0) {
            val list = records.toMutableList()
            list.removeAt(existing)
            records.clear()
            list.forEach(records::addLast)
            duplicateMerges++
        }
        records.addLast(record)
        while (records.size > policy.maximumRecords) records.removeFirst()
        return record
    }

    @Synchronized
    fun search(query: String, includeSensitive: Boolean = false, limit: Int = 20): List<VisionMemoryHit> {
        require(limit in 1..100)
        prune()
        val terms = tokenize(query)
        if (terms.isEmpty()) return emptyList()
        return records.asSequence()
            .filter { includeSensitive || !it.sensitive }
            .mapNotNull { record ->
                val corpus = tokenize(record.searchableText) + record.labels
                val matched = terms.filter { term -> corpus.any { token -> token.contains(term) || term.contains(token) } }.toSet()
                if (matched.isEmpty()) null else {
                    val coverage = matched.size.toDouble() / terms.size
                    val recency = (1.0 - ((now() - record.createdAt).coerceAtLeast(0).toDouble() /
                        (record.expiresAt - record.createdAt).coerceAtLeast(1))).coerceIn(0.0, 1.0)
                    VisionMemoryHit(record, (coverage * 0.65 + record.confidence * 0.25 + recency * 0.10).coerceIn(0.0, 1.0), matched)
                }
            }
            .sortedByDescending(VisionMemoryHit::score)
            .take(limit)
            .toList()
    }

    @Synchronized
    fun recent(limit: Int = 20, includeSensitive: Boolean = false): List<VisionMemoryRecord> {
        prune()
        return records.asSequence().filter { includeSensitive || !it.sensitive }.toList().takeLast(limit.coerceIn(1, 100)).reversed()
    }

    @Synchronized
    fun remove(id: String): Boolean {
        val retained = records.filterNot { it.id == id }
        if (retained.size == records.size) return false
        records.clear()
        retained.forEach(records::addLast)
        return true
    }

    @Synchronized
    fun clear(includeSensitiveOnly: Boolean = false): Int {
        val before = records.size
        val retained = if (includeSensitiveOnly) records.filterNot { it.sensitive } else emptyList()
        records.clear()
        retained.forEach(records::addLast)
        return before - records.size
    }

    @Synchronized
    fun prune() {
        val current = now()
        val retained = records.filterNot { it.expired(current) }
        expiredPruned += records.size - retained.size
        records.clear()
        retained.takeLast(policy.maximumRecords).forEach(records::addLast)
    }

    @Synchronized
    fun diagnostics(): VisionMemoryDiagnostics = VisionMemoryDiagnostics(
        records = records.size,
        sensitiveRecords = records.count(VisionMemoryRecord::sensitive),
        remoteProcessedRecords = records.count(VisionMemoryRecord::remoteProcessed),
        expiredPruned = expiredPruned,
        duplicateMerges = duplicateMerges,
        deniedWrites = deniedWrites
    )

    private fun tokenize(value: String): Set<String> = value.lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }
        .toSet()

    private fun stableFingerprint(vararg parts: String): String = MessageDigest.getInstance("SHA-256")
        .digest(parts.joinToString("|").toByteArray())
        .joinToString("") { "%02x".format(it) }
}

data class VisionIntentPlan(
    val tasks: Set<VisionTask>,
    val question: String? = null,
    val sensitivity: VisionSensitivity = VisionSensitivity.PERSONAL,
    val mode: VisionProcessingMode = VisionProcessingMode.PREFER_ON_DEVICE,
    val allowMemory: Boolean = false,
    val clarification: String? = null
)

class VisionIntentPlanner {
    fun plan(utterance: String, asset: VisionAsset?): VisionIntentPlan {
        val text = utterance.trim()
        val lower = text.lowercase(Locale.ROOT)
        if (asset == null) return VisionIntentPlan(setOf(VisionTask.DESCRIBE), clarification = "Pehle camera se photo lein ya gallery se image choose karein.")

        val tasks = linkedSetOf<VisionTask>()
        if (containsAny(lower, "text", "likha", "ocr", "padh", "read")) tasks += VisionTask.EXTRACT_TEXT
        if (containsAny(lower, "bill", "receipt", "total", "invoice")) tasks += VisionTask.READ_RECEIPT
        if (containsAny(lower, "document", "paper", "summar", "report")) tasks += setOf(VisionTask.CLASSIFY_DOCUMENT, VisionTask.SUMMARIZE_DOCUMENT)
        if (containsAny(lower, "barcode", "qr", "code scan")) tasks += VisionTask.DETECT_BARCODE
        if (containsAny(lower, "object", "kya kya", "cheez")) tasks += VisionTask.DETECT_OBJECTS
        if (containsAny(lower, "plant", "paudha", "पेड़", "पौधा")) tasks += VisionTask.IDENTIFY_PLANT
        if (containsAny(lower, "medicine", "tablet", "dawai", "दवा")) tasks += VisionTask.IDENTIFY_MEDICINE
        if (containsAny(lower, "kya hai", "what is", "describe", "batao", "photo me")) tasks += VisionTask.DESCRIBE

        val question = if (lower.contains('?') || containsAny(lower, "kya", "what", "which", "kitna", "kaun")) text else null
        if (question != null) tasks += VisionTask.ANSWER_QUESTION
        if (tasks.isEmpty()) tasks += VisionTask.DESCRIBE

        val sensitivity = when {
            containsAny(lower, "private", "personal", "medical", "prescription", "id card", "bank") -> VisionSensitivity.SENSITIVE
            containsAny(lower, "password", "otp", "card number", "aadhaar") -> VisionSensitivity.HIGHLY_SENSITIVE
            else -> if (asset.sensitive) VisionSensitivity.SENSITIVE else VisionSensitivity.PERSONAL
        }
        return VisionIntentPlan(
            tasks = tasks,
            question = question,
            sensitivity = sensitivity,
            mode = if (sensitivity == VisionSensitivity.HIGHLY_SENSITIVE) VisionProcessingMode.ON_DEVICE_ONLY else VisionProcessingMode.PREFER_ON_DEVICE,
            allowMemory = sensitivity !in setOf(VisionSensitivity.SENSITIVE, VisionSensitivity.HIGHLY_SENSITIVE)
        )
    }

    private fun containsAny(value: String, vararg options: String): Boolean = options.any(value::contains)
}

class VisionResponseComposer {
    fun compose(analysis: VisionAnalysis, locale: String = "hi-IN"): String {
        val hindi = locale.startsWith("hi", ignoreCase = true)
        val parts = mutableListOf<String>()
        parts += analysis.answer?.takeIf(String::isNotBlank) ?: analysis.summary
        if (analysis.receiptFields.isNotEmpty()) {
            val total = analysis.receiptFields.firstOrNull { it.name.contains("total", true) }
            total?.let { parts += if (hindi) "Total ${it.value} dikh raha hai." else "The total appears to be ${it.value}." }
        }
        if (analysis.textBlocks.isNotEmpty() && analysis.answer == null) {
            val preview = analysis.textBlocks.joinToString(" ") { it.text }.take(220)
            parts += if (hindi) "Likha hua text: $preview" else "Visible text: $preview"
        }
        if (analysis.warnings.isNotEmpty()) {
            parts += if (hindi) "Dhyan rahe: ${analysis.warnings.first()}" else "Note: ${analysis.warnings.first()}"
        }
        return parts.distinct().joinToString(" ").replace(Regex("\\s+"), " ").trim().take(2_000)
    }
}

data class MayraVisionSnapshot(
    val runtime: VisionRuntimeDiagnostics,
    val memory: VisionMemoryDiagnostics,
    val lastAnalysis: VisionAnalysis?
)

class MayraVisionCoordinator(
    private val runtime: MayraVisionRuntime,
    private val memory: MayraVisionMemory = MayraVisionMemory(),
    private val planner: VisionIntentPlanner = VisionIntentPlanner(),
    private val responses: VisionResponseComposer = VisionResponseComposer(),
    private val now: () -> Long = System::currentTimeMillis
) {
    @Volatile private var lastAnalysis: VisionAnalysis? = null

    suspend fun handle(utterance: String, asset: VisionAsset?): Pair<VisionRuntimeResult, String> {
        val plan = planner.plan(utterance, asset)
        if (plan.clarification != null || asset == null) {
            val result = VisionRuntimeResult.Blocked(plan.clarification ?: "Image required")
            return result to (plan.clarification ?: "Image required")
        }
        val request = VisionRequest(
            asset = asset,
            tasks = plan.tasks,
            question = plan.question,
            sensitivity = plan.sensitivity,
            mode = plan.mode,
            allowMemory = plan.allowMemory,
            createdAt = now(),
            expiresAt = now() + VisionRequest.DEFAULT_TTL_MS
        )
        return when (val result = runtime.analyze(request)) {
            is VisionRuntimeResult.Completed -> {
                lastAnalysis = result.analysis
                memory.remember(request, result.analysis)
                result to responses.compose(result.analysis, request.locale)
            }
            is VisionRuntimeResult.Blocked -> result to result.reason
            is VisionRuntimeResult.Failed -> result to "Image analysis complete nahi ho saki: ${result.reason}"
            is VisionRuntimeResult.DuplicateSuppressed -> result to "Ye image request abhi process ho chuki hai."
        }
    }

    fun searchMemory(query: String, includeSensitive: Boolean = false): List<VisionMemoryHit> = memory.search(query, includeSensitive)
    fun snapshot(): MayraVisionSnapshot = MayraVisionSnapshot(runtime.diagnostics(), memory.diagnostics(), lastAnalysis)
    fun clearMemory(): Int = memory.clear()
}
