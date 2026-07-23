package ai.mayra.app.autonomy

import ai.mayra.app.brain.BrainContextSnapshot
import ai.mayra.app.brain.MayraSkillRegistry
import android.content.Context
import java.util.UUID

data class AutonomySnapshot(
    val conversations: Int,
    val activeConversations: Int,
    val goals: GoalDiagnostics,
    val workflows: WorkflowDiagnostics,
    val lastDecision: DecisionScore?
)

data class GoalWorkflowLink(
    val goal: MayraGoal,
    val workflow: MayraWorkflow,
    val conversation: ConversationSession?
)

class MayraAutonomyCoordinator(
    context: Context,
    skills: MayraSkillRegistry,
    contextProvider: () -> BrainContextSnapshot,
    private val conversations: ConversationStateEngine = ConversationStateEngine(context),
    private val goals: GoalEngine = GoalEngine(context),
    private val decisionEngine: AutonomousDecisionEngine = AutonomousDecisionEngine(),
    private val workflows: WorkflowRuntime = WorkflowRuntime(skills, contextProvider)
) {
    @Volatile
    private var lastDecision: DecisionScore? = null

    fun startConversation(title: String, initialUserMessage: String? = null): ConversationSession {
        var session = conversations.create(title)
        if (!initialUserMessage.isNullOrBlank()) {
            session = conversations.append(
                session.id,
                ConversationTurn(role = ConversationRole.USER, text = initialUserMessage.trim())
            )
        }
        return session
    }

    fun addUserTurn(sessionId: String, text: String, intent: String? = null): ConversationSession =
        conversations.append(
            sessionId,
            ConversationTurn(role = ConversationRole.USER, text = text.trim(), intent = intent)
        )

    fun addAssistantTurn(
        sessionId: String,
        text: String,
        expectsReply: Boolean = false,
        intent: String? = null
    ): ConversationSession = conversations.append(
        sessionId,
        ConversationTurn(
            role = ConversationRole.ASSISTANT,
            text = text.trim(),
            intent = intent,
            attributes = if (expectsReply) mapOf("expects_reply" to "true") else emptyMap()
        )
    )

    fun createGoalWithWorkflow(
        title: String,
        description: String,
        milestones: List<String>,
        workflowNodes: List<WorkflowNode>,
        conversationId: String? = null,
        priority: GoalPriority = GoalPriority.NORMAL,
        dueAt: Long? = null
    ): GoalWorkflowLink {
        require(workflowNodes.isNotEmpty())
        val goal = goals.create(
            MayraGoal(
                title = title,
                description = description,
                priority = priority,
                dueAt = dueAt,
                milestones = milestones.filter(String::isNotBlank).map { GoalMilestone(title = it.trim()) }
            )
        )
        val workflow = workflows.submit(
            MayraWorkflow(
                title = "$title workflow",
                goalId = goal.id,
                nodes = workflowNodes
            )
        )
        val conversation = conversationId?.let { conversations.attachGoal(it, goal.id) }
        return GoalWorkflowLink(goal, workflow, conversation)
    }

    fun decide(input: DecisionInput): DecisionScore = decisionEngine.decide(input).also { lastDecision = it }

    suspend fun executeWorkflowStep(workflowId: String): WorkflowStepResult {
        val result = workflows.executeNext(workflowId)
        updateGoalFromWorkflow(result.workflow)
        return result
    }

    suspend fun drainWorkflow(workflowId: String, maxSteps: Int = 30): MayraWorkflow {
        val workflow = workflows.drain(workflowId, maxSteps)
        updateGoalFromWorkflow(workflow)
        return workflow
    }

    fun confirmWorkflowNode(workflowId: String, nodeId: String): Boolean = workflows.confirmNode(workflowId, nodeId)

    fun cancelWorkflow(workflowId: String): MayraWorkflow? {
        val workflow = workflows.cancel(workflowId) ?: return null
        workflow.goalId?.let { goalId -> runCatching { goals.cancel(goalId) } }
        return workflow
    }

    fun completeGoalMilestone(goalId: String, milestoneId: String): MayraGoal = goals.completeMilestone(goalId, milestoneId)
    fun pauseGoal(goalId: String): MayraGoal = goals.pause(goalId)
    fun resumeGoal(goalId: String): MayraGoal = goals.resume(goalId)
    fun blockGoal(goalId: String, reason: String): MayraGoal = goals.block(goalId, reason)

    fun snapshot(): AutonomySnapshot {
        val sessions = conversations.snapshot()
        return AutonomySnapshot(
            conversations = sessions.size,
            activeConversations = sessions.count { it.state in setOf(ConversationState.ACTIVE, ConversationState.WAITING_FOR_USER) },
            goals = goals.diagnostics(),
            workflows = workflows.diagnostics(),
            lastDecision = lastDecision
        )
    }

    fun conversationEngine(): ConversationStateEngine = conversations
    fun goalEngine(): GoalEngine = goals
    fun workflowRuntime(): WorkflowRuntime = workflows

    fun maintenance() {
        conversations.prune()
    }

    private fun updateGoalFromWorkflow(workflow: MayraWorkflow) {
        val goalId = workflow.goalId ?: return
        val actionNodes = workflow.nodes.filter { it.type == WorkflowNodeType.ACTION }
        val completed = workflow.nodes.count { it.state in setOf(WorkflowNodeState.COMPLETED, WorkflowNodeState.SKIPPED) }
        val percent = if (workflow.nodes.isEmpty()) 0 else completed * 100 / workflow.nodes.size
        when (workflow.state) {
            WorkflowState.COMPLETED -> runCatching { goals.reportProgress(goalId, 100) }
            WorkflowState.FAILED -> runCatching { goals.block(goalId, "Workflow failed") }
            WorkflowState.BLOCKED -> runCatching { goals.block(goalId, "Workflow requires attention") }
            WorkflowState.CANCELLED -> runCatching { goals.cancel(goalId) }
            else -> if (actionNodes.isNotEmpty()) runCatching { goals.reportProgress(goalId, percent) }
        }
    }

    companion object {
        fun simpleActionNode(
            order: Int,
            title: String,
            intent: String,
            parameters: Map<String, String> = emptyMap(),
            dependsOn: Set<String> = emptySet(),
            requiresConfirmation: Boolean = false
        ): WorkflowNode = WorkflowNode(
            id = UUID.randomUUID().toString(),
            order = order,
            type = WorkflowNodeType.ACTION,
            title = title,
            intent = intent,
            parameters = parameters,
            dependsOn = dependsOn,
            requiresConfirmation = requiresConfirmation
        )
    }
}
