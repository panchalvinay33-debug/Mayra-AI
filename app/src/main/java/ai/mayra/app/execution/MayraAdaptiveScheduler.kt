package ai.mayra.app.execution

import ai.mayra.app.device.DeviceAnalysis
import java.util.ArrayDeque
import java.util.UUID

enum class SchedulingDecision { RUN_NOW, KEEP_POSITION, PROMOTE, DEFER, REQUIRE_REVIEW }
enum class RuntimeEventType { INSPECTION, FINDING, SCHEDULE_RECOMMENDATION, ARCHIVED, MAINTENANCE }

data class SchedulingRecommendation(
    val requestId: String,
    val decision: SchedulingDecision,
    val recommendedPriority: ExecutionPriority,
    val score: Int,
    val reasons: List<String>,
    val retryAfterMillis: Long? = null
)

data class RuntimeSupervisorEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: RuntimeEventType,
    val message: String,
    val requestId: String? = null,
    val timestamp: Long,
    val attributes: Map<String, String> = emptyMap()
)

fun interface RuntimeEventListener {
    fun onEvent(event: RuntimeSupervisorEvent)
}

class RuntimeSupervisorEventBus(
    private val maxEvents: Int = 500
) {
    private val events = ArrayDeque<RuntimeSupervisorEvent>()
    private val listeners = linkedSetOf<RuntimeEventListener>()

    init { require(maxEvents in 50..5_000) }

    @Synchronized
    fun subscribe(listener: RuntimeEventListener) { listeners += listener }

    @Synchronized
    fun unsubscribe(listener: RuntimeEventListener) { listeners -= listener }

    fun publish(event: RuntimeSupervisorEvent) {
        val callbacks: List<RuntimeEventListener>
        synchronized(this) {
            events.addLast(event)
            while (events.size > maxEvents) events.removeFirst()
            callbacks = listeners.toList()
        }
        callbacks.forEach { listener -> runCatching { listener.onEvent(event) } }
    }

    @Synchronized
    fun recent(limit: Int = 100): List<RuntimeSupervisorEvent> =
        events.toList().takeLast(limit.coerceIn(1, maxEvents)).asReversed()
}

data class AdaptiveSchedulerPolicy(
    val agingPromotionMillis: Long = 20 * 60_000L,
    val urgentAgingMillis: Long = 2 * 60 * 60_000L,
    val retryPenalty: Int = 12,
    val failureReviewAttempts: Int = 4,
    val lowBatteryThreshold: Int = 15,
    val thermalRetryMillis: Long = 10 * 60_000L,
    val offlineRetryMillis: Long = 5 * 60_000L
) {
    init {
        require(agingPromotionMillis in 60_000L until urgentAgingMillis)
        require(retryPenalty in 1..30)
        require(failureReviewAttempts in 2..10)
        require(lowBatteryThreshold in 5..40)
        require(thermalRetryMillis >= 60_000L)
        require(offlineRetryMillis >= 60_000L)
    }
}

