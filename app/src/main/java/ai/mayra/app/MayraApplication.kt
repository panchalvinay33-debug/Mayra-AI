package ai.mayra.app

import ai.mayra.app.agent.AgentRisk
import ai.mayra.app.agent.AgentToolDescriptor
import ai.mayra.app.agent.MayraAgentPlanner
import ai.mayra.app.agent.MayraAgentRuntime
import ai.mayra.app.agent.MayraAgentToolRegistry
import ai.mayra.app.agent.UnavailableAgentTool
import ai.mayra.app.autonomy.MayraAutonomyCoordinator
import ai.mayra.app.background.BackgroundTaskQueue
import ai.mayra.app.background.MayraAmbientControlCenter
import ai.mayra.app.background.MayraBackgroundRuntime
import ai.mayra.app.background.MayraBriefingScheduler
import ai.mayra.app.background.PendingActionStore
import ai.mayra.app.brain.BrainContextSnapshot
import ai.mayra.app.brain.BrainEventBus
import ai.mayra.app.brain.MayraBrainCoordinator
import ai.mayra.app.brain.MayraContextMemory
import ai.mayra.app.brain.MayraPlanRuntime
import ai.mayra.app.brain.MayraPlanStore
import ai.mayra.app.brain.MayraRuntimeOrchestrator
import ai.mayra.app.brain.MayraSkillRegistry
import ai.mayra.app.brain.MayraTaskPlanner
import ai.mayra.app.brain.registerBuiltInDeviceSkills
import ai.mayra.app.context.MayraContextHolder
import ai.mayra.app.context.MayraContextRuntime
import ai.mayra.app.core.ActionDispatcher
import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.LocalMayraAssistant
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.device.MayraDeviceRuntime
import ai.mayra.app.device.androidDeviceRuntime
import ai.mayra.app.execution.AndroidExecutionCheckpointStore
import ai.mayra.app.execution.MayraExecutionControlPlane
import ai.mayra.app.execution.MayraExecutionCoordinator
import ai.mayra.app.knowledge.MayraKnowledgeStore
import ai.mayra.app.knowledge.MayraPersonalIntelligence
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.platform.device.AndroidActionExecutor
import ai.mayra.app.runtime.MayraRuntimeControlCenter
import ai.mayra.app.runtime.RuntimeAttentionNotifier
import ai.mayra.app.vision.MayraVisionCoordinator
import ai.mayra.app.vision.MayraVisionMemory
import ai.mayra.app.vision.MayraVisionRuntime
import ai.mayra.app.vision.UnavailableVisionProvider
import ai.mayra.app.voice.MayraVoiceCoordinator
import android.app.Application
import java.util.Calendar

class MayraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val actionExecutor = AndroidActionExecutor(applicationContext)
        MayraRuntime.assistant = LocalMayraAssistant(
            LocalCommandEngine(actionDispatcher = ActionDispatcher(actionExecutor))
        )

        val eventBus = BrainEventBus()
        val skillRegistry = MayraSkillRegistry().apply { registerBuiltInDeviceSkills(actionExecutor) }
        val contextMemory = MayraContextMemory(applicationContext)
        val taskPlanner = MayraTaskPlanner()
        val planStore = MayraPlanStore(applicationContext)
        val pendingActions = PendingActionStore(applicationContext)
        val backgroundTasks = BackgroundTaskQueue(applicationContext)
        val knowledgeStore = MayraKnowledgeStore(applicationContext)
        val personalMemory = MayraPersonalMemory(applicationContext)
        val personalIntelligence = MayraPersonalIntelligence(knowledgeStore, personalMemory)
        val voice = MayraVoiceCoordinator()
        val vision = MayraVisionCoordinator(
            runtime = MayraVisionRuntime(listOf(UnavailableVisionProvider())),
            memory = MayraVisionMemory()
        )
        val agentTools = MayraAgentToolRegistry(defaultAgentToolPlaceholders())
        val agentPlanner = MayraAgentPlanner(agentTools)
        val agentRuntime = MayraAgentRuntime(agentTools.snapshot())
        val contextRuntime = MayraContextRuntime()
        MayraContextHolder.runtime = contextRuntime
        val deviceRuntime = androidDeviceRuntime(applicationContext)
        val executionControlPlane = MayraExecutionControlPlane()
        val executionCoordinator = MayraExecutionCoordinator(
            controlPlane = executionControlPlane,
            agentRuntime = agentRuntime,
            deviceRuntime = deviceRuntime,
            checkpointStore = AndroidExecutionCheckpointStore(applicationContext)
        )

        val contextProvider = {
            val ambientHealth = MayraAmbientControlCenter.health(applicationContext)
            BrainContextSnapshot(
                hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                pendingActions = pendingActions.waiting().size,
                failedTasks = backgroundTasks.snapshot().count { it.state.name == "FAILED" },
                notificationAccessGranted = ambientHealth.notificationAccessGranted,
                recentCommandCount = contextMemory.topHabits("command", 20).sumOf { it.count },
                userAvailable = true
            )
        }
        val brain = MayraBrainCoordinator(eventBus, contextProvider)
        val planRuntime = MayraPlanRuntime(taskPlanner, planStore, skillRegistry, contextProvider)
        val orchestrator = MayraRuntimeOrchestrator(
            context = applicationContext,
            brain = brain,
            skills = skillRegistry,
            planner = taskPlanner,
            memory = contextMemory,
            pendingActions = pendingActions
        )
        val controlCenter = MayraRuntimeControlCenter(
            applicationContext, orchestrator, planRuntime, planStore, pendingActions
        )
        val autonomy = MayraAutonomyCoordinator(applicationContext, skillRegistry, contextProvider)

        MayraRuntime.install(
            brain = brain,
            skills = skillRegistry,
            memory = contextMemory,
            planner = taskPlanner,
            planStore = planStore,
            planRuntime = planRuntime,
            orchestrator = orchestrator,
            controlCenter = controlCenter,
            autonomy = autonomy,
            personalIntelligence = personalIntelligence,
            voice = voice,
            vision = vision,
            agentTools = agentTools,
            agentPlanner = agentPlanner,
            agentRuntime = agentRuntime,
            contextRuntime = contextRuntime,
            deviceRuntime = deviceRuntime,
            executionControlPlane = executionControlPlane,
            executionCoordinator = executionCoordinator
        )

        contextMemory.prune()
        planStore.prune()
        pendingActions.expireDue()
        pendingActions.prune()
        autonomy.maintenance()
        personalIntelligence.prune()
        deviceRuntime.capture(force = true)
        executionCoordinator.restore()
        MayraBackgroundRuntime.initialize(applicationContext)
        MayraBriefingScheduler.sync(applicationContext)
        RuntimeAttentionNotifier.scanAndNotify(applicationContext, controlCenter.snapshot())
    }

    private fun defaultAgentToolPlaceholders() = listOf(
        unavailableTool(
            id = "communication",
            operations = setOf("compose_message", "compose_whatsapp", "call"),
            risk = AgentRisk.HIGH,
            reason = "Communication agent adapter is not installed yet. Existing direct device actions remain available."
        ),
        unavailableTool(
            id = "personal",
            operations = setOf("create_reminder", "create_note"),
            reason = "Personal intelligence agent adapter is not installed yet."
        ),
        unavailableTool(
            id = "calendar",
            operations = setOf("create_event", "list_events"),
            reason = "Calendar agent provider is not installed yet."
        ),
        unavailableTool(
            id = "search",
            operations = setOf("weather", "unified_search"),
            requiresNetwork = true,
            reason = "Search and weather agent provider is not installed yet."
        ),
        unavailableTool(
            id = "vision",
            operations = setOf("analyze"),
            reason = "A real vision provider is not installed yet."
        )
    )

    private fun unavailableTool(
        id: String,
        operations: Set<String>,
        risk: AgentRisk = AgentRisk.LOW,
        requiresNetwork: Boolean = false,
        reason: String
    ) = UnavailableAgentTool(
        descriptor = AgentToolDescriptor(
            id = id,
            displayName = id.replaceFirstChar(Char::uppercase),
            operations = operations,
            risk = risk,
            requiresNetwork = requiresNetwork
        ),
        reason = reason
    )
}

