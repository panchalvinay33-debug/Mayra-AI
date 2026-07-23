package ai.mayra.app.brain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraRuntimeIntegrationTest {
    private lateinit var context: Context
    private lateinit var memory: MayraContextMemory
    private lateinit var registry: MayraSkillRegistry
    private lateinit var planner: MayraTaskPlanner
    private lateinit var brain: MayraBrainCoordinator
    private var clock = 1_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        listOf(
            "mayra_context_memory",
            "mayra_pending_actions",
            "mayra_trust_audit",
            "mayra_plans"
        ).forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        memory = MayraContextMemory(context)
        registry = MayraSkillRegistry()
        planner = MayraTaskPlanner()
        brain = MayraBrainCoordinator(
            eventBus = BrainEventBus(),
            contextProvider = { BrainContextSnapshot(10, 0, 0, true, 0) }
        )
    }

    @Test
    fun sensitiveCommandCreatesPersistentConfirmationAndSuppressesFastDuplicate() = runBlocking {
        val runtime = runtime()
        val request = RuntimeRequest(command = "Call Mummy", intent = "call")

        val first = runtime.handle(request)
        val second = runtime.handle(request)

        assertTrue(first.outcome is RuntimeOutcome.ConfirmationRequired)
        assertTrue(second.outcome is RuntimeOutcome.IgnoredDuplicate)
        assertEquals(1, context.getSharedPreferences("mayra_pending_actions", Context.MODE_PRIVATE)
            .getStringSet("actions", emptySet()).orEmpty().size)
        assertEquals(2L, runtime.diagnostics().processedRequests)
        assertEquals(1L, runtime.diagnostics().duplicateRequests)
    }

    @Test
    fun scheduledRequestBecomesOrderedPlanWithSensitiveGates() = runBlocking {
        val runtime = runtime()
        val receipt = runtime.handle(
            RuntimeRequest(
                command = "Morning follow up",
                intent = "plan",
                parameters = mapOf(
                    "scheduledFor" to "1000",
                    "steps" to "Call Mummy||Message Rahul||Remind me to buy milk"
                )
            )
        )

        val outcome = receipt.outcome as RuntimeOutcome.Planned
        assertEquals(3, outcome.plan.steps.size)
        assertTrue(outcome.plan.steps[0].requiresConfirmation)
        assertTrue(outcome.plan.steps[1].requiresConfirmation)
        assertEquals(setOf(outcome.plan.steps[0].id), outcome.plan.steps[1].dependsOn)
        assertEquals(setOf(outcome.plan.steps[1].id), outcome.plan.steps[2].dependsOn)
    }

    @Test
    fun planStoreRestoresPlanAndSkillRuntimeCompletesIt() = runBlocking {
        registry.register(successSkill("reminder"))
        val store = MayraPlanStore(context)
        val step = PlanStep(order = 1, intent = "reminder", description = "Buy milk")
        val submitted = planner.createPlan("Milk", "remind me", listOf(step))
        store.upsert(submitted)

        val restoredStore = MayraPlanStore(context)
        val restored = restoredStore.get(submitted.id)
        assertNotNull(restored)
        assertEquals("Buy milk", restored!!.steps.single().description)

        val runtime = MayraPlanRuntime(
            planner = planner,
            store = restoredStore,
            skills = registry,
            contextProvider = { BrainContextSnapshot(10, 0, 0, true, 0) }
        )
        val result = runtime.executeNext(submitted.id, now = 2_000L)

        assertEquals(PlanState.COMPLETED, result.plan.state)
        assertTrue(result.skillResult is SkillResult.Success)
        assertEquals(1, runtime.diagnostics().completedPlans)
    }

    @Test
    fun planRuntimeWaitsForConfirmationBeforeSensitiveStep() = runBlocking {
        registry.register(successSkill("call", sensitive = true))
        val store = MayraPlanStore(context)
        val step = PlanStep(
            order = 1,
            intent = "call",
            description = "Call Mummy",
            requiresConfirmation = true
        )
        val plan = planner.createPlan("Call", "call mummy", listOf(step))
        store.upsert(plan)
        val runtime = MayraPlanRuntime(
            planner = planner,
            store = store,
            skills = registry,
            contextProvider = { BrainContextSnapshot(10, 0, 0, true, 0) }
        )

        val blocked = runtime.executeNext(plan.id)
        assertTrue(blocked.waitingForConfirmation)
        assertEquals(null, blocked.executedStepId)

        assertTrue(runtime.confirmStep(plan.id, step.id))
        val completed = runtime.executeNext(plan.id)
        assertEquals(PlanState.COMPLETED, completed.plan.state)
    }

    @Test
    fun nonSensitiveSkillFlowsThroughBrainAndUpdatesDiagnostics() = runBlocking {
        registry.register(successSkill("help"))
        val runtime = runtime()
        val receipt = runtime.handle(RuntimeRequest("help me", "help"))

        assertTrue(receipt.outcome is RuntimeOutcome.Completed)
        assertEquals(1L, runtime.diagnostics().completedRequests)
        assertEquals(1L, runtime.diagnostics().brain.processedEvents)
        assertEquals(1, memory.topHabits("command").single().count)
    }

    private fun runtime(): MayraRuntimeOrchestrator = MayraRuntimeOrchestrator(
        context = context,
        brain = brain,
        skills = registry,
        planner = planner,
        memory = memory,
        now = { clock }
    )

    private fun successSkill(intent: String, sensitive: Boolean = false): MayraSkill = object : MayraSkill {
        override val descriptor = SkillDescriptor(
            id = "test-$intent",
            displayName = "Test $intent",
            supportedIntents = setOf(intent),
            sensitive = sensitive
        )

        override fun confidence(request: SkillRequest): Double = 1.0

        override suspend fun execute(request: SkillRequest): SkillResult = SkillResult.Success("done")
    }
}
