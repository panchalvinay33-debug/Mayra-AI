package ai.mayra.app.context

import java.util.concurrent.atomic.AtomicLong


data class ContextRuntimeDiagnostics(
    val receivedNotifications: Long,
    val ignoredNotifications: Long,
    val deferredNotifications: Long,
    val summarizedNotifications: Long,
    val interruptions: Long,
    val recentInsightCount: Int,
    val appPolicyCount: Int,
    val conversationTurns: Int,
    val knownEntities: Int
)

class MayraContextRuntime(
    val conversation: ConversationContextEngine = ConversationContextEngine(),
    private val attention: NotificationAttentionEngine = NotificationAttentionEngine(),
    private val fusion: ContextFusionEngine = ContextFusionEngine(),
    private val maxInsights: Int = 200,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val policies = linkedMapOf<String, AppNotificationPolicy>()
    private val insights = ArrayDeque<NotificationInsight>()
    private val received = AtomicLong(0)
    private val ignored = AtomicLong(0)
    private val deferred = AtomicLong(0)
    private val summarized = AtomicLong(0)
    private val interruptions = AtomicLong(0)

    @Synchronized
    fun analyzeNotification(
        notification: ContextNotification,
        context: AttentionContext
    ): NotificationInsight {
        received.incrementAndGet()
        val policy = policies[notification.sourcePackage] ?: AppNotificationPolicy(notification.sourcePackage)
        val insight = attention.analyze(notification, context, policy)
        when (insight.action) {
            AttentionAction.IGNORE, AttentionAction.STORE_ONLY -> ignored.incrementAndGet()
            AttentionAction.DEFER -> deferred.incrementAndGet()
            AttentionAction.SUMMARIZE, AttentionAction.ASK -> summarized.incrementAndGet()
            AttentionAction.INTERRUPT -> interruptions.incrementAndGet()
        }
        if (policy.storeHistory && insight.action != AttentionAction.IGNORE) {
            insights += insight
            pruneExpired(context.now)
            while (insights.size > maxInsights) insights.removeFirst()
        }
        return insight
    }

    @Synchronized
    fun setAppPolicy(policy: AppNotificationPolicy) {
        policies[policy.packageName] = policy
    }

    @Synchronized
    fun removeAppPolicy(packageName: String): Boolean = policies.remove(packageName) != null

    @Synchronized
    fun appPolicies(): List<AppNotificationPolicy> = policies.values.toList()

    @Synchronized
    fun recentInsights(
        limit: Int = 50,
        includeSensitive: Boolean = false,
        actions: Set<AttentionAction> = AttentionAction.entries.toSet()
    ): List<NotificationInsight> {
        require(limit in 1..maxInsights)
        pruneExpired(now())
        return insights.asSequence()
            .filter { it.action in actions }
            .filter { includeSensitive || it.sensitivity < NotificationSensitivity.SENSITIVE }
            .sortedByDescending { it.attentionScore }
            .take(limit)
            .toList()
    }

    @Synchronized
    fun buildSnapshot(
        activeWorkflowId: String? = null,
        workflowSummary: String? = null,
        visionSummary: String? = null,
        nextCalendarEvent: String? = null,
        memoryFacts: List<String> = emptyList(),
        deviceLocked: Boolean = false,
        userBusy: Boolean = false
    ): FusedContextSnapshot = fusion.fuse(
        ContextFusionInput(
            conversation = conversation.snapshot(),
            notifications = recentInsights(limit = 20, includeSensitive = false),
            activeWorkflowId = activeWorkflowId,
            workflowSummary = workflowSummary,
            visionSummary = visionSummary,
            nextCalendarEvent = nextCalendarEvent,
            memoryFacts = memoryFacts,
            deviceLocked = deviceLocked,
            userBusy = userBusy,
            generatedAt = now()
        )
    )

    @Synchronized
    fun clearNotificationHistory() {
        insights.clear()
        attention.clearDuplicates()
    }

    @Synchronized
    fun diagnostics(): ContextRuntimeDiagnostics {
        pruneExpired(now())
        val session = conversation.snapshot(includeSensitive = true)
        return ContextRuntimeDiagnostics(
            receivedNotifications = received.get(),
            ignoredNotifications = ignored.get(),
            deferredNotifications = deferred.get(),
            summarizedNotifications = summarized.get(),
            interruptions = interruptions.get(),
            recentInsightCount = insights.size,
            appPolicyCount = policies.size,
            conversationTurns = session.turns.size,
            knownEntities = session.entities.size
        )
    }

    private fun pruneExpired(timestamp: Long) {
        if (insights.isEmpty()) return
        val retained = insights.filter { it.expiresAt > timestamp }
        insights.clear()
        insights.addAll(retained)
    }
}

/** Process-local holder used by Android services that may be created separately from activities. */
object MayraContextHolder {
    @Volatile
    var runtime: MayraContextRuntime = MayraContextRuntime()
}
