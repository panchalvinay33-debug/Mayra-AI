package ai.mayra.app.brain

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

enum class BrainEventType {
    USER_COMMAND,
    NOTIFICATION,
    SCHEDULE_TRIGGER,
    DEVICE_STATE,
    SKILL_RESULT,
    ACTION_CONFIRMATION,
    SYSTEM
}

enum class BrainPriority { LOW, NORMAL, HIGH, CRITICAL }

data class BrainEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: BrainEventType,
    val source: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val priority: BrainPriority = BrainPriority.NORMAL,
    val attributes: Map<String, String> = emptyMap()
)

data class BrainDecision(
    val eventId: String,
    val route: String,
    val reason: String,
    val requiresConfirmation: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

fun interface BrainEventSubscriber {
    fun onEvent(event: BrainEvent)
}

class BrainEventBus {
    private val subscribers = CopyOnWriteArrayList<BrainEventSubscriber>()
    private val publishedCount = AtomicLong(0)
    private val failureCount = AtomicLong(0)

    fun subscribe(subscriber: BrainEventSubscriber): AutoCloseable {
        subscribers += subscriber
        return AutoCloseable { subscribers -= subscriber }
    }

    fun publish(event: BrainEvent) {
        publishedCount.incrementAndGet()
        subscribers.forEach { subscriber ->
            runCatching { subscriber.onEvent(event) }
                .onFailure { failureCount.incrementAndGet() }
        }
    }

    fun diagnostics(): EventBusDiagnostics = EventBusDiagnostics(
        subscribers = subscribers.size,
        publishedEvents = publishedCount.get(),
        subscriberFailures = failureCount.get()
    )
}

data class EventBusDiagnostics(
    val subscribers: Int,
    val publishedEvents: Long,
    val subscriberFailures: Long
)

interface BrainPolicy {
    fun evaluate(event: BrainEvent, context: BrainContextSnapshot): BrainDecision?
}

data class BrainContextSnapshot(
    val hourOfDay: Int,
    val pendingActions: Int,
    val failedTasks: Int,
    val notificationAccessGranted: Boolean,
    val recentCommandCount: Int,
    val userAvailable: Boolean = true
)

class DefaultBrainPolicy : BrainPolicy {
    override fun evaluate(event: BrainEvent, context: BrainContextSnapshot): BrainDecision {
        val sensitive = event.attributes["sensitive"] == "true" ||
            event.priority == BrainPriority.CRITICAL ||
            event.payload.contains("call", ignoreCase = true) ||
            event.payload.contains("message", ignoreCase = true) ||
            event.payload.contains("payment", ignoreCase = true)

        val route = when {
            sensitive -> "pending_action"
            event.type == BrainEventType.SCHEDULE_TRIGGER -> "planner"
            event.type == BrainEventType.NOTIFICATION && event.priority <= BrainPriority.NORMAL -> "memory"
            event.type == BrainEventType.DEVICE_STATE -> "diagnostics"
            else -> "skill_registry"
        }

        return BrainDecision(
            eventId = event.id,
            route = route,
            reason = when {
                sensitive -> "Sensitive action requires explicit user confirmation"
                route == "planner" -> "Scheduled work must be decomposed into executable steps"
                route == "memory" -> "Low-risk context can be retained for local reasoning"
                route == "diagnostics" -> "Device state events update runtime health"
                else -> "Event should be resolved by the best registered skill"
            },
            requiresConfirmation = sensitive
        )
    }
}

class MayraBrainCoordinator(
    private val eventBus: BrainEventBus,
    private val contextProvider: () -> BrainContextSnapshot,
    private val policies: List<BrainPolicy> = listOf(DefaultBrainPolicy())
) {
    private val decisionCount = AtomicLong(0)
    private val confirmationCount = AtomicLong(0)
    @Volatile private var lastDecision: BrainDecision? = null

    fun process(event: BrainEvent): BrainDecision {
        eventBus.publish(event)
        val context = contextProvider()
        val decision = policies.asSequence()
            .mapNotNull { it.evaluate(event, context) }
            .firstOrNull()
            ?: BrainDecision(event.id, "fallback", "No policy matched", true)

        decisionCount.incrementAndGet()
        if (decision.requiresConfirmation) confirmationCount.incrementAndGet()
        lastDecision = decision
        return decision
    }

    fun diagnostics(): BrainDiagnostics = BrainDiagnostics(
        processedEvents = decisionCount.get(),
        confirmationDecisions = confirmationCount.get(),
        lastRoute = lastDecision?.route,
        eventBus = eventBus.diagnostics()
    )
}

data class BrainDiagnostics(
    val processedEvents: Long,
    val confirmationDecisions: Long,
    val lastRoute: String?,
    val eventBus: EventBusDiagnostics
)
