package ai.mayra.app.brain

import ai.mayra.app.background.PendingAction
import ai.mayra.app.background.PendingActionStore
import ai.mayra.app.background.PendingActionType
import android.content.Context
import java.util.LinkedHashMap
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

sealed interface RuntimeOutcome {
    data class Completed(val message: String, val data: Map<String, String> = emptyMap()) : RuntimeOutcome
    data class ConfirmationRequired(val pendingActionId: String, val prompt: String) : RuntimeOutcome
    data class PermissionRequired(val permissions: Set<String>, val explanation: String) : RuntimeOutcome
    data class Planned(val plan: MayraPlan) : RuntimeOutcome
    data class Remembered(val memoryId: String) : RuntimeOutcome
    data class Failed(val reason: String, val retryable: Boolean = false) : RuntimeOutcome
    data object IgnoredDuplicate : RuntimeOutcome
    data object NotHandled : RuntimeOutcome
}

data class RuntimeRequest(
    val command: String,
    val intent: String,
    val parameters: Map<String, String> = emptyMap(),
    val source: String = "user",
    val confirmed: Boolean = false,
    val priority: BrainPriority = BrainPriority.NORMAL,
    val createdAt: Long = System.currentTimeMillis()
)

data class RuntimeReceipt(
    val id: String = UUID.randomUUID().toString(),
    val request: RuntimeRequest,
    val decision: BrainDecision,
    val outcome: RuntimeOutcome,
    val startedAt: Long,
    val finishedAt: Long
) {
    val durationMillis: Long get() = (finishedAt - startedAt).coerceAtLeast(0)
}

data class RuntimeDiagnostics(
    val processedRequests: Long,
    val completedRequests: Long,
    val confirmationRequests: Long,
    val permissionRequests: Long,
    val failedRequests: Long,
    val duplicateRequests: Long,
    val averageLatencyMillis: Long,
    val recentReceiptCount: Int,
    val brain: BrainDiagnostics,
    val skills: SkillRegistryDiagnostics
)

fun interface RuntimePlanFactory {
    fun create(request: RuntimeRequest): MayraPlan?
}

class DefaultRuntimePlanFactory(private val planner: MayraTaskPlanner) : RuntimePlanFactory {
    override fun create(request: RuntimeRequest): MayraPlan? {
        val rawSteps = request.parameters["steps"]
            ?.split("||")
            ?.map(String::trim)
            ?.filter(String::isNotBlank)
            .orEmpty()
        if (rawSteps.isEmpty()) return null

        var previousId: String? = null
        val steps = rawSteps.mapIndexed { index, text ->
            val id = UUID.randomUUID().toString()
            val step = PlanStep(
                id = id,
                order = index,
                intent = inferIntent(text),
                description = text,
                dependsOn = previousId?.let(::setOf).orEmpty(),
                requiresConfirmation = requiresConfirmation(text),
                failurePolicy = FailurePolicy.RETRY
            )
            previousId = id
            step
        }
        return planner.createPlan(
            title = request.parameters["title"] ?: "Mayra task plan",
            originalCommand = request.command,
            steps = steps,
            scheduledFor = request.parameters["scheduledFor"]?.toLongOrNull()
        )
    }

    private fun inferIntent(text: String): String = when {
        text.contains("call", true) || text.contains("फोन", true) -> "call"
        text.contains("message", true) || text.contains("मैसेज", true) -> "message"
        text.contains("remind", true) || text.contains("याद", true) -> "reminder"
        text.contains("open", true) || text.contains("खोल", true) -> "open_app"
        else -> "general"
    }

    private fun requiresConfirmation(text: String): Boolean =
        listOf("call", "message", "send", "payment", "delete", "फोन", "मैसेज", "भेज")
            .any { text.contains(it, ignoreCase = true) }
}

