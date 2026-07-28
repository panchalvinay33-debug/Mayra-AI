package ai.mayra.app

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
import ai.mayra.app.core.ActionDispatcher
import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.LocalMayraAssistant
import ai.mayra.app.core.MayraAndroidRuntimeComposition
import ai.mayra.app.core.MayraAnswerProvider
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.document.DocumentInsightAwareMayraAssistant
import ai.mayra.app.platform.device.AndroidActionExecutor
import android.app.Application
import java.util.Calendar
import kotlinx.coroutines.runBlocking

class MayraApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val actionExecutor = AndroidActionExecutor(applicationContext)
        val localCommandEngine = LocalCommandEngine(
            actionDispatcher = ActionDispatcher(actionExecutor)
        )
        val localAssistant = LocalMayraAssistant(localCommandEngine)
        MayraRuntime.assistant = DocumentInsightAwareMayraAssistant(
            delegate = localAssistant,
            context = applicationContext
        )

        val typedRuntime = MayraAndroidRuntimeComposition(
            context = applicationContext,
            answerProvider = MayraAnswerProvider { message ->
                runBlocking { localCommandEngine.respond(message, emptyList()) }
            },
            enableSafeFilePickerAction = true
        )
        MayraRuntime.installTypedRuntime(typedRuntime)

        val eventBus = BrainEventBus()
        val skillRegistry = MayraSkillRegistry().apply {
            registerBuiltInDeviceSkills(actionExecutor)
        }
        val contextMemory = MayraContextMemory(applicationContext)
        val taskPlanner = MayraTaskPlanner()
        val planStore = MayraPlanStore(applicationContext)
        val pendingActions = PendingActionStore(applicationContext)
        val backgroundTasks = BackgroundTaskQueue(applicationContext)

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
        val brain = MayraBrainCoordinator(
            eventBus = eventBus,
            contextProvider = contextProvider
        )
        val planRuntime = MayraPlanRuntime(
            planner = taskPlanner,
            store = planStore,
            skills = skillRegistry,
            contextProvider = contextProvider
        )
        val orchestrator = MayraRuntimeOrchestrator(
            context = applicationContext,
            brain = brain,
            skills = skillRegistry,
            planner = taskPlanner,
            memory = contextMemory,
            pendingActions = pendingActions
        )

        MayraRuntime.install(
            brain = brain,
            skills = skillRegistry,
            memory = contextMemory,
            planner = taskPlanner,
            planStore = planStore,
            planRuntime = planRuntime,
            orchestrator = orchestrator
        )

        contextMemory.prune()
        planStore.prune()
        pendingActions.expireDue()
        pendingActions.prune()

        // Background scheduling is useful but non-critical. A provider/OEM/test-environment failure
        // must not prevent Mayra's local assistant and safety runtime from starting.
        runCatching { MayraBackgroundRuntime.initialize(applicationContext) }
        runCatching { MayraBriefingScheduler.sync(applicationContext) }
    }
}

/** Application-level service container shared by chat, background workers and future UI screens. */
object MayraRuntime {
    @Volatile
    var assistant: MayraAssistant = LocalMayraAssistant()

    lateinit var typedRuntime: MayraAndroidRuntimeComposition
        private set
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

    val installed: Boolean
        get() = ::orchestrator.isInitialized
    val typedRuntimeInstalled: Boolean
        get() = ::typedRuntime.isInitialized

    fun installTypedRuntime(runtime: MayraAndroidRuntimeComposition) {
        typedRuntime = runtime
    }

    fun install(
        brain: MayraBrainCoordinator,
        skills: MayraSkillRegistry,
        memory: MayraContextMemory,
        planner: MayraTaskPlanner,
        planStore: MayraPlanStore,
        planRuntime: MayraPlanRuntime,
        orchestrator: MayraRuntimeOrchestrator
    ) {
        this.brain = brain
        this.skills = skills
        this.memory = memory
        this.planner = planner
        this.planStore = planStore
        this.planRuntime = planRuntime
        this.orchestrator = orchestrator
    }
}
