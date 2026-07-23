package ai.mayra.app.autonomy

import ai.mayra.app.brain.BrainContextSnapshot
import ai.mayra.app.brain.MayraSkill
import ai.mayra.app.brain.MayraSkillRegistry
import ai.mayra.app.brain.SkillDescriptor
import ai.mayra.app.brain.SkillRequest
import ai.mayra.app.brain.SkillResult
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MayraAutonomyEngineTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        listOf("mayra_conversations", "mayra_goals").forEach {
            context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun conversationTracksPendingQuestionAndGoal() {
        val engine = ConversationStateEngine(context)
        var session = engine.create("Morning planning", 1L)
        session = engine.append(session.id, ConversationTurn(role = ConversationRole.USER, text = "Plan my day", timestamp = 2L))
        session = engine.append(
            session.id,
            ConversationTurn(
                role = ConversationRole.ASSISTANT,
                text = "Which task is most important?",
                timestamp = 3L,
                attributes = mapOf("expects_reply" to "true")
            )
        )
        assertEquals(ConversationState.WAITING_FOR_USER, session.state)
        assertNotNull(session.pendingQuestion)
        session = engine.attachGoal(session.id, "goal-1")
        assertEquals("goal-1", session.activeGoalId)
        session = engine.append(session.id, ConversationTurn(role = ConversationRole.USER, text = "Finish report", timestamp = 4L))
        assertEquals(ConversationState.ACTIVE, session.state)
        assertEquals(null, session.pendingQuestion)
    }

    @Test
    fun milestonesAdvanceAndCompleteGoal() {
        val engine = GoalEngine(context)
        val first = GoalMilestone(title = "Draft")
        val second = GoalMilestone(title = "Review")
        var goal = engine.create(MayraGoal(title = "Finish report", milestones = listOf(first, second)))
        assertEquals(GoalState.ACTIVE, goal.state)
        goal = engine.completeMilestone(goal.id, first.id, 10L)
        assertEquals(50, goal.progressPercent)
        goal = engine.completeMilestone(goal.id, second.id, 20L)
        assertEquals(GoalState.COMPLETED, goal.state)
        assertEquals(100, goal.progressPercent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun validatorRejectsWorkflowCycle() {
        val a = WorkflowNode(id = "a", order = 1, type = WorkflowNodeType.CHECKPOINT, title = "A", dependsOn = setOf("b"))
        val b = WorkflowNode(id = "b", order = 2, type = WorkflowNodeType.CHECKPOINT, title = "B", dependsOn = setOf("a"))
        WorkflowValidator().validate(MayraWorkflow(title = "cycle", nodes = listOf(a, b)))
    }

    @Test
    fun workflowExecutesDependenciesAndCompletes() = runBlocking {
        val registry = MayraSkillRegistry().apply { register(successSkill()) }
        val runtime = WorkflowRuntime(registry, ::contextSnapshot)
        val first = WorkflowNode(order = 1, type = WorkflowNodeType.ACTION, title = "First", intent = "test")
        val second = WorkflowNode(order = 2, type = WorkflowNodeType.CHECKPOINT, title = "Second", dependsOn = setOf(first.id))
        val submitted = runtime.submit(MayraWorkflow(title = "dependency", nodes = listOf(first, second)))
        val completed = runtime.drain(submitted.id)
        assertEquals(WorkflowState.COMPLETED, completed.state)
        assertTrue(completed.nodes.all { it.state == WorkflowNodeState.COMPLETED })
    }

    @Test
    fun sensitiveWorkflowNodeWaitsForConfirmation() = runBlocking {
        val registry = MayraSkillRegistry().apply { register(successSkill()) }
        val runtime = WorkflowRuntime(registry, ::contextSnapshot)
        val node = WorkflowNode(
            order = 1,
            type = WorkflowNodeType.ACTION,
            title = "Sensitive action",
            intent = "test",
            requiresConfirmation = true
        )
        val workflow = runtime.submit(MayraWorkflow(title = "confirmation", nodes = listOf(node)))
        val blocked = runtime.executeNext(workflow.id)
        assertTrue(blocked.waitingForConfirmation)
        assertEquals(WorkflowState.BLOCKED, blocked.workflow.state)
        assertTrue(runtime.confirmNode(workflow.id, node.id))
        val completed = runtime.executeNext(workflow.id)
        assertEquals(WorkflowState.COMPLETED, completed.workflow.state)
    }

    @Test
    fun boundedRetryStopsAfterBudget() = runBlocking {
        val registry = MayraSkillRegistry().apply {
            register(object : MayraSkill {
                override val descriptor = SkillDescriptor("fail", "Fail", supportedIntents = setOf("fail"))
                override fun confidence(request: SkillRequest) = 1.0
                override suspend fun execute(request: SkillRequest): SkillResult = SkillResult.Failure("offline", retryable = true)
            })
        }
        val runtime = WorkflowRuntime(registry, ::contextSnapshot)
        val node = WorkflowNode(
            order = 1,
            type = WorkflowNodeType.ACTION,
            title = "Retry",
            intent = "fail",
            failurePolicy = WorkflowFailurePolicy.RETRY,
            maxAttempts = 2
        )
        val workflow = runtime.submit(MayraWorkflow(title = "retry", nodes = listOf(node)))
        runtime.executeNext(workflow.id)
        val second = runtime.executeNext(workflow.id)
        assertEquals(WorkflowState.FAILED, second.workflow.state)
        assertEquals(2, second.workflow.nodes.single().attempt)
    }

    @Test
    fun decisionEngineProtectsRiskAndPermissions() {
        val engine = AutonomousDecisionEngine()
        val safe = engine.decide(DecisionInput(0.95, 0.1, 0.8, 0.9, true, true))
        assertEquals(AutonomyDecision.EXECUTE, safe.decision)

        val risky = engine.decide(DecisionInput(0.95, 0.9, 0.8, 0.9, true, true))
        assertEquals(AutonomyDecision.ASK, risky.decision)

        val permissionMissing = engine.decide(DecisionInput(0.9, 0.1, 0.5, 0.8, false, true))
        assertEquals(AutonomyDecision.ASK, permissionMissing.decision)

        val irrelevant = engine.decide(DecisionInput(0.1, 0.1, 0.1, 0.1, true, false))
        assertEquals(AutonomyDecision.IGNORE, irrelevant.decision)
    }

    @Test
    fun coordinatorLinksConversationGoalAndWorkflow() {
        val registry = MayraSkillRegistry().apply { register(successSkill()) }
        val coordinator = MayraAutonomyCoordinator(context, registry, ::contextSnapshot)
        val conversation = coordinator.startConversation("Project", "Help finish the report")
        val link = coordinator.createGoalWithWorkflow(
            title = "Finish report",
            description = "Complete and review",
            milestones = listOf("Draft", "Review"),
            workflowNodes = listOf(WorkflowNode(order = 1, type = WorkflowNodeType.ACTION, title = "Draft", intent = "test")),
            conversationId = conversation.id
        )
        assertEquals(link.goal.id, link.conversation?.activeGoalId)
        assertEquals(link.goal.id, link.workflow.goalId)
        assertFalse(coordinator.snapshot().goals.total == 0)
    }

    private fun successSkill(): MayraSkill = object : MayraSkill {
        override val descriptor = SkillDescriptor("test", "Test", supportedIntents = setOf("test"))
        override fun confidence(request: SkillRequest) = 1.0
        override suspend fun execute(request: SkillRequest): SkillResult = SkillResult.Success("done")
    }

    private fun contextSnapshot() = BrainContextSnapshot(10, 0, 0, true, 0)
}