class MayraRuntimeOrchestrator(
    context: Context,
    private val brain: MayraBrainCoordinator,
    private val skills: MayraSkillRegistry,
    private val planner: MayraTaskPlanner,
    private val memory: MayraContextMemory,
    private val planFactory: RuntimePlanFactory = DefaultRuntimePlanFactory(planner),
    private val pendingActions: PendingActionStore = PendingActionStore(context.applicationContext),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val processed = AtomicLong(0)
    private val completed = AtomicLong(0)
    private val confirmations = AtomicLong(0)
    private val permissions = AtomicLong(0)
    private val failures = AtomicLong(0)
    private val duplicates = AtomicLong(0)
    private val totalLatency = AtomicLong(0)
    private val recentReceipts = ArrayDeque<RuntimeReceipt>()
    private val recentFingerprints = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean = size > 100
    }

    suspend fun handle(request: RuntimeRequest): RuntimeReceipt {
        val started = now()
        val event = request.toBrainEvent()
        val decision = brain.process(event)
        val outcome = if (isDuplicate(request, started)) {
            duplicates.incrementAndGet()
            RuntimeOutcome.IgnoredDuplicate
        } else {
            route(request, decision, started)
        }
        val receipt = RuntimeReceipt(
            request = request,
            decision = decision,
            outcome = outcome,
            startedAt = started,
            finishedAt = now()
        )
        processed.incrementAndGet()
        totalLatency.addAndGet(receipt.durationMillis)
        trackOutcome(outcome)
        rememberReceipt(receipt)
        memory.observeHabit("command", request.intent.ifBlank { request.command.take(80) }, receipt.finishedAt)
        return receipt
    }

    fun recentReceipts(limit: Int = 20): List<RuntimeReceipt> = synchronized(recentReceipts) {
        recentReceipts.takeLast(limit.coerceAtLeast(0)).reversed()
    }

    fun diagnostics(): RuntimeDiagnostics {
        val count = processed.get()
        return RuntimeDiagnostics(
            processedRequests = count,
            completedRequests = completed.get(),
            confirmationRequests = confirmations.get(),
            permissionRequests = permissions.get(),
            failedRequests = failures.get(),
            duplicateRequests = duplicates.get(),
            averageLatencyMillis = if (count == 0L) 0 else totalLatency.get() / count,
            recentReceiptCount = synchronized(recentReceipts) { recentReceipts.size },
            brain = brain.diagnostics(),
            skills = skills.diagnostics()
        )
    }

    private suspend fun route(request: RuntimeRequest, decision: BrainDecision, timestamp: Long): RuntimeOutcome = when (decision.route) {
        "pending_action" -> createPendingAction(request, timestamp)
        "planner" -> planFactory.create(request)?.let(RuntimeOutcome::Planned)
            ?: RuntimeOutcome.Failed("I could not split this scheduled request into executable steps.")
        "memory" -> rememberContext(request, timestamp)
        "diagnostics" -> RuntimeOutcome.Completed("Runtime diagnostics updated.")
        "skill_registry" -> executeSkill(request)
        else -> RuntimeOutcome.NotHandled
    }

    private suspend fun executeSkill(request: RuntimeRequest): RuntimeOutcome {
        val result = skills.executeBest(
            SkillRequest(
                intent = request.intent,
                utterance = request.command,
                parameters = request.parameters,
                context = currentContext(),
                confirmed = request.confirmed
            )
        )
        return when (result) {
            is SkillResult.Success -> RuntimeOutcome.Completed(result.message, result.data)
            is SkillResult.NeedsConfirmation -> {
                val pending = pendingActions.add(
                    PendingAction(
                        type = result.actionType.toPendingActionType(),
                        title = result.prompt,
                        payload = result.payload,
                        createdAt = now()
                    )
                )
                RuntimeOutcome.ConfirmationRequired(pending.id, result.prompt)
            }
            is SkillResult.MissingPermission -> RuntimeOutcome.PermissionRequired(result.permissions, result.explanation)
            is SkillResult.Failure -> RuntimeOutcome.Failed(result.reason, result.retryable)
            SkillResult.NotHandled -> RuntimeOutcome.NotHandled
        }
    }

    private fun createPendingAction(request: RuntimeRequest, timestamp: Long): RuntimeOutcome {
        val pending = pendingActions.add(
            PendingAction(
                type = request.intent.toPendingActionType(),
                title = "Confirm ${request.intent.ifBlank { "action" }}",
                payload = request.command,
                createdAt = timestamp,
                expiresAt = request.parameters["expiresAt"]?.toLongOrNull(),
                source = request.source
            )
        )
        return RuntimeOutcome.ConfirmationRequired(pending.id, pending.title)
    }

    private fun rememberContext(request: RuntimeRequest, timestamp: Long): RuntimeOutcome {
        val record = memory.remember(
            MemoryRecord(
                kind = MemoryKind.CONTEXT,
                key = request.parameters["memoryKey"] ?: "${request.source}:${request.intent}",
                value = request.parameters["memoryValue"] ?: request.command.take(500),
                confidence = request.parameters["confidence"]?.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.55,
                sensitivity = MemorySensitivity.PUBLIC,
                createdAt = timestamp,
                expiresAt = request.parameters["expiresAt"]?.toLongOrNull()
            )
        )
        return RuntimeOutcome.Remembered(record.id)
    }

    private fun currentContext(): BrainContextSnapshot = BrainContextSnapshot(
        hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
        pendingActions = pendingActions.waiting().size,
        failedTasks = 0,
        notificationAccessGranted = true,
        recentCommandCount = memory.topHabits("command", 20).sumOf { it.count },
        userAvailable = true
    )

    private fun isDuplicate(request: RuntimeRequest, timestamp: Long): Boolean {
        val fingerprint = listOf(request.source, request.intent, request.command.trim().lowercase(), request.parameters.toSortedMap())
            .joinToString("|")
        synchronized(recentFingerprints) {
            recentFingerprints.entries.removeAll { timestamp - it.value > DEDUP_WINDOW_MILLIS }
            val previous = recentFingerprints[fingerprint]
            recentFingerprints[fingerprint] = timestamp
            return previous != null && timestamp - previous <= DEDUP_WINDOW_MILLIS
        }
    }

    private fun rememberReceipt(receipt: RuntimeReceipt) = synchronized(recentReceipts) {
        recentReceipts.addLast(receipt)
        while (recentReceipts.size > MAX_RECEIPTS) recentReceipts.removeFirst()
    }

    private fun trackOutcome(outcome: RuntimeOutcome) = when (outcome) {
        is RuntimeOutcome.Completed, is RuntimeOutcome.Planned, is RuntimeOutcome.Remembered -> completed.incrementAndGet()
        is RuntimeOutcome.ConfirmationRequired -> confirmations.incrementAndGet()
        is RuntimeOutcome.PermissionRequired -> permissions.incrementAndGet()
        is RuntimeOutcome.Failed -> failures.incrementAndGet()
        RuntimeOutcome.IgnoredDuplicate, RuntimeOutcome.NotHandled -> Unit
    }

    private fun RuntimeRequest.toBrainEvent(): BrainEvent = BrainEvent(
        type = if (parameters.containsKey("scheduledFor")) BrainEventType.SCHEDULE_TRIGGER else BrainEventType.USER_COMMAND,
        source = source,
        payload = command,
        createdAt = createdAt,
        priority = priority,
        attributes = parameters + mapOf("intent" to intent, "sensitive" to isSensitiveIntent(intent).toString())
    )

    private fun String.toPendingActionType(): PendingActionType = when (lowercase()) {
        "call", "phone" -> PendingActionType.CALL
        "message", "sms", "send_message" -> PendingActionType.MESSAGE
        "reminder" -> PendingActionType.REMINDER
        "security", "review_security" -> PendingActionType.SECURITY_REVIEW
        "open_app", "app" -> PendingActionType.OPEN_APP
        else -> PendingActionType.OTHER
    }

    private fun isSensitiveIntent(intent: String): Boolean = intent.lowercase() in setOf(
        "call", "message", "sms", "send_message", "payment", "delete", "security"
    )

    private companion object {
        const val DEDUP_WINDOW_MILLIS = 2_500L
        const val MAX_RECEIPTS = 100
    }
}