class MayraAdaptiveScheduler(
    private val policy: AdaptiveSchedulerPolicy = AdaptiveSchedulerPolicy(),
    private val eventBus: RuntimeSupervisorEventBus = RuntimeSupervisorEventBus(),
    private val now: () -> Long = System::currentTimeMillis
) {
    fun recommend(
        request: ExecutionRequest,
        device: DeviceAnalysis?,
        analytics: ExecutionAnalytics
    ): SchedulingRecommendation {
        val timestamp = now()
        val reasons = mutableListOf<String>()
        var score = request.priority.weight
        var decision = SchedulingDecision.KEEP_POSITION
        var priority = request.priority
        var retryAfter: Long? = null

        val age = (timestamp - request.createdAt).coerceAtLeast(0L)
        when {
            age >= policy.urgentAgingMillis && request.priority != ExecutionPriority.URGENT -> {
                score += 35
                priority = ExecutionPriority.URGENT
                decision = SchedulingDecision.PROMOTE
                reasons += "Workflow has waited long enough for urgent aging promotion."
            }
            age >= policy.agingPromotionMillis && request.priority.weight < ExecutionPriority.HIGH.weight -> {
                score += 20
                priority = when (request.priority) {
                    ExecutionPriority.LOW -> ExecutionPriority.NORMAL
                    ExecutionPriority.NORMAL -> ExecutionPriority.HIGH
                    else -> request.priority
                }
                decision = SchedulingDecision.PROMOTE
                reasons += "Workflow age justifies one bounded priority promotion."
            }
        }

        if (request.attempts > 1) {
            score -= (request.attempts - 1) * policy.retryPenalty
            reasons += "Repeated attempts reduce immediate scheduling confidence."
        }
        if (request.attempts >= policy.failureReviewAttempts) {
            decision = SchedulingDecision.REQUIRE_REVIEW
            reasons += "Retry budget is nearly exhausted; manual review is safer."
        }

        if (device == null) {
            score -= 10
            reasons += "Device state is unavailable."
        } else {
            val snapshot = device.snapshot
            if (snapshot.battery.levelPercent <= policy.lowBatteryThreshold && !snapshot.battery.charging &&
                ExecutionResource.CPU_HEAVY in request.resources) {
                decision = SchedulingDecision.DEFER
                retryAfter = 15 * 60_000L
                score -= 40
                reasons += "CPU-heavy work should wait while battery is low."
            }
            if (snapshot.thermal.name in setOf("SEVERE", "CRITICAL", "EMERGENCY", "SHUTDOWN")) {
                decision = SchedulingDecision.DEFER
                retryAfter = policy.thermalRetryMillis
                score -= 60
                reasons += "Thermal pressure makes execution unsafe right now."
            }
            if (ExecutionResource.NETWORK in request.resources && !snapshot.network.validated) {
                decision = SchedulingDecision.DEFER
                retryAfter = policy.offlineRetryMillis
                score -= 50
                reasons += "Validated network is unavailable."
            }
            if (ExecutionResource.NETWORK in request.resources && snapshot.network.metered && request.priority.weight < ExecutionPriority.HIGH.weight) {
                decision = SchedulingDecision.DEFER
                retryAfter = 30 * 60_000L
                score -= 20
                reasons += "Non-urgent network work should avoid a metered connection."
            }
        }

        val demand = request.resources.maxOfOrNull { analytics.resourceDemand[it] ?: 0 } ?: 0
        if (demand >= 10 && decision !in setOf(SchedulingDecision.DEFER, SchedulingDecision.REQUIRE_REVIEW)) {
            score -= 10
            reasons += "Required execution resources are currently congested."
        }

        if (decision == SchedulingDecision.KEEP_POSITION && score >= ExecutionPriority.HIGH.weight && request.state in setOf(
                ExecutionRequestState.QUEUED,
                ExecutionRequestState.WAITING
            )) {
            decision = SchedulingDecision.RUN_NOW
            reasons += "Priority and device conditions support immediate dispatch."
        }

        val result = SchedulingRecommendation(
            requestId = request.id,
            decision = decision,
            recommendedPriority = priority,
            score = score.coerceIn(0, 150),
            reasons = reasons.ifEmpty { listOf("Current queue position remains appropriate.") },
            retryAfterMillis = retryAfter
        )
        eventBus.publish(
            RuntimeSupervisorEvent(
                type = RuntimeEventType.SCHEDULE_RECOMMENDATION,
                message = "Scheduling recommendation: ${result.decision.name.lowercase()}.",
                requestId = request.id,
                timestamp = timestamp,
                attributes = mapOf(
                    "score" to result.score.toString(),
                    "priority" to result.recommendedPriority.name
                )
            )
        )
        return result
    }

    fun recommendAll(
        requests: List<ExecutionRequest>,
        device: DeviceAnalysis?,
        analytics: ExecutionAnalytics,
        limit: Int = 50
    ): List<SchedulingRecommendation> = requests.asSequence()
        .filter { it.state in setOf(ExecutionRequestState.QUEUED, ExecutionRequestState.WAITING, ExecutionRequestState.BLOCKED) }
        .map { recommend(it, device, analytics) }
        .sortedWith(compareByDescending<SchedulingRecommendation> { it.decision == SchedulingDecision.RUN_NOW }
            .thenByDescending { it.score })
        .take(limit.coerceIn(1, 200))
        .toList()

    fun events(limit: Int = 100): List<RuntimeSupervisorEvent> = eventBus.recent(limit)
}