/** Application-level service container shared by chat, background workers and future UI screens. */
object MayraRuntime {
    @Volatile var assistant: MayraAssistant = LocalMayraAssistant()

    lateinit var brain: MayraBrainCoordinator
        private set
    lateinit var skills: MayraSkillRegistry
        private set
    lateinit var memory: MayraContextMemory
        private set
    lateinit var planner: MayraTaskPlanner
        private set
    lateinit var planStore: MayraPlanStore
        private set
    lateinit var planRuntime: MayraPlanRuntime
        private set
    lateinit var orchestrator: MayraRuntimeOrchestrator
        private set
    lateinit var controlCenter: MayraRuntimeControlCenter
        private set
    lateinit var autonomy: MayraAutonomyCoordinator
        private set
    lateinit var personalIntelligence: MayraPersonalIntelligence
        private set
    lateinit var voice: MayraVoiceCoordinator
        private set
    lateinit var vision: MayraVisionCoordinator
        private set
    lateinit var agentTools: MayraAgentToolRegistry
        private set
    lateinit var agentPlanner: MayraAgentPlanner
        private set
    lateinit var agentRuntime: MayraAgentRuntime
        private set
    lateinit var contextRuntime: MayraContextRuntime
        private set
    lateinit var deviceRuntime: MayraDeviceRuntime
        private set
    lateinit var executionControlPlane: MayraExecutionControlPlane
        private set
    lateinit var executionCoordinator: MayraExecutionCoordinator
        private set

    val installed: Boolean
        get() = ::orchestrator.isInitialized && ::controlCenter.isInitialized &&
            ::autonomy.isInitialized && ::personalIntelligence.isInitialized &&
            ::voice.isInitialized && ::vision.isInitialized &&
            ::agentTools.isInitialized && ::agentPlanner.isInitialized &&
            ::agentRuntime.isInitialized && ::contextRuntime.isInitialized &&
            ::deviceRuntime.isInitialized && ::executionControlPlane.isInitialized &&
            ::executionCoordinator.isInitialized

    fun install(
        brain: MayraBrainCoordinator,
        skills: MayraSkillRegistry,
        memory: MayraContextMemory,
        planner: MayraTaskPlanner,
        planStore: MayraPlanStore,
        planRuntime: MayraPlanRuntime,
        orchestrator: MayraRuntimeOrchestrator,
        controlCenter: MayraRuntimeControlCenter,
        autonomy: MayraAutonomyCoordinator,
        personalIntelligence: MayraPersonalIntelligence,
        voice: MayraVoiceCoordinator,
        vision: MayraVisionCoordinator,
        agentTools: MayraAgentToolRegistry,
        agentPlanner: MayraAgentPlanner,
        agentRuntime: MayraAgentRuntime,
        contextRuntime: MayraContextRuntime,
        deviceRuntime: MayraDeviceRuntime,
        executionControlPlane: MayraExecutionControlPlane,
        executionCoordinator: MayraExecutionCoordinator
    ) {
        this.brain = brain
        this.skills = skills
        this.memory = memory
        this.planner = planner
        this.planStore = planStore
        this.planRuntime = planRuntime
        this.orchestrator = orchestrator
        this.controlCenter = controlCenter
        this.autonomy = autonomy
        this.personalIntelligence = personalIntelligence
        this.voice = voice
        this.vision = vision
        this.agentTools = agentTools
        this.agentPlanner = agentPlanner
        this.agentRuntime = agentRuntime
        this.contextRuntime = contextRuntime
        this.deviceRuntime = deviceRuntime
        this.executionControlPlane = executionControlPlane
        this.executionCoordinator = executionCoordinator
    }
}
