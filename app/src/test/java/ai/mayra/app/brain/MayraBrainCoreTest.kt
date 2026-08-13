package ai.mayra.app.brain

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MayraBrainCoreTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mayra_context_memory", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun sensitiveBrainEventRequiresConfirmation() {
        val bus = BrainEventBus()
        val coordinator = MayraBrainCoordinator(bus, contextProvider = {
            BrainContextSnapshot(10, 0, 0, true, 0)
        })

        val decision = coordinator.process(
            BrainEvent(
                type = BrainEventType.USER_COMMAND,
                source = "chat",
                payload = "Call Mummy",
                priority = BrainPriority.NORMAL
            )
        )

        assertTrue(decision.requiresConfirmation)
        assertEquals("pending_action", decision.route)
        assertEquals(1L, coordinator.diagnostics().processedEvents)
    }

    @Test
    fun eventBusSurvivesSubscriberFailure() {
        val bus = BrainEventBus()
        var delivered = false
        bus.subscribe { error("boom") }
        bus.subscribe { delivered = true }

        bus.publish(BrainEvent(type = BrainEventType.SYSTEM, source = "test", payload = "ping"))

        assertTrue(delivered)
        assertEquals(1L, bus.diagnostics().subscriberFailures)
    }

    @Test
    fun sensitiveSkillReturnsConfirmationBeforeExecution() = runBlocking {
        val registry = MayraSkillRegistry()
        registry.register(object : MayraSkill {
            override val descriptor = SkillDescriptor(
                id = "phone",
                displayName = "Phone Call",
                supportedIntents = setOf("call"),
                sensitive = true
            )

            override fun confidence(request: SkillRequest) = 1.0
            override suspend fun execute(request: SkillRequest): SkillResult = SkillResult.Success("called")
        })

        val request = SkillRequest(
            intent = "call",
            utterance = "call mummy",
            context = BrainContextSnapshot(9, 0, 0, true, 1)
        )

        assertTrue(registry.executeBest(request) is SkillResult.NeedsConfirmation)
        assertTrue(registry.executeBest(request.copy(confirmed = true)) is SkillResult.Success)
    }

    @Test
    fun plannerHonoursDependenciesAndCompletesPlan() {
        val planner = MayraTaskPlanner()
        val first = PlanStep(order = 1, intent = "call", description = "Call Mummy")
        val second = PlanStep(order = 2, intent = "message", description = "Message Rahul", dependsOn = setOf(first.id))
        var plan = planner.createPlan("Morning follow-up", "Call then message", listOf(first, second))

        assertEquals(listOf(first.id), planner.readySteps(plan).map { it.id })
        plan = planner.markRunning(plan, first.id)
        plan = planner.markCompleted(plan, first.id)
        assertEquals(listOf(second.id), planner.readySteps(plan).map { it.id })
        plan = planner.markRunning(plan, second.id)
        plan = planner.markCompleted(plan, second.id)

        assertEquals(PlanState.COMPLETED, plan.state)
        assertEquals(100, planner.progress(plan).percent)
    }

    @Test
    fun plannerRejectsDependencyCycle() {
        val planner = MayraTaskPlanner()
        val firstId = "a"
        val secondId = "b"
        val steps = listOf(
            PlanStep(id = firstId, order = 1, intent = "a", description = "A", dependsOn = setOf(secondId)),
            PlanStep(id = secondId, order = 2, intent = "b", description = "B", dependsOn = setOf(firstId))
        )

        try {
            planner.createPlan("cycle", "cycle", steps)
            fail("Expected cyclic plan to be rejected")
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun retryPolicyReturnsFailedStepToReadyState() {
        val planner = MayraTaskPlanner()
        val step = PlanStep(
            order = 1,
            intent = "network",
            description = "Fetch data",
            failurePolicy = FailurePolicy.RETRY,
            maxAttempts = 3
        )
        var plan = planner.createPlan("retry", "fetch", listOf(step))
        plan = planner.markRunning(plan, step.id)
        plan = planner.markFailed(plan, step.id, "offline")

        assertEquals(PlanStepState.READY, plan.steps.single().state)
        assertEquals(1, plan.steps.single().attempt)
    }

    @Test
    fun sensitiveMemoryRequiresExplicitOptIn() {
        val memory = MayraContextMemory(context)
        val record = MemoryRecord(
            kind = MemoryKind.CONTACT,
            key = "bank-pin",
            value = "secret",
            confidence = 1.0,
            sensitivity = MemorySensitivity.SENSITIVE,
            createdAt = 1L
        )

        try {
            memory.remember(record)
            fail("Expected sensitive memory to be blocked")
        } catch (_: SecurityException) {
            assertTrue(memory.snapshot().isEmpty())
        }
    }

    @Test
    fun repeatedHabitObservationsIncreaseCount() {
        val memory = MayraContextMemory(context)
        memory.observeHabit("app", "maps", 10L)
        val second = memory.observeHabit("app", "maps", 20L)

        assertEquals(2, second.count)
        assertEquals("maps", memory.topHabits("app").single().key)
        assertFalse(memory.topHabits("contact").isNotEmpty())
    }
}
